package com.terry00123.livesync

import android.util.Log
import kotlin.math.pow
import kotlin.math.sqrt

object CrossCorrelation {

    private fun nearestPowerOf2(n: Int): Int {
        var a = 1
        while (a <= n) {
            a = a shl 1
        }
        return a
    }

    fun crossCorrelate(source: ShortArray, target: ShortArray): Int {
        val n = nearestPowerOf2(source.size + target.size - 1)

        val sourceReal= FFT.shortToDouble(source.copyOf(n))
        val sourceImag = DoubleArray(n)

        val targetReal = FFT.shortToDouble(target.copyOf(n))
        val targetImag = DoubleArray(n)

        val fft = FFT(n)

        fft.fft(sourceReal, sourceImag)
        fft.fft(targetReal, targetImag)

        // Conjugate
        for (i in sourceImag.indices)
            sourceImag[i] = -sourceImag[i]

        val timeProductReal = DoubleArray(n)
        val timeProductImag = DoubleArray(n)

        for (i in 1 until n) {
            timeProductReal[i] = sourceReal[i] * targetReal[i] - sourceImag[i] * targetImag[i]
            timeProductImag[i] = sourceReal[i] * targetImag[i] + sourceImag[i] * targetReal[i]
        }

        fft.ifft(timeProductReal, timeProductImag)

        val sortedList = argMax(timeProductReal, timeProductImag)

        //Log.i("myTag", sortedList.slice(0..4).toString())

        var idx = sortedList[0].index

        if (idx > n - source.size)
            idx -= n

        Log.i("myTag", "max arg: $idx, max val: ${sortedList[0].value}")

        return idx
    }

    private fun argMax(re: DoubleArray, im: DoubleArray): List<IndexedValue<Double>> {

        val arr = DoubleArray(re.size)

        for (i in re.indices) {
            arr[i] = sqrt(re[i].pow(2.0) + im[i].pow(2.0))
        }

        val arrWithIndex = arr.withIndex().sortedWith(compareByDescending{it.value})

        return arrWithIndex.toList()
    }
}