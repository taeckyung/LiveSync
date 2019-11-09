package com.example.myapplication

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import java.util.concurrent.Delayed
import kotlin.concurrent.timer
import kotlin.math.abs


class FindLatency(){

    val Sampling_rate = 44100
    val Channel = AudioFormat.CHANNEL_IN_MONO
    val Encoding = AudioFormat.ENCODING_PCM_16BIT
    val recorder =  AudioRecord(MediaRecorder.AudioSource.MIC, Sampling_rate, Channel, Encoding, 2048)
    val BufferSize = 1024

    // This is what we will using, return: Audio Latency
    fun findlatency(mediaplayer: MediaPlayer, sleep_time: Long): Long{
        var start_time: Long = 0
        var end_time:Long = 0
        var sw = false


        mediaplayer.start()
        start_time = System.currentTimeMillis()

        kotlin.concurrent.timer(period = 2){
            recorder.startRecording()
            val goal = 680
            val fft = make_FFT()
            val index = find_max_id(fft)
            val frequency = decode_frequency(index)
            val valid = valid_frequency(frequency, goal)


            if(valid) {
                end_time = System.currentTimeMillis()
                recorder.stop()
                mediaplayer.stop()
                sw = true
            }
        }
        Thread.sleep(sleep_time)



        return (end_time - start_time)

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
    fun valid_frequency(input: Int, goal: Int):Boolean{
        if(abs(input - goal) <= 30)
        {
            return true
        }
        return false
    }
    fun decode_frequency(index: Int): Int{
        val tmp = (index * Sampling_rate / BufferSize).toInt()
        return tmp
    }
}