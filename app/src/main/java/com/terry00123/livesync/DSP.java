package com.terry00123.livesync;

import java.util.Arrays;

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
public class DSP {
    /**
     * Find the time difference of two signal.
     * Added by Taeckyung LEE.
     */
    public static int findDelay(short[] a, short[] b, int interval) {
        short[] y = xcorr(a, b, interval);
        int i_max = y.length;
        int index = 0;
        short value = (short) -Short.MIN_VALUE;
        for (int i = 0 ; i < i_max ; ++i) {
            if (y[i] > value) {
                index = i;
                value = y[i];
            }
        }
        return (index - (y.length / 2)) * interval;
    }

    /**
     * Computes the cross correlation between sequences a and b.
     */
    public static short[] xcorr(short[] a, short[] b, int interval) {
        int len = a.length;
        if (b.length > a.length)
            len = b.length;

        len -= 1;
        len -= len % interval;

        return xcorr(a, b, len, interval);
    }

    /**
     * Computes the auto correlation of a.
     */
    public static short[] xcorr(short[] a, int interval) {
        return xcorr(a, a, interval);
    }

    /**
     * Computes the cross correlation between sequences a and b.
     * maxlag is the maximum lag to
     */
    public static short[] xcorr(short[] a, short[] b, int maxlag, int interval) {
        short[] y = new short[2 * (maxlag/interval) + 1];
        Arrays.fill(y, (short) 0);

        for (int lag = b.length - 1, idx = (maxlag - b.length + 1) / interval;
             lag > -a.length; lag-=interval, ++idx) {
            if (idx < 0)
                continue;

            if (idx >= y.length)
                break;

            // where do the two signals overlap?
            int start = 0;
            // we can't start past the left end of b
            if (lag < 0) {
                start = -lag;
            }

            int end = a.length - 1;
            // we can't go past the right end of b
            if (end > b.length - lag - 1) {
                end = b.length - lag - 1;
            }

            for (int n = start; n <= end; n++) {
                y[idx] += a[n] * b[lag + n];
            }
        }

        return y;
    }
}