package com.example.volumedetection

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.log10
import kotlin.math.max

interface SoundThresholdListener {
    fun onThresholdExceeded()
    fun onVolumeChanged(dbLevel: Double)
    fun onPlaybackStarted()
    fun onPlaybackFinished()
    fun onVolumeBackToNormal()  // 新增：声音恢复正常时的回调
}

class SoundDetector(
    private val context: Context,
    private val threshold: Double,
    private val listener: SoundThresholdListener
) {
    companion object {
        private const val TAG = "SoundDetector"
        private const val SAMPLE_RATE = 44100
        private const val BUFFER_SIZE_FACTOR = 4
        private const val TRIGGER_COUNT = 3  // 连续超过阈值多少次触发
    }

    private var audioRecord: AudioRecord? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isDetecting = false
    private var isPaused = false
    private var bufferSize = 0

    fun start() {
        if (isDetecting) {
            Log.w(TAG, "Already detecting, ignoring start request")
            return
        }

        Log.d(TAG, "Starting sound detection with threshold: $threshold dB")
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * BUFFER_SIZE_FACTOR
        
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Failed to initialize AudioRecord")
            audioRecord = null
            return
        }

        audioRecord?.startRecording()
        isDetecting = true
        isPaused = false
        Log.i(TAG, "Sound detection started successfully")

        scope.launch {
            detectSound()
        }
    }

    private suspend fun detectSound() {
        val buffer = ShortArray(bufferSize)
        var exceedCount = 0
        var lastTriggerTime = 0L
        val minTriggerInterval = 2000L  // 最小触发间隔（毫秒）
        var wasExceeding = false  // 标记之前是否超过阈值

        Log.d(TAG, "Detection loop started")

        while (isDetecting && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            if (isPaused) {
                Log.d(TAG, "Detection paused")
                delay(100)
                continue
            }

            val read = audioRecord?.read(buffer, 0, bufferSize) ?: -1
            if (read > 0) {
                val dbLevel = calculateDB(buffer, read)
                
                // 实时通知音量变化
                withContext(Dispatchers.Main) {
                    listener.onVolumeChanged(dbLevel)
                }

                // 检查是否超过阈值
                if (dbLevel > threshold) {
                    exceedCount++
                    val currentTime = System.currentTimeMillis()
                    
                    Log.v(TAG, "Volume exceeded threshold: ${dbLevel.toInt()} dB, count: $exceedCount")
                    
                    // 防止频繁触发，需要间隔一定时间
                    if (exceedCount >= TRIGGER_COUNT && (currentTime - lastTriggerTime) > minTriggerInterval) {
                        Log.i(TAG, "🎯 THRESHOLD EXCEEDED! Volume: ${dbLevel.toInt()} dB, Count: $exceedCount")
                        
                        // 立即暂停检测，防止连续触发
                        isPaused = true
                        Log.d(TAG, "⏸️ Detection paused to prevent multiple triggers")
                        
                        withContext(Dispatchers.Main) {
                            listener.onThresholdExceeded()
                        }
                        exceedCount = 0
                        lastTriggerTime = currentTime
                        wasExceeding = true
                    }
                } else {
                    // 声音回落到阈值以下
                    if (wasExceeding) {
                        Log.i(TAG, "✅ Volume back to normal: ${dbLevel.toInt()} dB - Resetting state")
                        withContext(Dispatchers.Main) {
                            listener.onVolumeBackToNormal()
                        }
                        wasExceeding = false
                    }
                    if (exceedCount > 0) {
                        Log.v(TAG, "Volume back to normal: ${dbLevel.toInt()} dB, reset count")
                    }
                    exceedCount = 0
                }
            } else if (read < 0) {
                Log.e(TAG, "Error reading from AudioRecord: $read")
            }
        }
        
        Log.d(TAG, "Detection loop ended")
    }

    private fun calculateDB(buffer: ShortArray, size: Int): Double {
        var sum = 0.0
        for (i in 0 until size) {
            val sample = buffer[i] / 32768.0
            sum += sample * sample
        }
        val rms = Math.sqrt(sum / size)
        val db = 20.0 * log10(max(rms, 1e-10))
        return (db + 120).coerceIn(0.0, 120.0)
    }

    fun pauseDetection() {
        Log.d(TAG, "Pausing detection")
        isPaused = true
    }

    fun resumeDetection() {
        Log.d(TAG, "Resuming detection")
        isPaused = false
    }

    fun stop() {
        Log.d(TAG, "Stopping detection")
        isDetecting = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            Log.i(TAG, "AudioRecord released successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord: ${e.message}")
        }
        audioRecord = null
        scope.cancel()
        Log.i(TAG, "Sound detection stopped")
    }
}
