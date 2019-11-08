package com.terry00123.livesync

import android.util.Log

import java.util.Arrays

/*
 *  Copyright 2006 Columbia University.
 *
 *  This file is part of MEAPsoft.
 *
 *  MEAPsoft is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 as
 *  published by the Free Software Foundation.
 *
 *  MEAPsoft is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MEAPsoft; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA
 *
 *  See the file "COPYING" for the text of the license.
 */
/*
 * https://www.ee.columbia.edu/~ronw/code/dev/MEAPsoft/src/com/meapsoft/DSP.java
 */
object DSP {
    /**
     * Find the time difference of two signal.
     * Added by Taeckyung LEE.
     */
    fun findDelay(a: ShortArray, b: ShortArray, interval: Int): Int {
        val corr = xcorr(a, b, interval)
        var index = 0
        var value = java.lang.Double.MIN_VALUE

        for (i in corr.indices) {
            if (corr[i] > value) {
                index = i
                value = corr[i]
            }
        }

        Log.i("myTag", "Index " + (index - corr.size / 2) + " Value " + value)
        return (index - corr.size / 2) * interval
    }

    /**
     * Computes the cross correlation between sequences a and b.
     */
    fun xcorr(a: ShortArray, b: ShortArray, interval: Int): DoubleArray {
        var len = a.size
        if (b.size > a.size)
            len = b.size

        len -= 1
        len -= len % interval

        return xcorr(a, b, len, interval)
    }

    /**
     * Computes the cross correlation between sequences a and b.
     */
    fun xcorr(a: ShortArray, b: ShortArray, maxlag: Int, interval: Int): DoubleArray {
        val y = DoubleArray(2 * (maxlag / interval) + 1)
        Arrays.fill(y, 0.0)

        var lagMax = b.size - 1
        lagMax -= lagMax % interval
        var lagMin = -a.size + 1
        lagMin += lagMin % interval

        var lag = lagMax
        var idx = (maxlag - b.size + 1) / interval
        while (lag >= lagMin) {
            if (idx < 0) {
                lag -= interval
                ++idx
                continue
            }

            if (idx >= y.size)
                break

            // where do the two signals overlap?
            var start = 0
            // we can't start past the left end of b
            if (lag < 0) {
                start = -lag
            }

            var end = a.size - 1
            // we can't go past the right end of b
            if (end > b.size - lag - 1) {
                end = b.size - lag - 1
            }

            for (n in start..end) {
                y[idx] += (a[n] * b[lag + n]).toDouble()
            }
            lag -= interval
            ++idx
        }

        return y
    }
}