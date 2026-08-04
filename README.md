# 灵喵台球辅助 (BilliardAssist)

基于 OpenCV 模板匹配的 Android 台球瞄准辅助工具，通过 MediaProjection 截屏 + 图像识别自动绘制瞄准辅助线。

## ✨ 功能特性

- 🎯 **自动瞄准环检测** — OpenCV 模板匹配，实时定位瞄准环
- 📐 **双模式辅助线** — 角度补偿模式 + 镜像反射模式
- 🎨 **可配置外观** — 10 色可选、线宽可调、蚂蚁线开关
- 🔧 **8 种识别方案** — 适配不同游戏画面风格
- 📱 **悬浮窗覆盖** — 不侵入游戏，随时启停
- 🔋 **电池优化引导** — 一键跳转系统设置

## 📁 项目结构

```
BilliardAssist/
├── .github/workflows/build.yml     # GitHub Actions CI
├── README.md
├── build.gradle                    # 根级 Gradle
├── settings.gradle
├── gradle/wrapper/
│   └── gradle-wrapper.properties
└── app/
    ├── build.gradle
    ├── proguard-rules.pro
    ├── libs/                        # ⚠️ 放 opencv-android-4.x.x.jar
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/billiardassist/
        │   ├── App.java
        │   ├── MainActivity.java
        │   ├── OverlayService.java
        │   ├── AimDetector.java
        │   ├── ImageUtils.java
        │   ├── ai/AimProcessor.java
        │   ├── service/
        │   │   ├── CapturePermissionActivity.java
        │   │   ├── CaptureService.java
        │   │   └── FloatingService.java
        │   └── ui/
        │       ├── GuideActivity.java
        │       ├── CalibrateActivity.java
        │       ├── SettingsActivity.java
        │       └── AimSchemeActivity.java
        └── res/
            ├── layout/             # 5 个 XML 布局
            ├── drawable/           # ✅ 全部图片素材已生成
            ├── values/strings.xml
            ├── mipmap-hdpi/       # ✅ 应用图标
            ├── mipmap-mdpi/
            ├── mipmap-xhdpi/
            ├── mipmap-xxhdpi/
            └── mipmap-xxxhdpi/
```

## 🔧 手动补齐步骤（仅需 2 步）

### 第 1 步：下载 OpenCV Android SDK

1. 访问 https://opencv.org/releases/ 下载 **OpenCV 4.x.x for Android**
2. 解压后找到以下文件：

| 源路径 (OpenCV SDK 内) | 复制到项目路径 |
|---|---|
| `sdk/java/opencv-4.x.x.jar` | `app/libs/opencv-4.x.x.jar` |
| `sdk/native/libs/armeabi-v7a/libopencv_java4.so` | `app/src/main/jniLibs/armeabi-v7a/libopencv_java4.so` |
| `sdk/native/libs/arm64-v8a/libopencv_java4.so` | `app/src/main/jniLibs/arm64-v8a/libopencv_java4.so` |

### 第 2 步：准备瞄准环模板图片

1. 打开你的台球游戏，进入有瞄准环的画面
2. 截图 → 用任意图片编辑器裁剪出**仅包含瞄准环**的小图（建议 80×80 像素）
3. 保存为 `aim_template.png` → 覆盖 `app/src/main/res/drawable/aim_template.png`

> 💡 提示：模板越精确，识别率越高。建议 100% 缩放下裁剪。

## 🚀 使用方式

### GitHub Actions 自动构建

1. 补齐上述 4 个文件后，推送到 GitHub
2. 进入 **Actions** → **Build APK** → **Run workflow**
3. 等待 5-10 分钟，下载 `BilliardAssist-Debug` 产物

### 本地构建

```bash
git clone <your-repo-url>
cd BilliardAssist
./gradlew assembleDebug
# APK 输出: app/build/outputs/apk/debug/app-debug.apk
```

## 📱 运行流程

1. 安装 APK → 打开应用
2. 点击 **"竖版进"** 或 **"横版进"**
3. 授予**悬浮窗权限** → 授予**录屏权限**
4. 屏幕上出现绿色辅助线 → 开始游戏

## ⚙️ 设置说明

| 设置项 | 说明 |
|---|---|
| 桌布选择 | 3 种桌布纹理可选 |
| 反射方案 | 角度补偿 / 镜像反射 |
| 补偿比例 | 0.18（可调节） |
| 辅助线颜色 | 10 种颜色可选 |
| 辅助线粗细 | 1.0 ~ 10.0 |
| 蚂蚁线 | 开启后显示虚线边框 |
| 吸附最近球 | 自动吸附到最近的球 |
| 识别方案 | 8 种预设参数组合 |
| V/S/P 参数 | 亮度/圆白度/灵敏度 |

## 📄 License

MIT License
