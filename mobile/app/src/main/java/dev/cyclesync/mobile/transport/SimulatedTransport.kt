package dev.cyclesync.mobile.transport

import dev.cyclesync.mobile.domain.ConnectionState
import dev.cyclesync.mobile.domain.ControlCommand
import dev.cyclesync.mobile.domain.DeviceAcknowledgement
import dev.cyclesync.mobile.domain.DeviceState
import dev.cyclesync.mobile.domain.Telemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

class SimulatedTransport(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : CycleSyncTransport {
    private val mutableConnectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    private val mutableTelemetry = MutableSharedFlow<Telemetry>(replay = 1)
    private val mutableAcknowledgements = MutableSharedFlow<DeviceAcknowledgement>()
    private var telemetryJob: Job? = null
    private var sequence = 0L

    override val connectionState = mutableConnectionState.asStateFlow()
    override val telemetry = mutableTelemetry.asSharedFlow()
    override val acknowledgements = mutableAcknowledgements.asSharedFlow()

    override suspend fun connect() {
        mutableConnectionState.value = ConnectionState.CONNECTING
        delay(350)
        mutableConnectionState.value = ConnectionState.CONNECTED
        telemetryJob?.cancel()
        telemetryJob = scope.launch {
            while (isActive) {
                sequence += 1
                val phase = sequence / 4.0
                mutableTelemetry.emit(
                    Telemetry(
                        signalQuality = 0.88 + sin(phase) * 0.04,
                        emgRms = 0.16 + sin(phase) * 0.025,
                        heartRateBpm = 78.0 + sin(phase) * 3.0,
                        spo2Percent = 98.0,
                        batteryPercent = (92 - sequence / 120).coerceAtLeast(20).toInt(),
                        sequence = sequence,
                        capturedAtMs = System.currentTimeMillis(),
                    ),
                )
                delay(1_000)
            }
        }
    }

    override suspend fun disconnect() {
        telemetryJob?.cancel()
        mutableConnectionState.value = ConnectionState.DISCONNECTED
    }

    override suspend fun send(command: ControlCommand, sessionId: String) {
        check(mutableConnectionState.value == ConnectionState.CONNECTED)
        delay(180)
        val state = when (command.action) {
            ControlCommand.Action.APPLY_SETTING -> DeviceState.ACTIVE
            ControlCommand.Action.STOP -> DeviceState.STOPPED
        }
        mutableAcknowledgements.emit(
            DeviceAcknowledgement(
                acknowledgedSequence = command.sequence,
                accepted = true,
                state = state,
                reason = if (state == DeviceState.STOPPED) command.reason else null,
            ),
        )
    }
}
