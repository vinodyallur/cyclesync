package dev.cyclesync.mobile.transport

import dev.cyclesync.mobile.domain.ConnectionState
import dev.cyclesync.mobile.domain.ControlCommand
import dev.cyclesync.mobile.domain.DeviceAcknowledgement
import dev.cyclesync.mobile.domain.Telemetry
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface CycleSyncTransport {
    val connectionState: StateFlow<ConnectionState>
    val telemetry: SharedFlow<Telemetry>
    val acknowledgements: SharedFlow<DeviceAcknowledgement>

    suspend fun connect()
    suspend fun disconnect()
    suspend fun send(command: ControlCommand, sessionId: String)
}
