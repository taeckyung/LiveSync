package com.terry00123.livesync

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max

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

    private class ToneInfo(val tone: ShortArray,
                           val toneSize: Int,
                           val interval: Int) {
        fun getValueAt(i: Int, offset: Int) : Short {
            val index = offset % interval
            return if (index + i < toneSize) {
                tone[index + i]
            } else {
                0
            }
        }
    }
    private val toneList = ArrayList<ToneInfo>()

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
            /*.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)*/
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
        source = ShortArray(bufferSize) {0}
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

    fun addBeepSound(duration: Int, repeatInterval: Int) {
        val count = max(1, duration * sampleRate / 1000)
        val interval = repeatInterval * sampleRate / 1000
        val array = Tone.generateFreq(11000, sampleRate, count)
        Log.i("LiveSync_Speaker", "addBeepSound: $duration, $repeatInterval")
        toneList.add(ToneInfo(array, count, interval))
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
                        if (!muted) {
                            array = source.sliceArray(offsetNow until offsetNext)
                            if (toneList.size != 0) {
                                addToneToArray(array, offsetNow)
                            }
                        }
                        else {
                            array = zeroTone.copyOf()
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
                player.write(array, 0, bufferSize)
            }
        }
    }

    private fun addToneToArray(array: ShortArray, offset: Int) {
        for (i in 0 until bufferSize) {
            var value: Int = array[i].toInt()
            for (tone in toneList) {
                value += tone.getValueAt(i, offset)
            }
            value = value.coerceIn(java.lang.Short.MIN_VALUE..java.lang.Short.MAX_VALUE)
            array[i] = value.toShort()
        }
    }

}