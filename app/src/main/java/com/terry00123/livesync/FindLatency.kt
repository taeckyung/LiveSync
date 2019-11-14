package com.terry00123.livesync

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlin.math.abs


class FindLatency(){

    val Sampling_rate = 44100
    val Channel = AudioFormat.CHANNEL_IN_MONO
    val Encoding = AudioFormat.ENCODING_PCM_16BIT
    val recorder =  AudioRecord(MediaRecorder.AudioSource.MIC, Sampling_rate, Channel, Encoding, 1024)
    val BufferSize = 512
    val TargetFreq = 680

    var start_time: Long = 0
    var end_time: Long = 0
    var latency: Long = 0
    var ave_latency: Long = 0

    fun find_Latency(){

        val runnable = Thread(Latency_thread())
        runnable.start()

    }

    fun Latency_thread() = Runnable {

        recorder.startRecording()

        for(i in 0..4){
            val make_sound = GenerateTone().playSound(TargetFreq)
            val sound_Thread = Thread(make_sound)
            sound_Thread.start()

            start_time = System.currentTimeMillis()
            while (true) {
                val fft = make_FFT()
                val index = find_max_id(fft)
                val frequency = decode_frequency(index)
                val valid = valid_frequency(frequency, TargetFreq)

                if (valid) {
                    end_time = System.currentTimeMillis()
                    latency = end_time - start_time
                    ave_latency += latency
                    break
                }
            }
            Thread.sleep(2000)
        }
        ave_latency /= 5
        Log.e("Check Latency", "Ave_Latency: ${ave_latency}")

    }

    fun find_max_id(input: DoubleArray): Int{
        var max = 0
        for(i in 0 .. input.size-1)
        {
            if(input[i] >= input[max])
            {
                val tmp = input[max]
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