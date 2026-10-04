package dev.cyclesync.mobile.protocol

import dev.cyclesync.mobile.domain.ControlCommand
import dev.cyclesync.mobile.domain.DeviceAcknowledgement
import dev.cyclesync.mobile.domain.DeviceState
import dev.cyclesync.mobile.domain.Telemetry
import org.json.JSONObject

object BleProfile {
    const val SERVICE_UUID = "7d6b0001-62f4-4c89-9ad5-3a5b39b1f401"
    const val TELEMETRY_UUID = "7d6b0002-62f4-4c89-9ad5-3a5b39b1f401"
    const val COMMAND_UUID = "7d6b0003-62f4-4c89-9ad5-3a5b39b1f401"
    const val ACK_UUID = "7d6b0004-62f4-4c89-9ad5-3a5b39b1f401"
}

object ProtocolCodec {
    const val PROTOCOL_VERSION = 1

    fun decodeTelemetry(raw: String): Telemetry {
        val envelope = JSONObject(raw)
        require(envelope.getInt("protocolVersion") == PROTOCOL_VERSION)
        require(envelope.getString("kind") == "telemetry")
        val payload = envelope.getJSONObject("payload")
        return Telemetry(
            signalQuality = payload.getDouble("signalQuality"),
            emgRms = payload.optNullableDouble("emgRms"),
            heartRateBpm = payload.optNullableDouble("heartRateBpm"),
            spo2Percent = payload.optNullableDouble("spo2Percent"),
            batteryPercent = payload.optInt("batteryPercent", 0),
            sequence = envelope.getLong("sequence"),
            capturedAtMs = envelope.getLong("timestampMs"),
        )
    }

    fun encodeCommand(
        command: ControlCommand,
        deviceId: String,
        sessionId: String,
        timestampMs: Long = System.currentTimeMillis(),
    ): String {
        val payload = JSONObject()
        when (command.action) {
            ControlCommand.Action.APPLY_SETTING -> {
                require(command.userConfirmed) { "Apply commands require user confirmation" }
                payload.put("action", "apply_setting")
                payload.put("settingLevel", requireNotNull(command.settingLevel))
                payload.put("durationSeconds", requireNotNull(command.durationSeconds))
                payload.put("userConfirmed", true)
            }
            ControlCommand.Action.STOP -> {
                payload.put("action", "stop")
                payload.put("reason", command.reason ?: "User requested stop")
            }
        }

        return JSONObject()
            .put("protocolVersion", PROTOCOL_VERSION)
            .put("kind", "control_command")
            .put("deviceId", deviceId)
            .put("sessionId", sessionId)
            .put("sequence", command.sequence)
            .put("timestampMs", timestampMs)
            .put("payload", payload)
            .toString()
    }

    fun decodeAcknowledgement(raw: String): DeviceAcknowledgement {
        val envelope = JSONObject(raw)
        require(envelope.getInt("protocolVersion") == PROTOCOL_VERSION)
        require(envelope.getString("kind") == "ack")
        val payload = envelope.getJSONObject("payload")
        return DeviceAcknowledgement(
            acknowledgedSequence = payload.getLong("acknowledgedSequence"),
            accepted = payload.getBoolean("accepted"),
            state = DeviceState.valueOf(payload.getString("state").uppercase()),
            reason = payload.optString("reason").ifBlank { null },
        )
    }

    private fun JSONObject.optNullableDouble(name: String): Double? =
        if (has(name) && !isNull(name)) getDouble(name) else null
}
