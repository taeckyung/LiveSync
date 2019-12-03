package com.terry00123.livesync

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Runnable
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {
    private val PERMISSION_GRANTED = 1

    private val sampleRate = 44100
    private val audioInChannel = AudioFormat.CHANNEL_IN_MONO
    private val audioOutChannel = AudioFormat.CHANNEL_OUT_MONO
    private val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
    private var bufferSizeInBytes = 1024
    
    private lateinit var recorder : Recorder
    private lateinit var speaker : Speaker
    private lateinit var audio : ShortArray

    private lateinit var bluetooth: Bluetooth

    private var isSyncThreadRunning = AtomicBoolean(false)

    private var syncDuration = 10.0
    private var audioLatency = 0
    private var timeInterval = 0
    private var propDelay = 0

    //for test
    private lateinit var handler : Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_GRANTED)
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
            PERMISSION_GRANTED -> {
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

        bluetooth = Bluetooth(this)

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
        speaker.muteOn()
        speaker.addBeepSound(100, 3000)

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
            bluetooth.setUnSynchronized()
        }

        syncButton.setOnClickListener {
            Thread {
                kotlin.run {
                    onSyncButtonClicked()
                }
            }.start()
        }

        leadingButton.setOnClickListener {
            speaker.muteOff()
            bluetooth.setSynchronized()
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
        bluetooth.release()
        videoView.stopPlayback()
        audio = ShortArray(0)
        super.onDestroy()
    }

    private fun onSyncButtonClicked() {
        if (!isSyncThreadRunning.compareAndSet(false, true)) return

        runOnUiThread {
            progressBar.visibility = View.VISIBLE
            progressBar.invalidate()
        }
        syncDuration = rangeText.text.toString().toDouble() * 2

        timeInterval = getTDoA(recorder, speaker, sampleRate, audio, syncDuration)
        propDelay = bluetooth.getMaxPropDelay()

        Log.i(
            "LiveSync_MainActivity",
            "setRelativeTime: ${audioLatency - timeInterval}"
        )
        Log.i("LiveSync_MainActivity", "currentSpeakerTime: ${speaker.getTime()}")

        speaker.setRelativeTime(audioLatency - timeInterval + propDelay)
        videoView.seekTo(speaker.getTime() + audioLatency)
        speaker.muteOff()

        Log.i("LiveSync_MainActivity", "currentSpeakerTime: ${speaker.getTime()}")
        bluetooth.setSynchronized()
        runOnUiThread {
            progressBar.visibility = View.INVISIBLE
            progressBar.invalidate()
            textTDoA.text = timeInterval.toString()
            textPropDelay.text = propDelay.toString()
        }

        isSyncThreadRunning.set(false)
    }
}
