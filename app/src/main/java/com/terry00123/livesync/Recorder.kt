package com.terry00123.livesync

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class Recorder {
    val sampleRate = 44100
    val bufferSize = 512
    private val bufferSizeInBytes = bufferSize * Short.SIZE_BYTES

    private val recorder : AudioRecord
    private val recordingThread : Thread
    private var isRecording : Boolean

    private enum class RecorderState{
        IDLE, RECORDING, FINISHED
    }

    /* Variables shared among threads */
    private val waitObject = Object()
    private var currentState = AtomicReference<RecorderState>(RecorderState.IDLE)
    private var offset = 0
    private var recordedTime = LongArray(0)
    private var micData = ShortArray(0)


    init {
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSizeInBytes
        )
        recorder.startRecording()
        isRecording = true
        recordingThread = Thread {
            kotlin.run {
                recordingThreadBody()
            }
        }
        recordingThread.start()
    }

    fun release() {
        if (isRecording) {
            isRecording = false
            recordingThread.join()
            recorder.stop()
            recorder.release()
        }
    }

    fun getRecordedAudio(bufSize_: Int) : ShortArray {
        return when (currentState.get()) {
            RecorderState.IDLE -> {
                val bufSize = bufSize_ - bufSize_ % bufferSize

                offset = 0
                micData = ShortArray(bufSize)
                currentState = AtomicReference(RecorderState.RECORDING)

                synchronized(waitObject) {
                    waitObject.wait()
                }

                currentState = AtomicReference(RecorderState.IDLE)

                micData
            }
            else -> throw error("More than two threads are attempting to access recorder.")
        }
    }

    fun getRecordedAudioWithTime(bufSize_: Int) : Pair<ShortArray, LongArray> {
        recordedTime = LongArray(bufSize_ / bufferSize)
        return Pair(getRecordedAudio(bufSize_), recordedTime)
    }

    private fun recordingThreadBody() {
        val tempBuffer = ShortArray(bufferSize)
        while (isRecording) {
            when (currentState.get()) {
                RecorderState.RECORDING -> {
                    if (offset < micData.size) {
                        recorder.read(micData, offset, bufferSize)
                        recordedTime[offset/bufferSize] = System.currentTimeMillis()

                        offset += bufferSize
                    }
                    else {
                        currentState = AtomicReference(RecorderState.FINISHED)
                        synchronized(waitObject) {
                            waitObject.notifyAll()
                        }
                    }
                }
                else -> { // IDLE or FINISHED
                    /*
                     * We should read data from buffer even if the data is useless
                     * https://stackoverflow.com/questions/12002031/what-will-happen-when-the-number
                     * -of-data-which-is-sampled-through-audio-exceed-t/12002230#12002230
                     */
                    recorder.read(tempBuffer, 0, bufferSize)
                }
            }
        }
    }
}