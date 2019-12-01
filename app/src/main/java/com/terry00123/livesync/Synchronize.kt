package com.terry00123.livesync

import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

fun getTDoA(recorder: Recorder, speaker: Speaker, sampleRate: Int, audio: ShortArray, syncDuration: Double) : Int {
    var ret = 0
    runBlocking {
        val contentWait = async {
            Log.i("LiveSync_Synchronize", "contentWait: ${System.currentTimeMillis()}")
            speaker.getOffset()
        }
        val recordWait = async {
            Log.i("LiveSync_Synchronize", "recordWait: ${System.currentTimeMillis()}")
            recorder.getRecordedAudio((syncDuration * sampleRate).toInt())
        }

        val recordedArray = recordWait.await()

        val offset = contentWait.await()
        val contentArray = audio.sliceArray(offset until (offset + syncDuration*sampleRate).toInt())

/*
        var startTime = System.currentTimeMillis()

        Log.i("LiveSync_MainActivity", "Recorded: ${recordedArray.size}, Content: ${contentArray.size}")

        val tempTimeInterval = IntArray(3)
        val stride = ((syncDuration - syncPartial) / 2.0)
        for (i in 0 .. 2) {
            val start = stride*i*sampleRate
            var end =  (stride*i + syncPartial)*sampleRate
            if (end >= recordedArray.size) {
                end = recordedArray.size.toDouble()
            }
            tempTimeInterval[i] = compareInterval(contentArray,
                recordedArray.sliceArray(start.toInt() until end.toInt())) + (stride*i*1000).toInt()
        }
        Log.i("LiveSync_MainActivity", "timeInterval: ${tempTimeInterval.contentToString()}")

        timeInterval = tempTimeInterval.valueOfMinDistance()

        var elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
        Log.i("LiveSync_MainActivity", "synchronize: $elapsedTime seconds")
*/

        ret = compareInterval(contentArray, recordedArray, sampleRate)
    }
    return ret
}

private fun compareInterval(originalArray: ShortArray?, recordedArray: ShortArray?, sampleRate: Int) : Int {
    return if (originalArray == null || recordedArray == null) {
        Log.e("LiveSync_Synchronize", "Converted array is null!")
        0
    } else {
        val startTime = System.currentTimeMillis()
        val d = CrossCorrelation.crossCorrelate(originalArray, recordedArray)
        val timeInterval = (d * 1000.0 / sampleRate).toInt()
        val elapsedTime = System.currentTimeMillis() - startTime
        Log.i("LiveSync_Synchronize", "timeInterval: $timeInterval | elapsedTime: $elapsedTime")
        timeInterval
    }
}
