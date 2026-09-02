import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.Copy
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.android.application)
}

val versionProps = Properties().apply {
    val file = rootProject.file("version.properties")
    if (file.exists()) {
        load(file.inputStream())
    }
}

val versionCodeProp = versionProps.getProperty("VERSION_CODE", "1").toIntOrNull()
val versionNameProp: String = versionProps.getProperty("VERSION_NAME", "1.1.1")

androidComponents {
    onVariants { variant ->
        val fileName = "jmnext_v${versionNameProp}_${variant.buildType}.apk"
        variant.outputs.forEach { output ->
            if (output is com.android.build.api.variant.impl.VariantOutputImpl) {
                output.outputFileName.set(fileName)
            }
        }
    }
}

// 将两种构建产物同步到桌面统一 APK 目录，目录位置相对项目根目录保持可移植。
val apkExportDir = rootProject.file("../../JMNext-APK")
val debugApkName = "jmnext_v${versionNameProp}_debug.apk"
val releaseApkName = "jmnext_v${versionNameProp}_release.apk"

val exportDebugApk = tasks.register<Copy>("exportDebugApk") {
    from(layout.buildDirectory.dir("outputs/apk/debug")) {
        include(debugApkName)
    }
    into(apkExportDir)
}
val exportReleaseApk = tasks.register<Copy>("exportReleaseApk") {
    from(layout.buildDirectory.dir("outputs/apk/release")) {
        include(releaseApkName)
    }
    into(apkExportDir)
}

tasks.configureEach {
    when (name) {
        "assembleDebug" -> finalizedBy(exportDebugApk)
        "assembleRelease" -> finalizedBy(exportReleaseApk)
    }
}

// 每次发布更新时统一构建两个可安装版本。
tasks.register("assembleBoth") {
    group = "build"
    description = "Build and export both Debug and Release APKs"
    dependsOn("assembleDebug", "assembleRelease")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

android {
    // namespace 是代码包名，保持不变即可：改它需要重构全部源码包路径，
    // 而区分应用身份只取决于下面的 applicationId
    namespace = "com.par9uet.jm"
    compileSdk = 36

    defaultConfig {
        // 二创自用版的独立包名，与原版互不覆盖、可共存。
        // 注意：改动此值会被系统视为全新应用，登录态与本地数据不与旧包共享。
        applicationId = "com.jmnext.reader"
        // Android 6.0 Marshmallow is API 23.
        minSdk = 23
        targetSdk = 36
        versionCode = versionCodeProp
        versionName = versionNameProp

        // testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        // 为了能在代码里按 BuildConfig.DEBUG 区分构建类型
        // （release 下关掉 logcat 输出与 HTTP 日志拦截器）
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

composeCompiler {}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // 分层模块：界面层依赖设计系统与服务层；仓库/存储仅因 ViewModel 与
    // 部分页面直接注入而暴露，新增代码应优先经由 :domain 访问
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":domain"))
    implementation(project(":data:repository"))
    implementation(project(":data:storage"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.material.kolor)
    implementation(libs.material.icons.extended)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.coil.compose)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.compose.navigation)
    implementation(libs.koin.androidx.workmanager)
    implementation(libs.paging3.compose)
    implementation(libs.kizitonwose.calendar)
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.ui.tooling)
}
