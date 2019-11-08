package com.terry00123.livesync

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val originalWav = resources.openRawResource(R.raw.mono_recording_0ms)
        val originalArray = Wave.wavToShortArray(originalWav)?.sliceArray(441000 .. 882000)

        val delayedWav = resources.openRawResource(R.raw.mono_recording_1s)
        val delayedArray =  Wave.wavToShortArray(delayedWav)?.sliceArray(441000 .. 882000)


        if (originalArray == null || delayedArray == null) {
            Log.e("myTag", "Converted array is null!")
            finishAffinity()
        }
        else {
            val time = measureTimeMillis {
                val d = CrossCorrelation.crossCorrelate(originalArray, delayedArray)
                val timeInterval = d.toDouble() / 44100.0
                Log.i("myTag", timeInterval.toString())
            } / 1000.0
            Log.i("myTag", time.toString())

            /*
            val d2 = DSP.findDelay(originalArray, delayedArray, 441)
            val timeInterval2 = d2.toDouble() / 44100.0
            Log.i("myTag", timeInterval2.toString())
             */
        }

    }
}
