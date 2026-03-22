package com.example.volumedetection

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import android.util.Log

class MusicService(
    private val context: Context,
    private val musicUri: Uri? = null,  // 用户选择的音乐 URI，null 表示使用默认
    private val musicVolume: Int
) {
    companion object {
        private const val TAG = "MusicService"
    }
    
    private var mediaPlayer: MediaPlayer? = null
    private var originalVolume = 0
    private var wakeLock: PowerManager.WakeLock? = null
    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    private val powerManager: PowerManager by lazy {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    fun playMusic() {
        try {
            Log.i(TAG, "▶️ Starting music playback")
            
            // 获取 WakeLock
            if (wakeLock == null) {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "MusicService:WakeLock"
                )
            }
            wakeLock?.acquire(60*1000L) // 最多持有 60 秒
            Log.d(TAG, "WakeLock acquired")
            
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val targetVolume = (maxVolume * musicVolume / 100)
            Log.d(TAG, "Setting volume: $musicVolume% -> $targetVolume / $maxVolume")
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)

            mediaPlayer = MediaPlayer().apply {
                setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                
                // 如果有用户选择的 URI，使用它；否则使用内置默认铃声
                if (musicUri != null) {
                    Log.d(TAG, "Using custom user-selected music")
                    // 使用用户选择的音乐
                    val fd = context.contentResolver.openFileDescriptor(musicUri, "r")
                    setDataSource(fd?.fileDescriptor)
                    fd?.close()
                } else {
                    Log.d(TAG, "Using built-in default alarm sound")
                    // 使用内置默认铃声
                    try {
                        val afd = context.resources.openRawResourceFd(R.raw.default_alarm)
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.declaredLength)
                        // 不要立即关闭，让 MediaPlayer 管理
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading default alarm: ${e.message}")
                        throw e
                    }
                }
                
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                
                setOnCompletionListener {
                    Log.i(TAG, "⏹️ Music playback completed")
                    release()
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
                    
                    // 释放 WakeLock
                    if (wakeLock?.isHeld == true) {
                        wakeLock?.release()
                        Log.d(TAG, "WakeLock released")
                    }
                    
                    VolumeDetectionApp.listener?.onPlaybackFinished()
                }
                
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "❌ MediaPlayer error: what=$what, extra=$extra")
                    // 播放出错也要通知恢复检测
                    release()
                    if (wakeLock?.isHeld == true) {
                        wakeLock?.release()
                        Log.d(TAG, "WakeLock released in error handler")
                    }
                    VolumeDetectionApp.listener?.onPlaybackFinished()
                    true
                }
                
                setOnPreparedListener {
                    Log.d(TAG, "MediaPlayer prepared, starting playback...")
                    start()
                    VolumeDetectionApp.listener?.onPlaybackStarted()
                    Log.i(TAG, "🔊 Music is now playing")
                }
                
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error playing music: ${e.message}", e)
            e.printStackTrace()
            // 播放失败也要通知恢复检测
            VolumeDetectionApp.listener?.onPlaybackFinished()
        }
    }

    fun stop() {
        Log.d(TAG, "Stopping music playback")
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    Log.d(TAG, "Stopping MediaPlayer")
                    stop()
                }
                Log.d(TAG, "Releasing MediaPlayer")
                release()
            }
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
            
            // 释放 WakeLock
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "WakeLock released in stop()")
            }
            
            Log.i(TAG, "Music stopped and volume restored")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping music: ${e.message}")
        }
        mediaPlayer = null
    }

    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying ?: false
        } catch (e: Exception) {
            Log.d(TAG, "Error checking isPlaying: ${e.message}")
            false
        }
    }
}
