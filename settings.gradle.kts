pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // LibretroDroid is only published through JitPack.
        maven("https://jitpack.io") { content { includeGroup("com.github.Swordfish90") } }
    }
}
rootProject.name = "totv-retro"
include(":app")
