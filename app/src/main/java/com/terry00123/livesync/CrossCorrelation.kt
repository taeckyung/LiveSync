package com.terry00123.livesync

import android.util.Log

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
        val sourceComplex = arrayOfNulls<Complex>(n)
        val targetComplex = arrayOfNulls<Complex>(n)

        for (i in source.indices) {
            sourceComplex[i] = Complex(source[i].toDouble(), 0.0)
        }
        for (i in source.size until n) {
            sourceComplex[i] = Complex(0.0, 0.0)
        }
        for (i in target.indices) {
            targetComplex[i] = Complex(target[i].toDouble(), 0.0)
        }
        for (i in target.size until n) {
            targetComplex[i] = Complex(0.0, 0.0)
        }

        val fftS = FFT.fft(sourceComplex)
        val fftT = FFT.fft(targetComplex)

        for (i in fftS.indices) {
            fftS[i] = fftS[i].conjugate()
        }

        val timeProduct = arrayOfNulls<Complex>(fftS.size)
        for (i in fftS.indices) {
            timeProduct[i] = fftS[i].times(fftT[i])
        }

        val y = FFT.ifft(timeProduct)

        val sortedList = argMax(y)

        //Log.i("myTag", sortedList.slice(0..4).toString())

        var idx = sortedList[0].index

        if (idx > n - source.size)
            idx -= n

        Log.i("myTag", "max arg: $idx, max val: ${sortedList[0].value}")

        return idx
    }

    private fun argMax(a: Array<Complex>): List<IndexedValue<Double>> {

        val arr = DoubleArray(a.size)

        for (i in a.indices) {
            arr[i] = a[i].abs()
        }

        val arrWithIndex = arr.withIndex().sortedWith(compareByDescending{it.value})

        return arrWithIndex.toList()
    }
}