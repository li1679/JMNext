plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.par9uet.jm.core.model"
    compileSdk = 36
    defaultConfig { minSdk = 23 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17 }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    // 模型上带有 Gson 注解，序列化契约与模型定义放在一起
    api(libs.gson)
}
