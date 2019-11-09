package com.example.testingaudiolatency

import android.content.pm.PackageManager
import android.media.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.media.AudioFormat.ENCODING_PCM_16BIT
import android.media.AudioFormat.CHANNEL_OUT_MONO
import android.media.AudioFormat.CHANNEL_IN_MONO
import android.media.MediaRecorder
import android.media.AudioManager
import android.media.AudioTrack
import android.media.AudioRecord
import android.util.Log
import android.widget.Button
import android.media.audiofx.BassBoost
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.media.audiofx.AcousticEchoCanceler
import android.media.AudioAttributes
import android.os.Build
import android.view.View
import com.example.cs442_hw2.FFT
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.reflect.Method
import kotlin.concurrent.timer
import kotlin.math.abs
import android.media.MediaPlayer
import android.os.SystemClock
import android.provider.Settings


class MainActivity : AppCompatActivity() {
    val Sampling_rate = 44100
    val Channel = AudioFormat.CHANNEL_IN_MONO
    val Encoding = AudioFormat.ENCODING_PCM_16BIT
    val recorder =  AudioRecord(MediaRecorder.AudioSource.MIC, Sampling_rate, Channel, Encoding, 2048)
    val BufferSize = 1024


    ////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////////////


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


    }

    override fun onResume() {
        super.onResume()
        requestPermissions()

        val mediaplayer = MediaPlayer.create(this, R.raw.sample)
        var start_time: Long = 0
        var end_time:Long = 0
        var sw = true


        start_btn.setOnClickListener(){
            mediaplayer.start()
            start_time = System.currentTimeMillis()
            sw = true
        }
        end_btn.setOnClickListener(){
            mediaplayer.pause()
        }



        timer(period = 1){
            requestPermissions()
            recorder.startRecording()
            val goal = 680
            val fft = make_FFT()
            val index = find_max_id(fft)
            val frequency = decode_frequency(index)
            val valid = valid_frequency(frequency, goal)

            if(valid && sw)
            {
                end_time = System.currentTimeMillis()
                runOnUiThread{

                    val tmp = Timediff.text.toString()
                    Timediff.text = tmp + "/"+(end_time - start_time).toString()
                    //Timediff.text = (end_time - start_time).toString()
                }
                sw = false
            }


            runOnUiThread{
                textView.text = frequency.toString()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()


    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////


    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    fun check_frequency(){

    }
    fun find_max(input: DoubleArray): Double{
        var max: Double = 0.0
        for(i in 0 .. input.size-1)
        {
            if(input[i] > max)
            {
                max = input[i]
            }
        }
        return max
    }
    fun find_max_id(input: DoubleArray): Int{
        var max = 0
        for(i in 0 .. input.size-1)
        {
            if(input[i] >= input[max])
            {
                max = i
            }
        }
        return max
    }
    fun make_FFT(): DoubleArray{

        var readData = ShortArray(BufferSize)
        val bytes = recorder.read(readData, 0, BufferSize)
        val fftresult = FFT(BufferSize).getFreqSpectrumFromShort(readData)

        return fftresult
    }
    fun decode_frequency(index: Int): Int{
        val tmp = (index * Sampling_rate / BufferSize).toInt()
        return tmp
    }
    fun valid_frequency(input: Int, goal: Int):Boolean{
        if(abs(input - goal) <= 30)
        {
            return true
        }
        return false
    }


    fun make_sound() {

    }
    fun get_from_mic(){

    }
    fun start_mic() {
        recorder.startRecording()
    }
    fun stop_mic() {
        recorder.stop()
    }
    fun requestPermissions() {
        val PERMISSION_REQUEST_CODE = 0
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_CODE
            )
        }
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE
            )
        }

    }
}
