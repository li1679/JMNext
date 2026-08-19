plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.par9uet.jm.data.repository"
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
    api(project(":data:network"))
    api(project(":data:database"))
    implementation(project(":core:common"))
    implementation(project(":data:storage"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.gson)
    implementation(libs.okhttp)
    api(libs.paging3.runtime)
    implementation(libs.koin.androidx.compose)
    // 内置数据源：直接走 JMComic 客户端，与 HTTP 线路互为备选
    api(libs.jmcomic.api)
    implementation(libs.jmcomic.core) {
        exclude(group = "org.sejda.imageio", module = "webp-imageio")
    }
    implementation(libs.jmcomic.android.support)
}
