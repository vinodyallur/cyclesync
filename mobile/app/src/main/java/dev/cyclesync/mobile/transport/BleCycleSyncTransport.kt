package dev.cyclesync.mobile.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import dev.cyclesync.mobile.domain.ConnectionState
import dev.cyclesync.mobile.domain.ControlCommand
import dev.cyclesync.mobile.domain.DeviceAcknowledgement
import dev.cyclesync.mobile.domain.Telemetry
import dev.cyclesync.mobile.protocol.BleProfile
import dev.cyclesync.mobile.protocol.ProtocolCodec
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class BleCycleSyncTransport(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : CycleSyncTransport {
    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val adapter = bluetoothManager.adapter
    private val mutableConnectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    private val mutableTelemetry = MutableSharedFlow<Telemetry>(extraBufferCapacity = 8)
    private val mutableAcknowledgements = MutableSharedFlow<DeviceAcknowledgement>(extraBufferCapacity = 8)
    private var gatt: BluetoothGatt? = null
    private var commandCharacteristic: BluetoothGattCharacteristic? = null

    override val connectionState = mutableConnectionState.asStateFlow()
    override val telemetry = mutableTelemetry.asSharedFlow()
    override val acknowledgements = mutableAcknowledgements.asSharedFlow()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            adapter.bluetoothLeScanner?.stopScan(this)
            mutableConnectionState.value = ConnectionState.CONNECTING
            gatt = result.device.connectGatt(context, false, gattCallback)
        }

        override fun onScanFailed(errorCode: Int) {
            mutableConnectionState.value = ConnectionState.ERROR
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> gatt.discoverServices()
                BluetoothProfile.STATE_DISCONNECTED -> {
                    commandCharacteristic = null
                    mutableConnectionState.value = ConnectionState.DISCONNECTED
                }
                else -> Unit
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(UUID.fromString(BleProfile.SERVICE_UUID))
            val telemetryCharacteristic = service?.getCharacteristic(
                UUID.fromString(BleProfile.TELEMETRY_UUID),
            )
            val acknowledgementCharacteristic = service?.getCharacteristic(
                UUID.fromString(BleProfile.ACK_UUID),
            )
            commandCharacteristic = service?.getCharacteristic(UUID.fromString(BleProfile.COMMAND_UUID))

            if (service == null || telemetryCharacteristic == null || commandCharacteristic == null) {
                mutableConnectionState.value = ConnectionState.ERROR
                return
            }

            enableNotifications(gatt, telemetryCharacteristic)
            acknowledgementCharacteristic?.let { enableNotifications(gatt, it) }
            mutableConnectionState.value = ConnectionState.CONNECTED
        }

        @Deprecated("Used for compatibility below Android 13")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            handleCharacteristic(characteristic.uuid, characteristic.value ?: return)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            handleCharacteristic(characteristic.uuid, value)
        }
    }

    override suspend fun connect() {
        mutableConnectionState.value = ConnectionState.SCANNING
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(UUID.fromString(BleProfile.SERVICE_UUID)))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        adapter.bluetoothLeScanner?.startScan(listOf(filter), settings, scanCallback)
            ?: run { mutableConnectionState.value = ConnectionState.ERROR }
    }

    override suspend fun disconnect() {
        adapter.bluetoothLeScanner?.stopScan(scanCallback)
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        commandCharacteristic = null
        mutableConnectionState.value = ConnectionState.DISCONNECTED
    }

    override suspend fun send(command: ControlCommand, sessionId: String) {
        val characteristic = checkNotNull(commandCharacteristic) { "CycleSync command channel unavailable" }
        val value = ProtocolCodec.encodeCommand(
            command = command,
            deviceId = "cyclesync-devkit-01",
            sessionId = sessionId,
        ).encodeToByteArray()
        characteristic.value = value
        check(gatt?.writeCharacteristic(characteristic) == true) { "BLE command write failed" }
    }

    private fun enableNotifications(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
    ) {
        gatt.setCharacteristicNotification(characteristic, true)
        val cccd = characteristic.getDescriptor(
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),
        ) ?: return
        cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(cccd)
    }

    private fun handleCharacteristic(uuid: UUID, value: ByteArray) {
        val raw = value.decodeToString()
        scope.launch {
            runCatching {
                when (uuid.toString()) {
                    BleProfile.TELEMETRY_UUID -> mutableTelemetry.emit(ProtocolCodec.decodeTelemetry(raw))
                    BleProfile.ACK_UUID -> mutableAcknowledgements.emit(
                        ProtocolCodec.decodeAcknowledgement(raw),
                    )
                }
            }.onFailure {
                mutableConnectionState.value = ConnectionState.ERROR
            }
        }
    }
}
