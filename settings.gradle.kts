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
        // maven("https://jitpack.io") // در صورت نیاز
    }
}
rootProject.name = "PrayerTimes"
include(":app", ":PersianDate")