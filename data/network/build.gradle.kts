plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.par9uet.jm.data.network"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
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
    implementation(project(":core:common"))
    // Retrofit CookieJar 与内部图片配置请求需要持久化 Cookie
    implementation(project(":data:storage"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    api(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    api(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.gson)
    implementation(libs.koin.androidx.compose)
    testImplementation(libs.junit)
}
