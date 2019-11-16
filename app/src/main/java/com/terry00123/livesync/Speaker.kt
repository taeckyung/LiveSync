package com.terry00123.livesync

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.measureTimeMillis

class Speaker {
    val sampleRate = 44100
    val bufferSize = 512
    private val bufferSizeInBytes = bufferSize * Short.SIZE_BYTES

    private val player : AudioTrack
    private val playerThread : Thread

    private val zeroTone : ShortArray = ShortArray(bufferSize) {0}

    private enum class SpeakerState{
        IDLE, PLAYING, FINISHED
    }

    /* Variables shared among threads */
    private val waitObject = Object()
    private var currentState = AtomicReference<SpeakerState>(SpeakerState.IDLE)

    private var source : ShortArray = ShortArray(bufferSize) {0}
    private var offset = 0

    private val toneList = ArrayList<Pair<ShortArray, Int>>()
    private var flushed = AtomicBoolean(false)

    init {
        player = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
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

    fun play(shortArray: ShortArray, offsetInShorts: Short, immediateReturn: Boolean) {
        var size = shortArray.size - offsetInShorts
        size -= size % bufferSize
        Log.i("LiveSync_Speaker", "play: Size $size added.")

        if (currentState.compareAndSet(SpeakerState.IDLE, SpeakerState.PLAYING)) {
            source = shortArray.sliceArray(offsetInShorts until offsetInShorts+size)

            offset = 0
            currentState = AtomicReference(SpeakerState.PLAYING)

            if (!immediateReturn) {
                synchronized(waitObject) {
                    waitObject.wait()
                }
            }
        }
        else {
            throw error("More than two threads are attempting to access speaker.")
        }
    }

    fun addToneImmediate(freq: Int, milliseconds: Int) {
        val tone = Tone.generateFreq(freq, sampleRate, bufferSize)
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

    private fun playerThreadBody() {
        var finished = false
        while (!finished) {
            var array : ShortArray? = null

            when (currentState.get()) {
                SpeakerState.FINISHED -> {
                    finished = true
                }
                SpeakerState.PLAYING -> {
                    if (offset < source.size && !flushed.compareAndSet(true, false)) {
                        array = source.sliceArray(offset until offset+bufferSize)
                        offset += bufferSize
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