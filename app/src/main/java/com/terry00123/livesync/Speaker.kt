package com.terry00123.livesync

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

class Speaker {
    val sampleRate = 44100
    val bufferSize = 512
    private val bufferSizeInBytes = bufferSize * Short.SIZE_BYTES

    private val player : AudioTrack
    private val playerThread : Thread
    private var isPlaying : Boolean

    private val zeroTone : ShortArray = ShortArray(bufferSize) {0}

    private enum class SpeakerState{
        IDLE, PLAYING, FINISHED
    }

    /* Variables shared among threads */
    private val waitObject = Object()
    private var currentState = AtomicReference<SpeakerState>(SpeakerState.IDLE)
    //private val bufferQueue : ConcurrentLinkedQueue<ShortArray> = ConcurrentLinkedQueue()
    private var source : ShortArray = ShortArray(bufferSize) {0}
    private var offset = 0
    private var flushed = false

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
            .build()

        player.play()
        isPlaying = true

        playerThread = Thread {
            kotlin.run {
                playerThreadBody()
            }
        }
        playerThread.start()

    }

    fun release() {
        if (isPlaying) {
            isPlaying = false
            //playerThread.join()
            player.stop()
            player.release()
        }
    }

    fun play(shortArray: ShortArray, offsetInShorts: Short) {
        var size = shortArray.size - offsetInShorts
        size -= size % bufferSize
        //bufferQueue.add(shortArray.copyOf(size))
        Log.i("myTag", "Size $size added.")

        when (currentState.get()) {
            SpeakerState.IDLE -> {
                source = shortArray.sliceArray(offsetInShorts until offsetInShorts+size)

                offset = 0
                currentState = AtomicReference(SpeakerState.PLAYING)

                synchronized(waitObject) {
                    waitObject.wait()
                }

                currentState = AtomicReference(SpeakerState.IDLE)
            }
            else -> throw error("More than two threads are attempting to access speaker.")
        }
    }

    fun flush() {
        flushed = true
    }

    private fun playerThreadBody() {
        while (isPlaying) {
            when (currentState.get()) {
                SpeakerState.PLAYING -> {
                    if (offset < source.size && !flushed) {
                        player.write(source, offset, bufferSize)
                        offset += bufferSize
                    }
                    else {
                        currentState = AtomicReference(SpeakerState.FINISHED)
                        synchronized(waitObject) {
                            waitObject.notifyAll()
                        }
                        flushed = false
                    }
                }
                else -> player.write(zeroTone, 0, bufferSize)
            }
        }
    }

}