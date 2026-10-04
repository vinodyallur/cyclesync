package dev.cyclesync.mobile

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.cyclesync.mobile.databinding.ActivityMainBinding
import dev.cyclesync.mobile.domain.ConnectionState
import dev.cyclesync.mobile.domain.SafetyDecision
import dev.cyclesync.mobile.ui.DashboardState
import dev.cyclesync.mobile.ui.DashboardViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.painSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) viewModel.setPainScore(value.toInt())
        }
        binding.comfortSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) viewModel.setComfortScore(value.toInt())
        }
        binding.connectButton.setOnClickListener { viewModel.connect() }
        binding.recommendButton.setOnClickListener { viewModel.generateRecommendation() }
        binding.applyButton.setOnClickListener { viewModel.applyRecommendation() }
        binding.stopButton.setOnClickListener { viewModel.emergencyStop() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: DashboardState) = with(binding) {
        painValue.text = getString(R.string.score_value, state.painScore)
        comfortValue.text = getString(R.string.score_value, state.comfortScore)
        statusText.text = state.message
        connectionValue.text = state.connectionState.name.lowercase().replaceFirstChar(Char::uppercase)
        connectButton.isEnabled = state.connectionState != ConnectionState.CONNECTED

        val telemetry = state.telemetry
        signalQualityValue.text = telemetry?.let {
            getString(R.string.percent_value, (it.signalQuality * 100).toInt())
        } ?: getString(R.string.not_available)
        heartRateValue.text = telemetry?.heartRateBpm?.let {
            getString(R.string.heart_rate_value, it.toInt())
        } ?: getString(R.string.not_available)
        spo2Value.text = telemetry?.spo2Percent?.let {
            getString(R.string.spo2_value, it.toInt())
        } ?: getString(R.string.not_available)
        emgValue.text = telemetry?.emgRms?.let {
            getString(R.string.emg_value, it)
        } ?: getString(R.string.not_available)

        val recommendation = state.recommendation
        recommendationCard.isVisible = recommendation != null
        recommendation?.let {
            recommendedLevelValue.text = getString(R.string.level_value, it.suggestedLevel)
            durationValue.text = getString(R.string.duration_value, it.durationSeconds / 60)
            confidenceValue.text = getString(R.string.percent_value, (it.confidence * 100).toInt())
            explanationValue.text = it.explanation
        }

        val allowed = state.safetyDecision is SafetyDecision.Allowed
        applyButton.isEnabled = allowed
        safetyValue.text = when (val decision = state.safetyDecision) {
            is SafetyDecision.Allowed -> getString(R.string.safety_passed, decision.checks.size)
            is SafetyDecision.Blocked -> decision.reasons.joinToString("\n")
            null -> getString(R.string.safety_waiting)
        }
        deviceStateValue.text = state.deviceState.name.lowercase().replaceFirstChar(Char::uppercase)
    }
}
