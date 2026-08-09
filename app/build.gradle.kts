plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    // 🔥 统一命名空间，彻底解决包名不匹配导致的资源找不到问题
    namespace = "com.lingmiao.v2"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.lingmiao.v2"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    
    // 顶级应用标准：统一启用 Compose 和 Java 17 支持
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    
    // Compose UI 系列（现代化的声明式 UI 基础）
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    
    // 协程（保证悬浮窗和服务流畅运行）
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
