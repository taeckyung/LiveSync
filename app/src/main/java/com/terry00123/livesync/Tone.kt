package com.terry00123.livesync

import kotlin.math.sin

object Tone {

    fun generateFreq(freq: Int, sampleRate: Int, bufferSize: Int): ShortArray {
        return generateFreqBody(freq, 1.0, sampleRate, bufferSize)
    }

    fun generateFreq(freq: Int, amplitude: Double, sampleRate: Int, bufferSize: Int): ShortArray {
        return generateFreqBody(freq, amplitude.coerceIn(0.0, 1.0), sampleRate, bufferSize)
    }

    private fun generateFreqBody(freq: Int, amplitude: Double, sampleRate: Int, bufferSize: Int) : ShortArray {
        val generatedSnd = ShortArray(bufferSize)

        for (i in 0 until bufferSize) {
            val tmp = sin(2.0 * Math.PI * i.toDouble() / (sampleRate / freq).toDouble())
            generatedSnd[i] = (tmp * amplitude * java.lang.Short.MAX_VALUE).toShort()
        }

        return generatedSnd
    }
}