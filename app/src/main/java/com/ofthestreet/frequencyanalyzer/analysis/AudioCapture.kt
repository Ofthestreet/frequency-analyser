package com.ofthestreet.frequencyanalyzer.analysis

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

/** Une fenêtre d'analyse, glissante : [samples] contient les [CaptureFormat.FRAME_SIZE] derniers échantillons. */
class AudioFrame(val samples: FloatArray, val sampleRate: Int)

/** Erreur de capture remontée à l'UI plutôt que de planter l'app. */
class AudioCaptureException(message: String) : Exception(message)

/**
 * Capture micro.
 *
 * La fenêtre d'analyse fait [CaptureFormat.FRAME_SIZE] échantillons mais n'avance que de [CaptureFormat.HOP_SIZE] à chaque
 * trame : l'affichage se rafraîchit deux fois plus vite qu'avec des blocs disjoints, sans coûter
 * plus cher en résolution fréquentielle.
 */
class AudioCapture {

    companion object {
        /** Par ordre de préférence : le moins de traitement appliqué par le téléphone, le mieux. */
        private val SOURCES = intArrayOf(
            MediaRecorder.AudioSource.UNPROCESSED,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC,
        )

        private val SAMPLE_RATES = intArrayOf(48_000, 44_100)
    }

    @SuppressLint("MissingPermission")
    fun frames(): Flow<AudioFrame> = flow {
        val recorder = open() ?: throw AudioCaptureException(
            "Impossible d'ouvrir le micro : il est peut-être utilisé par une autre application.",
        )
        val record = recorder.record
        try {
            record.startRecording()
            if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                throw AudioCaptureException("Le micro n'a pas démarré.")
            }

            val window = FloatArray(CaptureFormat.FRAME_SIZE)
            val chunk = ShortArray(CaptureFormat.HOP_SIZE)
            while (currentCoroutineContext().isActive) {
                var filled = 0
                while (filled < CaptureFormat.HOP_SIZE) {
                    val read = record.read(chunk, filled, CaptureFormat.HOP_SIZE - filled)
                    if (read <= 0) throw AudioCaptureException("Lecture micro interrompue (code $read).")
                    filled += read
                }

                window.copyInto(window, 0, CaptureFormat.HOP_SIZE, CaptureFormat.FRAME_SIZE)
                val offset = CaptureFormat.FRAME_SIZE - CaptureFormat.HOP_SIZE
                for (i in 0 until CaptureFormat.HOP_SIZE) {
                    window[offset + i] = chunk[i] / 32_768f
                }
                emit(AudioFrame(window.copyOf(), recorder.sampleRate))
            }
        } finally {
            runCatching { record.stop() }
            record.release()
        }
    }.flowOn(Dispatchers.Default)

    private class OpenRecorder(val record: AudioRecord, val sampleRate: Int)

    @SuppressLint("MissingPermission")
    private fun open(): OpenRecorder? {
        for (source in SOURCES) {
            for (sampleRate in SAMPLE_RATES) {
                val minBuffer = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                )
                if (minBuffer <= 0) continue

                val bufferSize = maxOf(minBuffer, CaptureFormat.HOP_SIZE * 2 * 4)
                val record = runCatching {
                    AudioRecord(
                        source,
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                    )
                }.getOrNull() ?: continue

                if (record.state == AudioRecord.STATE_INITIALIZED) {
                    return OpenRecorder(record, sampleRate)
                }
                record.release()
            }
        }
        return null
    }
}
