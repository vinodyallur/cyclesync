package dev.cyclesync.mobile.protocol

import dev.cyclesync.mobile.domain.ControlCommand
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolCodecTest {
    @Test
    fun confirmedApplyCommandMatchesSharedEnvelope() {
        val raw = ProtocolCodec.encodeCommand(
            command = ControlCommand(
                action = ControlCommand.Action.APPLY_SETTING,
                settingLevel = 3,
                durationSeconds = 600,
                userConfirmed = true,
                sequence = 42,
            ),
            deviceId = "cyclesync-devkit-01",
            sessionId = "demo-session-001",
            timestampMs = 1234,
        )
        val envelope = JSONObject(raw)

        assertEquals(1, envelope.getInt("protocolVersion"))
        assertEquals("control_command", envelope.getString("kind"))
        assertTrue(envelope.getJSONObject("payload").getBoolean("userConfirmed"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun applyCommandWithoutConfirmationIsRejected() {
        ProtocolCodec.encodeCommand(
            command = ControlCommand(
                action = ControlCommand.Action.APPLY_SETTING,
                settingLevel = 3,
                durationSeconds = 600,
                userConfirmed = false,
                sequence = 42,
            ),
            deviceId = "cyclesync-devkit-01",
            sessionId = "demo-session-001",
        )
    }
}