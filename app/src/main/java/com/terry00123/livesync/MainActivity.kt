package com.terry00123.livesync

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.lang.Runnable

class MainActivity : AppCompatActivity() {
    private val MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1

    private val sampleRate = 44100
    private val audioInChannel = AudioFormat.CHANNEL_IN_MONO
    private val audioOutChannel = AudioFormat.CHANNEL_OUT_MONO
    private val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
    private var bufferSizeInBytes = 1024
    
    private lateinit var recorder : Recorder
    private lateinit var speaker : Speaker
    private lateinit var audio : ShortArray

    private var syncDuration = 10.0
    private var audioLatency = 0
    private var timeInterval = 0

    //for test
    private lateinit var handler : Handler

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
        recorder = Recorder(sampleRate, audioInChannel, audioEncoding, bufferSizeInBytes)
        speaker = Speaker(sampleRate, audioOutChannel, audioEncoding, bufferSizeInBytes)

        val controller = MediaController(this)
        videoView.setMediaController(controller)
        videoView.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
        val path = "android.resource://" + packageName + "/" + R.raw.sample_video
        videoView.setVideoPath(path)
        videoView.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.setVolume(0F, 0F)
        }

        val latency = FindLatency(recorder, speaker).getLatency()
        Log.i("LiveSync_MainActivity", "Audio latency: $latency")
        textLatency.text = latency.toString()
        audioLatency = latency ?: 0

        val audioFile = Wave.wavToShortArray(resources.openRawResource(R.raw.sample_sound))
        if (audioFile != null) {
            Log.i("LiveSync_MainActivity", "Audio file of size ${audioFile.size} loaded")
            audio = audioFile
        }
        else {
            Log.e("LiveSync_MainActivity","Can't load audio file")
            finish()
        }

        speaker.setSource(audio)

        playButton.setOnClickListener {
            val offsetInMilliseconds = (offsetText.text.toString().toDouble() * 1000).toInt()

            speaker.setTime(offsetInMilliseconds)
            speaker.play()

            videoView.seekTo(offsetInMilliseconds + audioLatency)
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
            syncDuration = rangeText.text.toString().toDouble() * 2 * 1.25

            speaker.muteOn()

            timeInterval = getTDoA(recorder, speaker, sampleRate, audio, syncDuration)
            Log.i("LiveSync_MainActivity", "setRelativeTime: ${audioLatency - timeInterval}")
            Log.i("LiveSync_MainActivity", "currentSpeakerTime: ${speaker.getTime()}")
            speaker.setRelativeTime(audioLatency - timeInterval)
            videoView.seekTo(speaker.getTime() + audioLatency)
            speaker.muteOff()
            Log.i("LiveSync_MainActivity", "currentSpeakerTime: ${speaker.getTime()}")

            textTDoA.text = timeInterval.toString()
        }

        handler = Handler()

        timeChecker.run()
    }

    private val timeChecker = object : Runnable {
        override fun run() {
            try {
                Log.i("LiveSync_Test", "Speaker time: ${speaker.getTime()}")
            } finally {
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(timeChecker)
        recorder.release()
        speaker.release()
        videoView.stopPlayback()
        super.onDestroy()
    }
}
