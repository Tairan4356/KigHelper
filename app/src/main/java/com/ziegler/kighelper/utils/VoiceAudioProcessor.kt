package com.ziegler.kighelper.utils

import com.ziegler.kighelper.data.VoiceProfile
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.roundToInt

object VoiceAudioProcessor {
    fun process(samples: FloatArray, profile: VoiceProfile): FloatArray {
        if (samples.isEmpty()) return samples

        val pitchShift = profile.pitch.coerceIn(0.9f, 1.1f)
        val pitched = if (abs(pitchShift - 1f) > 0.015f) {
            resampleForPitch(samples, pitchShift)
        } else {
            samples.copyOf()
        }

        val warmed = applyWarmth(pitched, profile.warmth.coerceIn(0f, 1f))
        val gain = (0.98f + (profile.expressiveness - 0.5f) * 0.12f).coerceIn(0.92f, 1.06f)
        return applyGainAndSoftLimit(warmed, gain)
    }

    fun writeWav(file: File, samples: FloatArray, sampleRate: Int) {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { output ->
            val dataSize = samples.size * BYTES_PER_SAMPLE
            output.writeAscii("RIFF")
            output.writeIntLe(36 + dataSize)
            output.writeAscii("WAVE")
            output.writeAscii("fmt ")
            output.writeIntLe(16)
            output.writeShortLe(1)
            output.writeShortLe(1)
            output.writeIntLe(sampleRate)
            output.writeIntLe(sampleRate * BYTES_PER_SAMPLE)
            output.writeShortLe(BYTES_PER_SAMPLE)
            output.writeShortLe(16)
            output.writeAscii("data")
            output.writeIntLe(dataSize)

            samples.forEach { sample ->
                val pcm = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).roundToInt()
                output.writeShortLe(pcm)
            }
        }
    }

    private fun resampleForPitch(samples: FloatArray, pitchShift: Float): FloatArray {
        val outputSize = (samples.size / pitchShift).roundToInt().coerceAtLeast(1)
        val output = FloatArray(outputSize)
        val step = samples.size.toFloat() / outputSize

        for (index in output.indices) {
            val sourcePosition = index * step
            val left = sourcePosition.toInt().coerceIn(0, samples.lastIndex)
            val right = (left + 1).coerceAtMost(samples.lastIndex)
            val fraction = sourcePosition - left
            output[index] = samples[left] * (1f - fraction) + samples[right] * fraction
        }

        return output
    }

    private fun applyWarmth(samples: FloatArray, warmth: Float): FloatArray {
        val amount = ((warmth - 0.5f) * 2f).coerceIn(-1f, 1f)
        if (abs(amount) < 0.04f) return samples

        val output = FloatArray(samples.size)
        var low = 0f
        val alpha = 0.08f
        for (index in samples.indices) {
            val sample = samples[index]
            low += alpha * (sample - low)
            val high = sample - low
            output[index] = if (amount > 0f) {
                sample * (1f - amount * 0.28f) + low * (amount * 0.28f)
            } else {
                sample + high * (-amount * 0.18f)
            }
        }
        return output
    }

    private fun applyGainAndSoftLimit(samples: FloatArray, gain: Float): FloatArray {
        return FloatArray(samples.size) { index ->
            val amplified = samples[index] * gain
            amplified / (1f + abs(amplified) * 0.08f)
        }
    }

    private fun FileOutputStream.writeAscii(value: String) {
        write(value.toByteArray(Charsets.US_ASCII))
    }

    private fun FileOutputStream.writeIntLe(value: Int) {
        write(value and 0xff)
        write(value shr 8 and 0xff)
        write(value shr 16 and 0xff)
        write(value shr 24 and 0xff)
    }

    private fun FileOutputStream.writeShortLe(value: Int) {
        write(value and 0xff)
        write(value shr 8 and 0xff)
    }

    private const val BYTES_PER_SAMPLE = 2
}
