package com.terry00123.livesync

import android.media.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val originalWav = resources.openRawResource(R.raw.mono_original)
        val originalArray = Wave.wavToShortArray(originalWav)

        val delayedWav = resources.openRawResource(R.raw.mono_20ms)
        val delayedArray =  Wave.wavToShortArray(delayedWav)

        if (originalArray == null || delayedArray == null) {
            Log.e("myTag", "Converted array is null!")
            finishAffinity()
        }

        Wave.playShortArray(delayedArray)

        val d = DSP.findDelay(originalArray, delayedArray, 2205)
        Log.i("myTag", d.toString())

    }
}
