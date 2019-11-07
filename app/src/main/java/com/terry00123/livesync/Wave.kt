package com.terry00123.livesync

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/*
 * Written by Taeckyung LEE.
 * Functions for reading wave file into short array and playing them (for debugging purpose)
 */
object Wave{
    private val HEADER_SIZE = 44
    private val FORMAT : Short = 1
    private val CHANNELS : Short = 1
    private val RATE = 44100
    private val BITS : Short = 16
    private val DATA_HEADER = 0x61746164
    /*
     * https://mindtherobot.com/blog/580/android-audio-play-a-wav-file-on-an-audiotrack/
     */
    fun wavToShortArray(wavStream: InputStream) : ShortArray? {
        val buffer = ByteBuffer.allocate(HEADER_SIZE)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        wavStream.read(buffer.array(), buffer.arrayOffset(), buffer.capacity())

        buffer.rewind()
        buffer.position(buffer.position() + 20)

        val format = buffer.short
        val channels = buffer.short
        val rate = buffer.int

        buffer.position(buffer.position() + 6)

        val bits = buffer.short
        if (format != FORMAT || channels != CHANNELS || rate != RATE || bits != BITS)
            return null

        // Until "data" header appears
        while (buffer.int != DATA_HEADER) {
            val size = buffer.int
            wavStream.skip(size.toLong())
            buffer.rewind()
            wavStream.read(buffer.array(), buffer.arrayOffset(), 8)
            buffer.rewind()
        }
        val dataSize = buffer.int

        if (dataSize <= 0 || dataSize % 2 != 0)
            return null

        val byteArray = ByteArray(dataSize)
        wavStream.read(byteArray, 0, dataSize)

        val shortArray = ShortArray(dataSize / 2)
        for (i in 0 until dataSize / 2) {
            shortArray[i] = (byteArray[2*i] + (byteArray[2*i+1].toInt() shl 8)).toShort()
        }
        return shortArray
    }

    fun playShortArray(array: ShortArray?) {
        if (array == null) return

        val player = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(44100)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build())
            .setBufferSizeInBytes(512)
            .build()

        player.play()

        var offset = 0

        while (offset < array.size) {
            player.write(array, offset, 512)
            offset += 512
        }

        player.stop()
        player.release()
    }
}