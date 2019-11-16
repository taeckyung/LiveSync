package com.terry00123.livesync

import android.util.Log
import kotlinx.coroutines.*


class FindLatency(private val recorder: Recorder, private val speaker: Speaker) {
    private val freq = 11000
    private val maxLoop = 6
    private val maxLatencyInBuffers = 50
    private val threshold = 1.0

    fun getLatency() : Long? {
        return runBlocking {

            val tone = Tone.generateFreq(freq, speaker.sampleRate, speaker.bufferSize)
            val latencyArray = LongArray(maxLoop)

            var index = 0

            repeat(maxLoop) {
                Thread.sleep(500)

                val startTime = System.currentTimeMillis()

                val speakerWait = async {speaker.play(tone, 0, false)}

                val recorderWait = async {getLatencyRecorderBody()}

                speakerWait.await()
                val endTime = recorderWait.await()

                if (endTime != null) {
                    latencyArray[index] = endTime - startTime
                    Log.i("LiveSync_FindLatency", "$it-th loop latency ${latencyArray[index]}")
                    index += 1
                }
            }

            if (index != 0) {
                val finalArray = latencyArray.sliceArray(0 until index)
                finalArray.sort()
                if (index % 2 == 0) {
                    ((finalArray[index/2 -1] + finalArray[index/2])/2)
                } else {
                    finalArray[index/2]
                }
            }
            else {
                null
            }
        }
    }

    private fun getLatencyRecorderBody() : Long? {
        val bufferSize = recorder.bufferSize
        val index = (freq / (recorder.sampleRate.toDouble() / recorder.bufferSize)).toInt()

        val r = recorder.getRecordedAudioWithTime(bufferSize * maxLatencyInBuffers)
        val array = r.first
        val time = r.second

        for (i in 0 until maxLatencyInBuffers) {
            val abs = FFT(bufferSize)
                .getFreqSpectrumFromShort(array.sliceArray(bufferSize*i until bufferSize*(i+1)))

            val freqComponent = abs.slice(index-2..index+2).sum()

            if (freqComponent > threshold) {
                return time[i]
            }
        }
        return null
    }

}