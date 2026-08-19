// 说明：阿里云镜像回源故障时会返回 502 而非 404，Gradle 把 502 当作致命错误
// 中断整条仓库链，导致后面的官方源根本不会被尝试（表现为「plugin not found」，
// 但构件其实存在）。因此对已知有问题的 group 显式排除镜像，让它直接走官方源。
pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/public") {
            content { excludeGroup("com.google.devtools.ksp") }
        }
        maven("https://maven.aliyun.com/repository/google") {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven("https://maven.aliyun.com/repository/gradle-plugin") {
            content { excludeGroup("com.google.devtools.ksp") }
        }
        maven("https://maven.aliyun.com/repository/central") {
            content { excludeGroup("com.google.devtools.ksp") }
        }
        // 官方源兜底：镜像缺件或回源异常时从这里取
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://maven.aliyun.com/repository/public") {
            content { excludeGroup("com.google.devtools.ksp") }
        }
        maven("https://maven.aliyun.com/repository/google") {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven("https://maven.aliyun.com/repository/gradle-plugin") {
            content { excludeGroup("com.google.devtools.ksp") }
        }
        maven("https://maven.aliyun.com/repository/central") {
            content { excludeGroup("com.google.devtools.ksp") }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "jmnext"

// 分层模块：依赖方向自上而下单向流动，由 Gradle 在编译期强制。
//   app → designsystem / domain / data:repository → data:{network,database,storage} → core:{model,common}
include(":app")
include(":core:common")
include(":core:model")
include(":core:designsystem")
include(":data:network")
include(":data:database")
include(":data:storage")
include(":data:repository")
include(":domain")
