pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // THÊM DÒNG NÀY VÀO ĐỂ SỬA LỖI
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Music DNA"
include(":app")
