pluginManagement {
    repositories {
        maven { url = uri("https://maven.quiltmc.org/repository/release") }
        maven { url = uri("https://maven.fabricmc.net/") }
        maven { url = uri("https://maven.neoforged.net/releases") }
        maven { url = uri("https://repo.spongepowered.org/repository/maven-public/") }
        maven { url = uri("https://maven.architectury.dev/") }
        maven { url = uri("https://maven.parchmentmc.org") }
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "redstone_backport"
include("common")
include("forge")
include("fabric")
include("quilt")
