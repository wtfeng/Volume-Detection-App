# Volume Detection App

An Android application that monitors ambient sound levels and plays an alarm when the volume exceeds a specified threshold.

## Features

- 🔊 **Real-time Sound Monitoring** - Continuously monitors environmental noise levels
- ⚠️ **Threshold Detection** - Triggers alert when sound exceeds configured dB level (default: 80 dB)
- 🎵 **Custom Alarm Playback** - Plays built-in alarm sound or user-selected music
- 🔄 **Auto Recovery** - Automatically resumes detection after alarm playback completes
- 💤 **Background Service** - Runs persistently in background with foreground service protection
- 🔒 **Wake Lock** - Prevents device from sleeping during monitoring

## How It Works

1. **Detection Mode**: The app continuously monitors ambient sound through the microphone
2. **Threshold Trigger**: When sound exceeds the threshold for 3 consecutive times, it triggers the alarm
3. **Alarm Playback**: Music/alarm plays at full volume while pausing detection
4. **Auto Resume**: After playback completes, detection automatically resumes
5. **State Reset**: UI resets to normal when sound level returns below threshold

## Technical Highlights

- **Foreground Service**: Uses Android Foreground Service to prevent system from killing the app
- **WakeLock Management**: Properly acquires and releases WakeLock to keep CPU running
- **Single Task Pattern**: Ensures only one alarm plays at a time (detect → play → resume)
- **Error Handling**: Gracefully handles MediaPlayer errors and recovers detection state

## Requirements

- Android 6.0 (API level 23) or higher
- Microphone permission
- Audio recording permission

## Permissions

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

## Build & Install

```bash
# Build debug APK
./gradlew assembleDebug

# Install via ADB
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Project Structure

```
app/src/main/java/com/example/volumedetection/
├── MainActivity.kt              # Main UI and control logic
├── SoundDetector.kt             # Audio recording and threshold detection
├── MusicService.kt              # Music playback service
├── ForegroundDetectionService.kt # Background service for persistent monitoring
└── VolumeDetectionApp.kt        # Application class
```

## Default Configuration

- **Sound Threshold**: 80 dB (configurable in UI)
- **Trigger Count**: 3 consecutive detections
- **Music Volume**: 100% (configurable in UI)

## License

This project is open source and available for educational purposes.
