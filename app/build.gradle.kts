// HazeRoot — 通用 root 脚本执行器
// 作者：HazeRoot
// 第一代基线：v2.7.2 · 当前版本见 versionName
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.hazeroot.scriptrunner"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hazeroot.scriptrunner"
        minSdk = 26
        targetSdk = 34
        versionCode = 71
        versionName = "2.8.5"
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            // 包名与 release 一致（applicationId 已是正式包名）。
            // 开发期若需与实机正式版并存，临时加回：
            //   applicationIdSuffix = ".dev"
            //   versionNameSuffix = "-dev"
            //   resValue("string", "app_name", "HazeRoot Dev")
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

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core:1.12.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    // 单元测试：覆盖纯 Java 核心逻辑（温控解析 / 刷新率命令 / 编码解码 / 应答匹配）
    testImplementation("junit:junit:4.13.2")
}

// 重建期：放开 javac 错误显示上限
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "2000", "-Xmaxwarns", "2000"))
}
