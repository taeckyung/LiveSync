package com.terry00123.livesync

import android.media.AudioRecord
import android.media.MediaRecorder
import java.util.concurrent.atomic.AtomicReference

class Recorder (sampleRate_: Int,
                audioChannel: Int,
                audioEncoding: Int,
                bufferSizeInBytes: Int) {
    val sampleRate = sampleRate_
    val bufferSize = bufferSizeInBytes / Short.SIZE_BYTES

    private val recorder = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        sampleRate,
        audioChannel,
        audioEncoding,
        bufferSizeInBytes
    )
    private val recordingThread : Thread

    private enum class RecorderState{
        IDLE, RECORDING, FINISHED
    }

    /* Variables shared among threads */
    private val waitObject = Object()
    private var currentState = AtomicReference<RecorderState>(RecorderState.IDLE)
    private var offset = 0
    private var micData = ShortArray(0)
    private var recordedTime = LongArray(0)
    private var timeRecording = false


    init {
        recorder.startRecording()
        recordingThread = Thread {
            kotlin.run {
                recordingThreadBody()
            }
        }
        recordingThread.priority = Thread.MAX_PRIORITY
        recordingThread.start()
    }

    fun release() {
        currentState.set(RecorderState.FINISHED)
        recordingThread.join()
        micData = ShortArray(0)
        recordedTime = LongArray(0)
        recorder.stop()
        recorder.release()
    }

    fun getRecordedAudio(bufSize_: Int) : ShortArray {
        return if (currentState.compareAndSet(RecorderState.IDLE, RecorderState.RECORDING)) {
            val bufSize = bufSize_ - bufSize_ % bufferSize

            offset = 0
            micData = ShortArray(bufSize)

            synchronized(waitObject) {
                waitObject.wait()
            }

            micData
        }
        else {
            throw error("More than two threads are attempting to access recorder.")
        }
    }

    fun getRecordedAudioWithTime(bufSize_: Int) : Pair<ShortArray, LongArray> {
        recordedTime = LongArray(bufSize_ / bufferSize)
        timeRecording = true
        val ret = Pair(getRecordedAudio(bufSize_), recordedTime)
        timeRecording = false
        return ret
    }

    private fun recordingThreadBody() {
        var finished = false
        val tempBuffer = ShortArray(bufferSize)
        while (!finished) {
            when (currentState.get()) {
                RecorderState.FINISHED -> {
                    finished = true
                }
                RecorderState.RECORDING -> {
                    if (offset < micData.size) {
                        recorder.read(micData, offset, bufferSize)
                        if (timeRecording)
                            recordedTime[offset/bufferSize] = System.currentTimeMillis()

                        offset += bufferSize
                    }
                    else {
                        currentState = AtomicReference(RecorderState.IDLE)
                        synchronized(waitObject) {
                            waitObject.notifyAll()
                        }
                    }
                }
                else -> {
                    /* We should read data from buffer to prevent recorder driver to be idle. */
                    recorder.read(tempBuffer, 0, bufferSize)
                }
            }
        }
    }
}