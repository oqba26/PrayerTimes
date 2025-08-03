pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // اضافه شده برای لایبرری‌های گیتهاب مثل PersianDate (اگر در آینده نیاز شد)
    }
}

rootProject.name = "PrayerTimes"
include(":app")