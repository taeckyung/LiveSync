package com.terry00123.livesync

import kotlin.math.pow
import kotlin.math.sqrt

object CrossCorrelation {

    fun crossCorrelate(source: ShortArray, target: ShortArray): Int {
        val n = nearestPowerOf2(source.size + target.size - 1)

        val sourceReal= FFT.shortToDouble(source.copyOf(n))
        val sourceImag = DoubleArray(n) {0.0}

        val targetReal = FFT.shortToDouble(target.copyOf(n))
        val targetImag = DoubleArray(n) {0.0}

        val fft = FFT(n)

        fft.fft(sourceReal, sourceImag)
        fft.fft(targetReal, targetImag)

        // Conjugate
        for (i in sourceImag.indices)
            sourceImag[i] = -sourceImag[i]

        val timeProductReal = DoubleArray(n)
        val timeProductImag = DoubleArray(n)

        for (i in 0 until n) {
            timeProductReal[i] = sourceReal[i] * targetReal[i] - sourceImag[i] * targetImag[i]
            timeProductImag[i] = sourceReal[i] * targetImag[i] + sourceImag[i] * targetReal[i]
        }

        fft.ifft(timeProductReal, timeProductImag)

        var idx = argMax(timeProductReal, timeProductImag)

        if (idx > n - source.size)
            idx -= n

        return idx
    }

    private fun argMax(re: DoubleArray, im: DoubleArray): Int {

        val arr = DoubleArray(re.size)

        for (i in re.indices) {
            arr[i] = sqrt(re[i].pow(2.0) + im[i].pow(2.0))
        }

        val arrWithIndex = arr.withIndex().sortedWith(compareByDescending{it.value})

        return arrWithIndex[0].index
    }
}