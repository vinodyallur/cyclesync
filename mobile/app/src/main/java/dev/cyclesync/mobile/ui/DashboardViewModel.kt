package dev.cyclesync.mobile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.cyclesync.mobile.BuildConfig
import dev.cyclesync.mobile.domain.BaselineRecommendationEngine
import dev.cyclesync.mobile.domain.ConnectionState
import dev.cyclesync.mobile.domain.ControlCommand
import dev.cyclesync.mobile.domain.DeviceAcknowledgement
import dev.cyclesync.mobile.domain.DeviceState
import dev.cyclesync.mobile.domain.Recommendation
import dev.cyclesync.mobile.domain.SafetyDecision
import dev.cyclesync.mobile.domain.SafetyPolicy
import dev.cyclesync.mobile.domain.SessionInput
import dev.cyclesync.mobile.domain.Telemetry
import dev.cyclesync.mobile.transport.BleCycleSyncTransport
import dev.cyclesync.mobile.transport.CycleSyncTransport
import dev.cyclesync.mobile.transport.SimulatedTransport
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardState(
    val painScore: Int = 7,
    val comfortScore: Int = 5,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val telemetry: Telemetry? = null,
    val recommendation: Recommendation? = null,
    val safetyDecision: SafetyDecision? = null,
    val deviceState: DeviceState = DeviceState.IDLE,
    val message: String = "Connect to begin a local session",
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = BaselineRecommendationEngine()
    private val safetyPolicy = SafetyPolicy()
    private val transport: CycleSyncTransport = if (BuildConfig.USE_SIMULATED_TRANSPORT) {
        SimulatedTransport(viewModelScope)
    } else {
        BleCycleSyncTransport(application, viewModelScope)
    }
    private val mutableState = MutableStateFlow(DashboardState())
    private val sessionId = "session-${UUID.randomUUID().toString().take(12)}"
    private var sequence = 1L
    private var previousAcceptedLevel: Int? = null

    val state: StateFlow<DashboardState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            transport.connectionState.collect { connectionState ->
                mutableState.update {
                    it.copy(
                        connectionState = connectionState,
                        message = when (connectionState) {
                            ConnectionState.CONNECTED -> "Local device connected"
                            ConnectionState.SCANNING -> "Scanning for CycleSync"
                            ConnectionState.CONNECTING -> "Connecting"
                            ConnectionState.ERROR -> "Connection error; use simulation fallback"
                            ConnectionState.DISCONNECTED -> "Disconnected"
                        },
                    )
                }
            }
        }
        viewModelScope.launch {
            transport.telemetry.collect { telemetry ->
                mutableState.update { it.copy(telemetry = telemetry) }
            }
        }
        viewModelScope.launch {
            transport.acknowledgements.collect(::handleAcknowledgement)
        }
    }

    fun setPainScore(value: Int) {
        mutableState.update { it.copy(painScore = value, recommendation = null, safetyDecision = null) }
    }

    fun setComfortScore(value: Int) {
        mutableState.update { it.copy(comfortScore = value, recommendation = null, safetyDecision = null) }
    }

    fun connect() = viewModelScope.launch {
        runCatching { transport.connect() }
            .onFailure { error -> mutableState.update { it.copy(message = error.message ?: "Connection failed") } }
    }

    fun generateRecommendation() {
        val current = mutableState.value
        val telemetry = current.telemetry ?: run {
            mutableState.update { it.copy(message = "Waiting for fresh sensor telemetry") }
            return
        }
        val input = SessionInput(
            painScore = current.painScore,
            comfortScore = current.comfortScore,
            telemetry = telemetry,
            previousAcceptedLevel = previousAcceptedLevel,
            sessionActive = current.connectionState == ConnectionState.CONNECTED,
        )
        val recommendation = engine.recommend(input)
        val decision = safetyPolicy.evaluate(input, recommendation)
        mutableState.update {
            it.copy(
                recommendation = recommendation,
                safetyDecision = decision,
                message = when (decision) {
                    is SafetyDecision.Allowed -> "Recommendation ready for your review"
                    is SafetyDecision.Blocked -> decision.reasons.joinToString(" · ")
                },
            )
        }
    }

    fun applyRecommendation() = viewModelScope.launch {
        val decision = mutableState.value.safetyDecision as? SafetyDecision.Allowed ?: return@launch
        val command = ControlCommand(
            action = ControlCommand.Action.APPLY_SETTING,
            settingLevel = decision.recommendation.suggestedLevel,
            durationSeconds = decision.recommendation.durationSeconds,
            userConfirmed = true,
            sequence = sequence++,
        )
        mutableState.update { it.copy(message = "Sending confirmed setting") }
        runCatching { transport.send(command, sessionId) }
            .onFailure { error -> mutableState.update { it.copy(message = error.message ?: "Command failed") } }
    }

    fun emergencyStop() = viewModelScope.launch {
        val command = ControlCommand(
            action = ControlCommand.Action.STOP,
            reason = "User pressed emergency stop",
            sequence = sequence++,
        )
        runCatching { transport.send(command, sessionId) }
            .onFailure { error -> mutableState.update { it.copy(message = error.message ?: "Stop failed") } }
    }

    private fun handleAcknowledgement(acknowledgement: DeviceAcknowledgement) {
        if (acknowledgement.accepted && acknowledgement.state == DeviceState.ACTIVE) {
            previousAcceptedLevel = mutableState.value.recommendation?.suggestedLevel
        }
        mutableState.update {
            it.copy(
                deviceState = acknowledgement.state,
                message = if (acknowledgement.accepted) {
                    "Device ${acknowledgement.state.name.lowercase()} · command acknowledged"
                } else {
                    acknowledgement.reason ?: "Device rejected the command"
                },
            )
        }
    }
}
