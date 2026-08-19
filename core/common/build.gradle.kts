plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.par9uet.jm.core.common"
    compileSdk = 36
    defaultConfig { minSdk = 23 }
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.gson)
    // TlsCompat 需要直接操作 OkHttpClient 的连接规格
    api(libs.okhttp)
    implementation(libs.koin.androidx.compose)
    testImplementation(libs.junit)
}
