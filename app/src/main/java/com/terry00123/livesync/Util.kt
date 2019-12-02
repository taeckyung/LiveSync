package com.terry00123.livesync

import kotlin.math.abs

fun nearestPowerOf2(n: Int): Int {
    var a = 1
    while (a <= n) {
        a = a shl 1
    }
    return a
}

fun IntArray.median() : Int {
    val sorted = this.copyOf()
    sorted.sort()
    return if (this.size % 2 == 0) {
        (sorted[this.size/2 - 1] + sorted[this.size/2]) / 2
    }
    else {
        sorted[this.size/2]
    }
}

fun IntArray.valueOfMinDistance() : Int {
    val dist = IntArray(this.size)
    for (i in this.indices) {
        for (j in i until this.size) {
            val result = abs(this[i] - this[j])
            dist[i] += result
            dist[j] += result
        }
    }
    val sortedDist = dist.withIndex().sortedWith(compareBy { it.value })
    return this[sortedDist[0].index]
}