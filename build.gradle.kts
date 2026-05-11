import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("com.diffplug.spotless") version "7.0.4" apply false
    id("net.ltgt.errorprone") version "5.1.0" apply false
}

allprojects {
    repositories {
        mavenCentral()
        maven { url = uri("https://maven.quiltmc.org/repository/release") }
        maven { url = uri("https://maven.fabricmc.net/") }
        maven { url = uri("https://maven.parchmentmc.org") }
    }

    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "net.ltgt.errorprone")

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            target("src/**/*.java")
            googleJavaFormat("1.27.0")
            formatAnnotations()
        }
    }

    // Use plugins block-style configuration if available, otherwise use afterEvaluate or direct access
    plugins.withType<JavaPlugin> {
        dependencies {
            "compileOnly"("com.google.code.findbugs:jsr305:3.0.2")
            "errorprone"("com.google.errorprone:error_prone_core:2.31.0")
            "errorprone"("com.uber.nullaway:nullaway:0.11.0")
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.errorprone {
            disableWarningsInGeneratedCode.set(true)
            option("NullAway:AnnotatedPackages", "com.squinchmods.redstonebackport")
            error("NullAway")
        }
    }
}
