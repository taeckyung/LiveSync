package com.terry00123.livesync

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class Recorder {
    private val sampleRate = 44100
    private val bufferSize = 512
    private val bufferSizeInBytes = bufferSize * Short.SIZE_BYTES

    private val recorder : AudioRecord
    private val recordingThread : Thread
    private var isRecording = AtomicBoolean(false)

    private enum class RecorderState{
        IDLE, RECORDING
    }
    private var currentState = AtomicReference<RecorderState>(RecorderState.IDLE)
    private var offset = 0
    private var maxOffset = 0
    private val waitObject = Object()
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
        isRecording.set(true)
        recordingThread = Thread {
            kotlin.run {
                recordingThreadBody()
            }
        }
        recordingThread.start()
    }

    fun release() {
        if (isRecording.getAndSet(false)) {
            recordingThread.join()
            recorder.stop()
            recorder.release()
        }
    }

    fun getRecordedAudio(milliseconds: Int) : ShortArray? {
        return when (currentState.get()) {
            RecorderState.RECORDING -> null
            RecorderState.IDLE -> {
                offset = 0
                maxOffset = milliseconds * sampleRate / 1000
                maxOffset -= (maxOffset % bufferSize)
                micData = ShortArray(maxOffset)
                currentState = AtomicReference(RecorderState.RECORDING)

                synchronized(waitObject) {
                    waitObject.wait()
                }

                micData
            }
            else -> null
        }
    }

    private fun recordingThreadBody() {
        while (isRecording.get()) {
            when (currentState.get()) {
                RecorderState.IDLE -> {}
                RecorderState.RECORDING -> {
                    if (offset < maxOffset) {
                        recorder.read(micData, offset, bufferSize)
                        offset += bufferSize
                    }
                    else {
                        currentState = AtomicReference(RecorderState.IDLE)
                        synchronized(waitObject) {
                            waitObject.notifyAll()
                        }
                    }
                }
                else -> {}
            }
        }
    }
}