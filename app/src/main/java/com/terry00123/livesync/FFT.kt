package com.example.cs442_hw2

/*
 *  Copyright 2006-2007 Columbia University.
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
class FFT(internal var n: Int) {
    internal var m: Int = 0

    // Lookup tables.  Only need to recompute when size of FFT changes.
    internal var cos: DoubleArray
    internal var sin: DoubleArray

    var window: DoubleArray = doubleArrayOf()
        internal set

    init {
        this.m = (Math.log(n.toDouble()) / Math.log(2.0)).toInt()

        // Make sure n is a power of 2
        if (n != 1 shl m)
            throw RuntimeException("FFT length must be power of 2")

        // precompute tables
        cos = DoubleArray(n / 2)
        sin = DoubleArray(n / 2)

        for (i in 0 until n / 2) {
            cos[i] = Math.cos(-2.0 * Math.PI * i.toDouble() / n)
            sin[i] = Math.sin(-2.0 * Math.PI * i.toDouble() / n)
        }

        makeWindow()
    }

    protected fun makeWindow() {
        // Make a blackman window:
        window = DoubleArray(n)
        for (i in window.indices)
            window[i] =
                0.42 - 0.5 * Math.cos(2.0 * Math.PI * i.toDouble() / (n - 1)) + 0.08 * Math.cos(4.0 * Math.PI * i.toDouble() / (n - 1))
    }


    /***************************************************************
     * fft.c
     * Douglas L. Jones
     * University of Illinois at Urbana-Champaign
     * January 19, 1992
     * http://cnx.rice.edu/content/m12016/latest/
     *
     * fft: in-place radix-2 DIT DFT of a complex input
     *
     * input:
     * n: length of FFT: must be a power of two
     * m: n = 2**m
     * input/output
     * x: double array of length n with real part of data
     * y: double array of length n with imag part of data
     *
     * Permission to copy and use this program is granted
     * as long as this header is included.
     */
    fun fft(x: DoubleArray, y: DoubleArray) {
        var i: Int
        var j: Int
        var k: Int
        var n1: Int
        var n2: Int
        var a: Int
        var c: Double
        var s: Double
        val e: Double
        var t1: Double
        var t2: Double


        // Bit-reverse
        j = 0
        n2 = n / 2
        i = 1
        while (i < n - 1) {
            n1 = n2
            while (j >= n1) {
                j = j - n1
                n1 = n1 / 2
            }
            j = j + n1

            if (i < j) {
                t1 = x[i]
                x[i] = x[j]
                x[j] = t1
                t1 = y[i]
                y[i] = y[j]
                y[j] = t1
            }
            i++
        }

        // FFT
        n1 = 0
        n2 = 1

        i = 0
        while (i < m) {
            n1 = n2
            n2 = n2 + n2
            a = 0

            j = 0
            while (j < n1) {
                c = cos[a]
                s = sin[a]
                a += 1 shl m - i - 1

                k = j
                while (k < n) {
                    t1 = c * x[k + n1] - s * y[k + n1]
                    t2 = s * x[k + n1] + c * y[k + n1]
                    x[k + n1] = x[k] - t1
                    y[k + n1] = y[k] - t2
                    x[k] = x[k] + t1
                    y[k] = y[k] + t2
                    k = k + n2
                }
                j++
            }
            i++
        }
    }


    //added by Taesik Gong
    fun getAbs(re: DoubleArray): DoubleArray {
        val im = DoubleArray(this.n)
        this.fft(re, im)
        val abs = DoubleArray(this.n / 2 + 1)
        for (i in 0 until this.n / 2 + 1) {
            abs[i] = Math.sqrt(Math.pow(re[i], 2.0) + Math.pow(im[i], 2.0))
        }
        return abs
    }

    //*** use this for CS442 HW3
    fun getFreqSpectrumFromShort(input: ShortArray): DoubleArray {
        val re = shortToDouble(input)
        return getAbs(re)
    }

    companion object {

        fun shortToDouble(input: ShortArray): DoubleArray {
            val output = DoubleArray(input.size)
            for (i in input.indices)
                output[i] = input[i].toDouble() / java.lang.Short.MAX_VALUE
            return output
        }


        // Test the FFT to make sure it's working
        @JvmStatic
        fun main(args: Array<String>) {
            val N = 8

            val fft = FFT(N)

            val window = fft.window
            val re = DoubleArray(N)
            val im = DoubleArray(N)

            // Impulse
            re[0] = 1.0
            im[0] = 0.0
            for (i in 1 until N) {
                im[i] = 0.0
                re[i] = im[i]
            }
            beforeAfter(fft, re, im)

            // Nyquist
            for (i in 0 until N) {
                re[i] = Math.pow(-1.0, i.toDouble())
                im[i] = 0.0
            }
            beforeAfter(fft, re, im)

            // Single sin
            for (i in 0 until N) {
                re[i] = Math.cos(2.0 * Math.PI * i.toDouble() / N)
                im[i] = 0.0
            }
            beforeAfter(fft, re, im)

            // Ramp
            for (i in 0 until N) {
                re[i] = i.toDouble()
                im[i] = 0.0
            }
            beforeAfter(fft, re, im)

            var time = System.currentTimeMillis()
            val iter = 30000.0
            var i = 0
            while (i < iter) {
                fft.fft(re, im)
                i++
            }
            time = System.currentTimeMillis() - time
            println("Averaged " + time / iter + "ms per iteration")
        }

        protected fun beforeAfter(fft: FFT, re: DoubleArray, im: DoubleArray) {
            println("Before: ")
            printReIm(re, im)
            fft.fft(re, im)
            println("After: ")
            printReIm(re, im)
        }

        protected fun printReIm(re: DoubleArray, im: DoubleArray) {
            print("Re: [")
            for (i in re.indices)
                print(((re[i] * 1000).toInt() / 1000.0).toString() + " ")

            print("]\nIm: [")
            for (i in im.indices)
                print(((im[i] * 1000).toInt() / 1000.0).toString() + " ")

            println("]")
        }
    }
}