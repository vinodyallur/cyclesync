package dev.cyclesync.mobile.domain

class SafetyPolicy(
    private val minimumSignalQuality: Double = 0.65,
    private val maximumTelemetryAgeMs: Long = 6_000,
    private val maximumSettingLevel: Int = 6,
    private val maximumDurationSeconds: Int = 900,
    private val minimumConfidence: Double = 0.55,
) {
    fun evaluate(
        input: SessionInput,
        recommendation: Recommendation,
        nowMs: Long = System.currentTimeMillis(),
    ): SafetyDecision {
        val reasons = buildList {
            if (!input.sessionActive) add("No active session")
            if (nowMs - input.telemetry.capturedAtMs > maximumTelemetryAgeMs) {
                add("Telemetry is stale")
            }
            if (input.telemetry.signalQuality < minimumSignalQuality) {
                add("Signal quality is below the safe recommendation threshold")
            }
            if (recommendation.suggestedLevel !in 1..maximumSettingLevel) {
                add("Suggested level exceeds the prototype envelope")
            }
            if (recommendation.durationSeconds !in 30..maximumDurationSeconds) {
                add("Suggested duration exceeds the prototype envelope")
            }
            if (recommendation.confidence < minimumConfidence) {
                add("Recommendation confidence is too low")
            }
        }

        if (reasons.isNotEmpty()) return SafetyDecision.Blocked(reasons)

        return SafetyDecision.Allowed(
            recommendation = recommendation,
            checks = listOf(
                "active_session",
                "telemetry_fresh",
                "signal_quality_ok",
                "within_setting_limit",
                "within_duration_limit",
                "confidence_ok",
            ),
        )
    }
}
