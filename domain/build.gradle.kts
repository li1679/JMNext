plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.par9uet.jm.domain"
    compileSdk = 36
    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
}

kotlin {
    compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17 }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    api(project(":core:model"))
    api(project(":core:common"))
    api(project(":data:repository"))
    api(project(":data:storage"))
    implementation(project(":data:network"))
    implementation(project(":data:database"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.gson)
    implementation(libs.okhttp)
    api(libs.androidx.work.runtime.ktx)
    api(libs.coil.compose)
    api(libs.paging3.runtime)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.workmanager)

    // 仅取 Compose 的状态与图形原语（mutableStateOf / ImageBitmap），
    // 不引入 material / navigation 等 UI 组件——服务层不该反向依赖界面。
    implementation(platform(libs.androidx.compose.bom))
    api(libs.androidx.ui.graphics)

    testImplementation(libs.junit)
}
