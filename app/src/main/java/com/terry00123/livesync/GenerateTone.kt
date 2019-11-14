package com.terry00123.livesync

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.provider.MediaStore
import android.util.Log
import java.util.logging.Handler
import kotlin.concurrent.thread


class GenerateTone(){
    val duration = 1
    val SampleRate = 44100
    val numSamples = duration * SampleRate
    val zero_tone = ShortArray(numSamples, {i -> 0})




    fun genTone(freq: Int): ShortArray {
        var generatedSnd: ShortArray = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val tmp = Math.sin(2.0 * Math.PI * i.toDouble() / (SampleRate / freq))
            generatedSnd[i] =
                (tmp * java.lang.Short.MAX_VALUE).toShort()  // Higher amplitude increases volume
        }
        return generatedSnd
    }


    fun playSound(freq: Int) = Runnable(){
        val buffer = genTone(freq)
        val audioTrack = AudioTrack(AudioManager.STREAM_MUSIC, SampleRate, AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT, buffer.size, AudioTrack.MODE_STATIC)

        audioTrack.write(buffer, 0 , buffer.size)
        audioTrack.play()

        play_zero()
    }

    fun play_zero(){
        val audioTrack = AudioTrack(AudioManager.STREAM_MUSIC, SampleRate, AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT, zero_tone.size, AudioTrack.MODE_STATIC)

        audioTrack.write(zero_tone, 0 , zero_tone.size)
        audioTrack.play()
    }


}