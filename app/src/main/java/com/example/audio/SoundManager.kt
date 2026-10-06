package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Built-in retro arcade sound synthesizer using Android AudioTrack.
 * Generates fun, crisp 8-bit sound effects without any external audio asset dependencies!
 */
class SoundManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var soundEnabled: Boolean = true
    private var hapticEnabled: Boolean = true

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }

    fun isSoundEnabled(): Boolean = soundEnabled

    fun setHapticEnabled(enabled: Boolean) {
        hapticEnabled = enabled
    }

    fun isHapticEnabled(): Boolean = hapticEnabled

    fun vibrate(durationMs: Long = 40) {
        if (!hapticEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    fun playClick() {
        vibrate(20)
        if (!soundEnabled) return
        scope.launch {
            playTone(pitchStart = 600f, pitchEnd = 900f, durationMs = 35)
        }
    }

    fun playCountdown(finalBeep: Boolean = false) {
        vibrate(if (finalBeep) 70 else 35)
        if (!soundEnabled) return
        scope.launch {
            if (finalBeep) {
                playTone(pitchStart = 880f, pitchEnd = 1100f, durationMs = 150)
            } else {
                playTone(pitchStart = 440f, pitchEnd = 440f, durationMs = 80)
            }
        }
    }

    fun playShoot() {
        vibrate(30)
        if (!soundEnabled) return
        scope.launch {
            playTone(pitchStart = 900f, pitchEnd = 300f, durationMs = 70)
        }
    }

    fun playBounce() {
        vibrate(20)
        if (!soundEnabled) return
        scope.launch {
            playTone(pitchStart = 350f, pitchEnd = 550f, durationMs = 45)
        }
    }

    fun playExplosion() {
        vibrate(80)
        if (!soundEnabled) return
        scope.launch {
            playNoise(durationMs = 180)
        }
    }

    fun playCoin() {
        vibrate(40)
        if (!soundEnabled) return
        scope.launch {
            playTone(pitchStart = 987f, pitchEnd = 987f, durationMs = 60)
            playTone(pitchStart = 1318f, pitchEnd = 1318f, durationMs = 120)
        }
    }

    fun playGoal() {
        vibrate(100)
        if (!soundEnabled) return
        scope.launch {
            playTone(pitchStart = 523f, pitchEnd = 659f, durationMs = 80)
            playTone(pitchStart = 659f, pitchEnd = 784f, durationMs = 80)
            playTone(pitchStart = 784f, pitchEnd = 1046f, durationMs = 200)
        }
    }

    fun playWinFanfare() {
        vibrate(120)
        if (!soundEnabled) return
        scope.launch {
            val notes = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f)
            for (note in notes) {
                playTone(note, note, 100)
            }
            playTone(1046.50f, 1318.51f, 300)
        }
    }

    private fun playTone(pitchStart: Float, pitchEnd: Float, durationMs: Int) {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)
        var currentFreq = pitchStart
        val freqStep = (pitchEnd - pitchStart) / numSamples.toFloat()
        var phase = 0.0

        for (i in 0 until numSamples) {
            currentFreq += freqStep
            phase += 2.0 * Math.PI * currentFreq / sampleRate
            val envelope = (1.0 - (i.toDouble() / numSamples)).toFloat()
            buffer[i] = (sin(phase) * 32767 * 0.45f * envelope).toInt().toShort()
        }

        playRawPcm(buffer, sampleRate)
    }

    private fun playNoise(durationMs: Int) {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val envelope = (1.0 - (i.toDouble() / numSamples)).toFloat()
            val sample = ((Math.random() * 2.0 - 1.0) * 32767 * 0.5f * envelope).toInt()
            buffer[i] = sample.toShort()
        }

        playRawPcm(buffer, sampleRate)
    }

    private fun playRawPcm(buffer: ShortArray, sampleRate: Int) {
        var track: AudioTrack? = null
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(buffer.size * 2, minBufferSize)

            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            Thread.sleep((buffer.size * 1000L / sampleRate) + 20)
        } catch (_: Exception) {
            // Ignore audio interruption on device backgrounding
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Exception) {}
        }
    }
}
