package dev.cyclesync.mobile.domain

import kotlin.math.roundToInt

interface RecommendationEngine {
    fun recommend(input: SessionInput): Recommendation
}

/**
 * Deterministic baseline used to validate the complete workflow before a trained
 * on-device model is introduced behind the same interface.
 */
class BaselineRecommendationEngine : RecommendationEngine {
    override fun recommend(input: SessionInput): Recommendation {
        require(input.painScore in 0..10) { "painScore must be between 0 and 10" }
        require(input.comfortScore in 0..10) { "comfortScore must be between 0 and 10" }

        val previous = input.previousAcceptedLevel ?: 2
        val painAdjustment = when (input.painScore) {
            in 0..2 -> -1
            in 3..5 -> 0
            in 6..8 -> 1
            else -> 2
        }
        val comfortAdjustment = when (input.comfortScore) {
            in 0..2 -> -1
            in 8..10 -> 1
            else -> 0
        }
        val suggested = (previous + painAdjustment + comfortAdjustment).coerceIn(1, 6)
        val duration = when (input.painScore) {
            in 0..3 -> 300
            in 4..7 -> 600
            else -> 900
        }
        val sensorBonus = if (input.telemetry.emgRms != null) 0.05 else 0.0
        val confidence = (0.4 + input.telemetry.signalQuality * 0.5 + sensorBonus)
            .coerceIn(0.0, 0.95)
        val confidencePercent = (confidence * 100).roundToInt()

        return Recommendation(
            suggestedLevel = suggested,
            durationSeconds = duration,
            confidence = confidence,
            explanation = buildString {
                append("Pain ${input.painScore}/10 and comfort ${input.comfortScore}/10 ")
                append("were evaluated with ${confidencePercent}% input confidence. ")
                append("The suggestion stays within the prototype level 1-6 envelope.")
            },
        )
    }
}
