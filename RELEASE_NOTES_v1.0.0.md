# 🎉 Volume Detection App v1.0.0

## 📱 应用简介
Volume Detection App 是一款 Android 应用，可以实时监控环境音量，当声音超过设定阈值时自动播放警报音乐，帮助您保护听力健康。

## ✨ 核心功能

### 🔊 实时声音监控
- 持续监测环境噪音水平
- 可自定义声音阈值（默认 80 分贝）
- 高精度音频检测算法

### ⚠️ 智能警报系统
- 连续 3 次检测到超标声音后触发警报
- 自动暂停检测防止重复触发
- 播放完成后自动恢复监控

### 🎵 音乐播放服务
- 使用内置警报音或自定义音乐
- 自动调节到最大音量播放
- 支持后台播放服务

### 💤 后台持久运行
- 前台服务保障不被系统杀死
- WakeLock 防止设备休眠
- 低功耗优化设计

## 🛠️ 技术特性

- **开发语言**: Kotlin
- **最低版本**: Android 8.0 (API 26)
- **目标版本**: Android 14 (API 34)
- **架构模式**: Service + Foreground Detection
- **权限管理**: RECORD_AUDIO, WAKE_LOCK, FOREGROUND_SERVICE

## 📦 项目结构

```
app/src/main/java/com/example/volumedetection/
├── MainActivity.kt              # 主界面和控制逻辑
├── SoundDetector.kt             # 音频录制和阈值检测
├── MusicService.kt              # 音乐播放服务
├── ForegroundDetectionService.kt # 后台检测服务
└── VolumeDetectionApp.kt        # 应用类
```

## 🚀 快速开始

### 构建项目
```bash
./gradlew assembleDebug
```

### 安装应用
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 使用方法
1. 启动应用并授予麦克风权限
2. 设置声音阈值（默认 80 dB）
3. 点击"开始监控"按钮
4. 应用将在后台持续监控环境音量

## 📝 更新日志

### v1.0.0 - 初始发布
- ✅ 实现基础声音检测功能
- ✅ 添加阈值检测和警报播放
- ✅ 实现前台后台服务
- ✅ 支持多语言（中文/英文）
- ✅ 修复 MediaPlayer 和 WakeLock 问题
- ✅ 优化检测流程防止重复触发

## 📄 许可证
本项目开源，仅供学习和研究使用。

## 🙏 致谢
感谢所有贡献者和使用者！

---

**下载**: [Volume-Detection-App-v1.0.0.apk](../../releases/download/v1.0.0/Volume-Detection-App-v1.0.0.apk)
