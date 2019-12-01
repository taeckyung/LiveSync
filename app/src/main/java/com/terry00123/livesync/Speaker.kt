package com.terry00123.livesync

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.measureTimeMillis

class Speaker (sampleRate_: Int,
               audioChannel: Int,
               audioEncoding: Int,
               bufferSizeInBytes: Int) {
    val sampleRate = sampleRate_
    val bufferSize = bufferSizeInBytes / Short.SIZE_BYTES

    private val player : AudioTrack
    private val playerThread : Thread

    private val zeroTone : ShortArray = ShortArray(bufferSize) {1}

    private enum class SpeakerState {
        IDLE, PLAYING, FINISHED
    }

    /* Variables shared among threads */
    private val waitObject = Object()
    private var currentState = AtomicReference<SpeakerState>(SpeakerState.IDLE)

    private var source : ShortArray = ShortArray(bufferSize) {0}
    private var offset = AtomicInteger(0)
    private var muted = false

    private val toneList = ArrayList<Pair<ShortArray, Int>>()

    init {
        player = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioEncoding)
                    .setSampleRate(sampleRate)
                    .setChannelMask(audioChannel)
                    .build())
            .setBufferSizeInBytes(bufferSizeInBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        player.play()

        playerThread = Thread {
            kotlin.run {
                playerThreadBody()
            }
        }
        playerThread.priority = Thread.MAX_PRIORITY
        playerThread.start()

    }

    fun release() {
        currentState.set(SpeakerState.FINISHED)
        playerThread.join()
        player.stop()
        player.release()
    }

    private fun timeToOffset(msInInt: Int) : Int {
        return (msInInt.toDouble() * sampleRate / 1000).toInt()
    }

    private fun offsetToTime(offsetInInt: Int) : Int {
        return (offsetInInt.toDouble() * 1000 / sampleRate).toInt()
    }

    fun setSource(shortArray: ShortArray) {
        source = shortArray.copyOf()
        offset.set(0)
    }

    fun setOffset(offsetInInt: Int) {
        offset.set(offsetInInt)
    }

    fun setRelativeOffset(offsetInInt: Int) {
        offset.addAndGet(offsetInInt)
    }

    fun setTime(msInInt: Int) {
        setOffset(timeToOffset(msInInt))
    }

    fun setRelativeTime(msInInt: Int) {
        setRelativeOffset(timeToOffset(msInInt))
    }

    fun getOffset() : Int {
        return offset.get()
    }

    fun getTime() : Int {
        return offsetToTime(getOffset())
    }

    fun playAwait() {
        currentState.set(SpeakerState.PLAYING)

        synchronized(waitObject) {
            waitObject.wait()
        }
    }

    fun play() {
        currentState.set(SpeakerState.PLAYING)
    }

    fun stop()  {
        currentState.set(SpeakerState.IDLE)
    }

    fun muteOn() {
        muted = true
    }

    fun muteOff() {
        muted = false
    }

/*
    fun addToneImmediate(freq: Int, milliseconds: Int, amplitude: Double) {
        val tone = Tone.generateFreq(freq, sampleRate, bufferSize, amplitude)
        val count = milliseconds * sampleRate / bufferSize / 1000
        toneList.add(Pair(tone, count))
        Log.i("LiveSync_Speaker", "addToneImmediate: tone $freq hz of duration $count added.")
    }

    fun flush() {
        flushed.set(true)
        synchronized(waitObject) {
            waitObject.wait()
        }
    }
 */

    private fun playerThreadBody() {
        var finished = false
        while (!finished) {
            var array : ShortArray? = null

            when (currentState.get()) {
                SpeakerState.FINISHED -> {
                    finished = true
                }
                SpeakerState.PLAYING -> {
                    val offsetNow = offset.get()
                    val offsetNext = offsetNow + bufferSize
                    if (offsetNext <= source.size) {
                        array = if (!muted) {
                            source.sliceArray(offsetNow until offsetNext)
                        }
                        else {
                            zeroTone.copyOf()
                        }
                        offset.compareAndSet(offsetNow, offsetNext)
                    }
                    else {
                        currentState = AtomicReference(SpeakerState.IDLE)
                        synchronized(waitObject) {
                            waitObject.notifyAll()
                        }
                    }
                }
                else -> {
                    array = zeroTone.copyOf()
                }
            }

            if (array != null) {
                if (toneList.size != 0) {
                    val time = measureTimeMillis { addToneToArray(array) }
                    Log.i("LiveSync_Speaker", "addToneToArray: $time ms.")
                }
                player.write(array, 0, bufferSize)
            }
        }
    }

    private fun addToneToArray(array: ShortArray) {
        for (i in 0 until bufferSize) {
            var value: Int = array[i].toInt()
            for (tone in toneList) {
                value += tone.first[i]
            }
            value = value.coerceIn(java.lang.Short.MIN_VALUE..java.lang.Short.MAX_VALUE)
            array[i] = value.toShort()
        }
        for (i in toneList.indices.reversed()) {
            if (toneList[i].second == 1) {
                toneList.removeAt(i)
            }
            else {
                toneList[i] = toneList[i].copy(toneList[i].first, toneList[i].second - 1)
            }
        }
    }

}