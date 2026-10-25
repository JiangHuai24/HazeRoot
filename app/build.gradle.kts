// HazeRoot — 通用 root 脚本执行器
// 作者：HazeRoot
// 第一代基线：v2.7.2 · 当前版本见 versionName
plugins {
    id("com.android.application")
}

android {
    namespace = "com.hazeroot.scriptrunner"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hazeroot.scriptrunner"
        minSdk = 26
        targetSdk = 34
        versionCode = 70
        versionName = "2.8.4"
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += setOf("META-INF/*.kotlin_module")
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core:1.12.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
}

// 重建期：放开 javac 错误显示上限
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "2000", "-Xmaxwarns", "2000"))
}
