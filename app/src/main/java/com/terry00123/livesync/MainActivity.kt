package com.terry00123.livesync

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity() {
    private val MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1
    private lateinit var recorder : Recorder
    private lateinit var speaker : Speaker
    private lateinit var videoView : VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                MY_PERMISSIONS_REQUEST_RECORD_AUDIO)
        }
        else {
            lateInit()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_RECORD_AUDIO -> {
                if ((grantResults.isNotEmpty() && grantResults[0]
                            == PackageManager.PERMISSION_GRANTED)) {
                    lateInit()
                } else {
                    finish()
                }
            }
        }
    }

    private fun lateInit() {
        recorder = Recorder()
        speaker = Speaker()
/*
        videoView = findViewById(R.id.videoView)
        val controller = MediaController(this)
        videoView.setMediaController(controller)
        videoView.requestFocus()
        val path = Environment.getDataDirectory().toString() + "/video.mp4"
        videoView.setVideoPath(path)
*/

        //synchronize()
        val latency = FindLatency(recorder, speaker).getLatency()
        Log.i("myTag", "Latency found as $latency")
    }

    override fun onDestroy() {
        recorder.release()
        speaker.release()
        super.onDestroy()
    }

    private fun synchronize() {
        runBlocking {
            coroutineScope {
                val time = System.currentTimeMillis()

                val recordedArray : ShortArray?
                var contentArray : ShortArray?

                val recordWait = async {
                    recorder.getRecordedAudio(441000)
                }
                val contentWait = async {
                    Wave.wavToShortArray(
                        resources.openRawResource(R.raw.mono_recording_0ms)
                    )
                }

                recordedArray = recordWait.await()
                contentArray = contentWait.await()?.sliceArray(441000..882000)

                Log.i("myTag", recordedArray?.size?.toString() ?: "not recorded")

                val elapsedTime = (System.currentTimeMillis() - time) / 1000.0
                Log.i("myTag", "synchronize: $elapsedTime seconds")

                compareInterval(contentArray, recordedArray)
            }
        }
    }

    private fun compareInterval(originalArray: ShortArray?, recordedArray: ShortArray?) {
        if (originalArray == null || recordedArray == null) {
            Log.e("myTag", "Converted array is null!")
        }
        else {
            val time = measureTimeMillis {
                val d = CrossCorrelation.crossCorrelate(originalArray, recordedArray)
                val timeInterval = d.toDouble() / 44100.0
                Log.i("myTag", timeInterval.toString())
                runOnUiThread {
                    textTDoA.text = timeInterval.toString()
                }
            } / 1000.0
            Log.i("myTag", "compareInterval: $time seconds")
        }
    }
}
