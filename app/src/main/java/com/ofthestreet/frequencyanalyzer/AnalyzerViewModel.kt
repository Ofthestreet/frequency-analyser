package com.ofthestreet.frequencyanalyzer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ofthestreet.frequencyanalyzer.analysis.AnalysisParams
import com.ofthestreet.frequencyanalyzer.analysis.AudioCapture
import com.ofthestreet.frequencyanalyzer.analysis.AudioCaptureException
import com.ofthestreet.frequencyanalyzer.analysis.CaptureFormat
import com.ofthestreet.frequencyanalyzer.analysis.FrameAnalyzer
import com.ofthestreet.frequencyanalyzer.music.NoteNaming
import com.ofthestreet.frequencyanalyzer.settings.AnalyzerSettings
import com.ofthestreet.frequencyanalyzer.settings.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ln

class AnalyzerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    val settings: StateFlow<AnalyzerSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AnalyzerSettings(),
    )

    private val _state = MutableStateFlow(AnalyzerUiState())
    val state: StateFlow<AnalyzerUiState> = _state.asStateFlow()

    private var captureJob: Job? = null
    private var smoothedHz = 0f

    fun startListening() {
        if (captureJob?.isActive == true) return
        _state.value = _state.value.copy(listening = true, error = null)
        captureJob = viewModelScope.launch { capture() }
    }

    fun stopListening() {
        captureJob?.cancel()
        captureJob = null
        smoothedHz = 0f
        _state.value = _state.value.copy(listening = false, frozen = false)
    }

    fun toggleFreeze() {
        _state.value = _state.value.copy(frozen = !_state.value.frozen)
    }

    fun updateSettings(transform: (AnalyzerSettings) -> AnalyzerSettings) {
        viewModelScope.launch { repository.save(transform(settings.value)) }
    }

    private suspend fun capture() {
        var analyzer: FrameAnalyzer? = null
        var history = FloatArray(0)

        try {
            AudioCapture().frames().collect { frame ->
                val current = settings.value
                val engine = analyzer ?: FrameAnalyzer(frame.sampleRate).also { analyzer = it }

                val updatesPerSecond = frame.sampleRate.toFloat() / CaptureFormat.HOP_SIZE
                val wanted = (current.historySeconds * updatesPerSecond).toInt().coerceAtLeast(1)
                if (history.size != wanted) history = FloatArray(wanted)

                val result = engine.analyze(
                    frame.samples,
                    AnalysisParams(
                        algorithm = current.algorithm,
                        minHz = current.range.minHz,
                        maxHz = current.range.maxHz,
                        noiseThresholdDb = current.noiseThresholdDb,
                    ),
                )

                if (_state.value.frozen) return@collect

                smoothedHz = smooth(smoothedHz, result.frequencyHz)
                history.copyInto(history, 0, 1, history.size)
                history[history.size - 1] = smoothedHz

                _state.value = _state.value.copy(
                    listening = true,
                    frequencyHz = smoothedHz,
                    clarity = result.clarity,
                    levelDb = result.levelDb,
                    note = NoteNaming.fromFrequency(smoothedHz.toDouble(), current.referenceA4.toDouble()),
                    spectrum = result.spectrum.copyOf(),
                    history = history.copyOf(),
                    sampleRate = frame.sampleRate,
                    error = null,
                )
            }
        } catch (failure: AudioCaptureException) {
            _state.value = _state.value.copy(listening = false, error = failure.message)
        }
    }

    /**
     * Lissage exponentiel léger : il enlève le tremblement de l'affichage sans ajouter de retard
     * perceptible. Un saut de plus d'un demi-ton n'est pas lissé — c'est une note différente, pas
     * du bruit de mesure.
     */
    private fun smooth(previous: Float, measured: Float): Float {
        if (measured <= 0f) return 0f
        if (previous <= 0f) return measured
        val semitones = abs(12.0 * ln(measured / previous.toDouble()) / ln(2.0))
        if (semitones > 1.0) return measured
        return previous + SMOOTHING * (measured - previous)
    }

    override fun onCleared() {
        super.onCleared()
        captureJob?.cancel()
    }

    private companion object {
        const val SMOOTHING = 0.4f
    }
}
