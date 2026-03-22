package com.example.volumedetection

import android.app.Application

class VolumeDetectionApp : Application() {
    companion object {
        var listener: SoundThresholdListener? = null
    }
}
