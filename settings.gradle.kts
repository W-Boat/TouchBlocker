pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 添加 JitPack 仓库用于 Xposed API
        maven { url = uri("https://jitpack.io") }
        // 添加 Xposed 官方仓库
        maven { url = uri("https://api.xposed.info/") }
    }
}

rootProject.name = "My Application"
include(":app")
