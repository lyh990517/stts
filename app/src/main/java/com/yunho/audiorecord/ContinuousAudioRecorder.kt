package com.yunho.audiorecord

import android.Manifest
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.abs

class AutoStopAudioRecorder {
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    val amplitude = MutableStateFlow<Int>(0)

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording() = callbackFlow<ByteArray> {
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )
        audioRecord?.startRecording()
        isRecording = true

        val outputStream = ByteArrayOutputStream()

        withContext(Dispatchers.Default) {
            val buffer = ByteArray(bufferSize)
            var silenceStartTime: Long? = null

            while (isRecording) {
                audioRecord?.read(buffer, 0, buffer.size)?.let { readBytes ->
                    outputStream.write(buffer, 0, readBytes)

                    var maxAmplitude = buffer.getMaxAmplitude(readBytes)

                    amplitude.update { maxAmplitude }

                    if (maxAmplitude < SILENCE_THRESHOLD) {
                        if (silenceStartTime == null) {
                            silenceStartTime = System.currentTimeMillis()
                        } else {
                            val silenceTime = System.currentTimeMillis() - silenceStartTime

                            if (silenceTime >= SILENCE_DURATION) {
                                isRecording = false
                            }
                        }
                    } else {
                        silenceStartTime = null
                    }
                }
            }

            trySend(outputStream.toByteArray())
        }

        awaitClose {
            isRecording = false
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        }
    }

    private fun ByteArray.getMaxAmplitude(readBytes: Int): Int {
        var maxAmplitude = 0

        /**
         * Converts two bytes stored in little-endian order into a 16-bit value.
         *
         * Example:
         *   Original value: 0x1234
         *
         *   Little-endian storage:
         *     - Low byte (index 0):  0x34
         *     - High byte (index 1): 0x12
         *
         *   Reconstruction steps:
         *     1. Shift the high byte left by 8 bits: 0x12 << 8 = 0x1200
         *     2. Combine with the low byte using bitwise OR:
         *        0x1200 | 0x34 = 0x1234
         */
        for (i in 0 until readBytes step 2) {
            val low = this[i].toInt() and 0xFF
            val high = this[i + 1].toInt() shl 8
            val combined = high or low
            val original = combined.toShort()

            maxAmplitude = maxAmplitude.coerceAtLeast(abs(original.toInt()))
        }

        return maxAmplitude
    }

    companion object {
        private const val SILENCE_THRESHOLD = 4000
        private const val SILENCE_DURATION = 2000L

        fun ByteArray.play() {
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_OUT_MONO
            val audioEncoding = AudioFormat.ENCODING_PCM_16BIT

            val minBufferSize =
                AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioEncoding)

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioEncoding)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack.play()
            audioTrack.write(this, 0, size)
            audioTrack.stop()
            audioTrack.release()
        }
    }
}