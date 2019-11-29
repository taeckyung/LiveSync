package com.terry00123.livesync

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private val MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1

    private val sampleRate = 44100
    private val audioInChannel = AudioFormat.CHANNEL_IN_MONO
    private val audioOutChannel = AudioFormat.CHANNEL_OUT_MONO
    private val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSizeInBytes = 1024
    
    private val syncDuration = sampleRate * 5
    
    private lateinit var recorder : Recorder
    private lateinit var speaker : Speaker
    private lateinit var videoView : VideoView
    private lateinit var audio : ShortArray

    private var audioLatency = 0
    private var timeInterval = 0
    private var propagationDelay = 0L

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

        val recordedArray = Wave.wavToShortArray(resources.openRawResource(R.raw.mono_recording_0ms))?.sliceArray(441000 .. 882000)
        val contentArray = Wave.wavToShortArray(resources.openRawResource(R.raw.mono_recording_900ms))?.sliceArray(441000 .. 882000)
        compareInterval(contentArray, recordedArray)
        /*
        recorder = Recorder(sampleRate, audioInChannel, audioEncoding, bufferSizeInBytes)
        speaker = Speaker(sampleRate, audioOutChannel, audioEncoding, bufferSizeInBytes)

        videoView = findViewById(R.id.videoView)
        val controller = MediaController(this)
        videoView.setMediaController(controller)
        videoView.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
        val path = "android.resource://" + packageName + "/" + R.raw.sample_video
        videoView.setVideoPath(path)
        videoView.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.setVolume(0F, 0F)
        }

        getAudioLatency()

        val audioFile = Wave.wavToShortArray(resources.openRawResource(R.raw.sample_sound))
        if (audioFile != null) {
            Log.i("LiveSync_MainActivity", "Audio file of size ${audioFile.size} loaded")
            audio = audioFile
        }
        else {
            Log.i("LiveSync_MainActivity","SHIT")
            finish()
        }

        speaker.setSource(audio)

        playButton.setOnClickListener {
            val offsetInSeconds = offsetText.text.toString().toDouble()
            val offsetInMilliseconds = (offsetInSeconds * 1000).toInt()

            speaker.setTime(offsetInMilliseconds)
            speaker.play()

            videoView.seekTo(offsetInMilliseconds)
            videoView.start()
        }

        resumeButton.setOnClickListener {
            speaker.play()
            videoView.start()
        }

        stopButton.setOnClickListener {
            speaker.stop()
            videoView.pause()
        }

        syncButton.setOnClickListener {
            speaker.muteOn()
            synchronize()
            speaker.setRelativeTime(timeInterval - audioLatency)
            videoView.seekTo(speaker.getTime())
            speaker.muteOff()
            speaker.play()
            videoView.start()
        }
        */
    }

    override fun onDestroy() {
        recorder.release()
        speaker.release()
        super.onDestroy()
    }

    private fun getAudioLatency() {
        val latency = FindLatency(recorder, speaker).getLatency()
        Log.i("LiveSync_MainActivity", "Audio latency: $latency")
        textLatency.text = latency.toString()

        audioLatency = latency ?: 0
    }

    private fun synchronize() {
        runBlocking {
            coroutineScope {
                val time = System.currentTimeMillis()

                val recordedArray : ShortArray?
                val contentArray : ShortArray?

                val recordWait = async {
                    recorder.getRecordedAudio(syncDuration)
                }
                val contentWait = async {
                    speaker.getOffset()
                }

                val offset = contentWait.await()

                recordedArray = recordWait.await()
                contentArray = audio.sliceArray(offset until offset + syncDuration)

                val elapsedTime = (System.currentTimeMillis() - time) / 1000.0
                Log.i("LiveSync_MainActivity", "synchronize: $elapsedTime seconds")

                timeInterval = compareInterval(contentArray, recordedArray)
            }
        }
    }

    private fun compareInterval(originalArray: ShortArray?, recordedArray: ShortArray?) : Int {
        return if (originalArray == null || recordedArray == null) {
            Log.e("LiveSync_MainActivity", "Converted array is null!")
            0
        } else {
            val d = CrossCorrelation.crossCorrelate(originalArray, recordedArray)
            val timeInterval = (d.toDouble() * 1000.0 / 44100.0).toInt()
            Log.i("LiveSync_MainActivity", "timeInterval: $timeInterval")
            runOnUiThread {
                textTDoA.text = timeInterval.toString()
            }
            timeInterval
        }
    }
}
