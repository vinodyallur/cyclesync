package dev.cyclesync.mobile.domain

data class Telemetry(
    val signalQuality: Double,
    val emgRms: Double?,
    val heartRateBpm: Double?,
    val spo2Percent: Double?,
    val batteryPercent: Int,
    val sequence: Long,
    val capturedAtMs: Long,
)

data class SessionInput(
    val painScore: Int,
    val comfortScore: Int,
    val telemetry: Telemetry,
    val previousAcceptedLevel: Int?,
    val sessionActive: Boolean,
)

data class Recommendation(
    val suggestedLevel: Int,
    val durationSeconds: Int,
    val confidence: Double,
    val explanation: String,
)

sealed interface SafetyDecision {
    data class Allowed(
        val recommendation: Recommendation,
        val checks: List<String>,
    ) : SafetyDecision

    data class Blocked(val reasons: List<String>) : SafetyDecision
}

data class ControlCommand(
    val action: Action,
    val settingLevel: Int? = null,
    val durationSeconds: Int? = null,
    val reason: String? = null,
    val userConfirmed: Boolean = false,
    val sequence: Long,
) {
    enum class Action { APPLY_SETTING, STOP }
}

data class DeviceAcknowledgement(
    val acknowledgedSequence: Long,
    val accepted: Boolean,
    val state: DeviceState,
    val reason: String?,
)

enum class DeviceState {
    IDLE,
    ACTIVE,
    STOPPED,
    REJECTED,
}

enum class ConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    ERROR,
}
