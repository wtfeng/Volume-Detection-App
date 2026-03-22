package com.example.volumedetection

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var currentVolumeText: TextView
    private lateinit var volumeProgressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var thresholdEdit: TextInputEditText
    private lateinit var musicVolumeEdit: TextInputEditText
    private lateinit var selectMusicBtn: Button
    private lateinit var musicNameText: TextView
    private lateinit var startBtn: Button

    private var isDetecting = false
    private var musicUri: Uri? = null
    private var soundDetector: SoundDetector? = null
    private var musicService: MusicService? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (audioGranted) {
            startDetection()
        } else {
            Toast.makeText(this, R.string.toast_permission_required, Toast.LENGTH_LONG).show()
        }
    }

    private val musicPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            musicUri = uri
            musicNameText.text = getString(R.string.music_selected, uri.lastPathSegment)
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                // 如果无法获取持久权限，使用临时权限也可以正常工作
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 应用保存的语言设置
        LanguageHelper.applySavedLanguage(this)
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        currentVolumeText = findViewById(R.id.currentVolumeText)
        volumeProgressBar = findViewById(R.id.volumeProgressBar)
        statusText = findViewById(R.id.statusText)
        thresholdEdit = findViewById(R.id.thresholdEdit)
        musicVolumeEdit = findViewById(R.id.musicVolumeEdit)
        selectMusicBtn = findViewById(R.id.selectMusicBtn)
        musicNameText = findViewById(R.id.musicNameText)
        startBtn = findViewById(R.id.startBtn)
        
        // 设置初始文本，避免显示占位符
        currentVolumeText.text = getString(R.string.current_volume, 0)
        statusText.text = getString(R.string.status_not_started)
    }

    private fun setupListeners() {
        selectMusicBtn.setOnClickListener {
            // 长按按钮重置为默认铃声
            selectMusicBtn.setOnLongClickListener {
                musicUri = null
                musicNameText.text = getString(R.string.music_reset_to_default)
                Toast.makeText(this, R.string.toast_default_ringtone_reset, Toast.LENGTH_SHORT).show()
                true
            }
            // 短按选择音乐
            musicPickerLauncher.launch("audio/*")
        }

        startBtn.setOnClickListener {
            if (isDetecting) {
                stopDetection()
            } else {
                checkPermissionsAndStart()
            }
        }
    }

    private fun checkPermissionsAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startDetection()
            }
            else -> {
                permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
            }
        }
    }

    private fun startDetection() {
        Log.i(TAG, "🚀 Starting detection...")
        
        val threshold = try {
            thresholdEdit.text.toString().toDoubleOrNull() ?: 80.0
        } catch (e: Exception) {
            80.0
        }
        Log.d(TAG, "Threshold set to: $threshold dB")

        val musicVolume = try {
            musicVolumeEdit.text.toString().toIntOrNull() ?: 100
        } catch (e: Exception) {
            100
        }.coerceIn(0, 100)
        Log.d(TAG, "Music volume set to: $musicVolume%")

        // 如果用户没有选择音乐，使用默认铃声；否则使用用户选择的音乐
        musicService = MusicService(this, musicUri, musicVolume)
        
        if (musicUri != null) {
            Log.i(TAG, "Using custom user-selected music")
        } else {
            Log.i(TAG, "Using built-in default alarm sound")
        }
        
        // 创建监听器并注册到全局
        val listener = object : SoundThresholdListener {
            override fun onThresholdExceeded() {
                Log.w(TAG, "⚠️ THRESHOLD EXCEEDED - Triggering alarm!")
                runOnUiThread {
                    statusText.text = getString(R.string.status_too_loud)
                }
                musicService?.playMusic()
            }

            override fun onVolumeChanged(dbLevel: Double) {
                runOnUiThread {
                    currentVolumeText.text = getString(R.string.current_volume, dbLevel.toInt())
                    volumeProgressBar.progress = (dbLevel / 120 * 100).toInt().coerceIn(0, 100)
                }
            }

            override fun onPlaybackStarted() {
                Log.i(TAG, "▶️ Playback started")
                // SoundDetector 已经在触发时自动暂停了
                runOnUiThread {
                    statusText.text = getString(R.string.status_playing_music)
                    startBtn.text = getString(R.string.button_stop)
                }
            }

            override fun onPlaybackFinished() {
                Log.i(TAG, "⏹️ Playback finished - Resuming detection")
                soundDetector?.resumeDetection()
                runOnUiThread {
                        statusText.text = getString(R.string.status_detecting)
                        startBtn.text = getString(R.string.button_stop_detection)
                }
            }

            override fun onVolumeBackToNormal() {
                Log.i(TAG, "🔄 Volume back to normal - Resetting UI state")
                runOnUiThread {
                    // 如果当前没有在播放音乐，重置状态为检测中
                    val playing = musicService?.isPlaying() ?: false
                    if (!playing) {
                    statusText.text = getString(R.string.status_detecting)
                    startBtn.text = getString(R.string.button_stop_detection)
                    } else {
                        Log.d(TAG, "Music is still playing, not resetting UI")
                    }
                }
            }
        }
        
        // 注册监听器到全局，让 MusicService 可以访问
        VolumeDetectionApp.listener = listener
        
        soundDetector = SoundDetector(
            this,
            threshold,
            listener
        )

        isDetecting = true
        startBtn.text = getString(R.string.button_stop_detection)
        statusText.text = getString(R.string.status_detecting)
        Log.i(TAG, "✅ Detection started successfully")
        
        // 启动前台服务以保持后台运行
        val serviceIntent = Intent(this, ForegroundDetectionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        soundDetector?.start()
    }

    private fun stopDetection() {
        Log.i(TAG, "🛑 Stopping detection...")
        soundDetector?.stop()
        soundDetector = null
        musicService?.stop()
        musicService = null

        isDetecting = false
        startBtn.text = getString(R.string.button_start_detection)
        statusText.text = getString(R.string.status_stopped)
        currentVolumeText.text = getString(R.string.current_volume, 0)
        volumeProgressBar.progress = 0
        
        // 停止前台服务
        val serviceIntent = Intent(this, ForegroundDetectionService::class.java)
        stopService(serviceIntent)
        
        Log.i(TAG, "✅ Detection stopped")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_language -> {
                showLanguageSelectionDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showLanguageSelectionDialog() {
        val languages = LanguageHelper.getAvailableLanguages()
        val currentLanguage = LanguageHelper.getSavedLanguage(this)
        
        val languageNames = languages.map { getString(it.nameResId) }.toTypedArray()
        val currentIndex = languages.indexOfFirst { it.code == currentLanguage }
        
        AlertDialog.Builder(this)
            .setTitle(R.string.language_selection_title)
            .setSingleChoiceItems(languageNames, currentIndex) { dialog, which ->
                val selectedLanguage = languages[which]
                LanguageHelper.setLocale(this, selectedLanguage.code)
                dialog.dismiss()
                recreate() // 重启Activity以应用新语言
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopDetection()
    }
}
