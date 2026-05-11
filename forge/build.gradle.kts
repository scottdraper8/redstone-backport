import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("net.neoforged.moddev.legacyforge") version "2.0.141"
}

group = project.property("mod_group") as String
version = "${project.property("mod_version")}+${project.property("minecraft_version")}"

base {
    archivesName.set("${project.property("mod_id")}-forge")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

sourceSets {
    main {
        java {
            srcDir(project(":common").file("src/main/java"))
        }
        resources {
            srcDir(project(":common").file("src/main/resources"))
        }
    }
}

legacyForge {
    enable {
        forgeVersion = "${project.property("minecraft_version")}-${project.property("forge_version")}"
    }
    parchment {
        mappingsVersion = project.property("parchment_version") as String
        minecraftVersion = project.property("minecraft_version") as String
    }
    runs {
        register("client") {
            client()
            ideName = "Forge Client"
        }
        register("server") {
            server()
            ideName = "Forge Server"
        }
    }
    mods {
        register("redstone_backport") {
            sourceSet(sourceSets.main.get())
            sourceSet(project(":common").sourceSets.main.get())
        }
    }
}

val expandProps = mapOf(
    "version" to project.property("mod_version"),
)

tasks {
    processResources {
        inputs.properties(expandProps)
        filesMatching(listOf("META-INF/mods.toml")) {
            expand(expandProps)
        }
    }
}

repositories {
    mavenCentral()
    exclusiveContent {
        forRepository {
            maven("https://maven.minecraftforge.net") { name = "Forge" }
        }
        filter { includeGroupAndSubgroups("net.minecraftforge") }
    }
}

configurations {
    create("guavaForJavacBootclasspath") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }
}

dependencies {
    implementation(project(":common"))
    compileOnly("org.jetbrains:annotations:24.1.0")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    errorprone("com.google.errorprone:error_prone_core:2.39.0")
    errorprone("com.uber.nullaway:nullaway:0.12.7")

    add("guavaForJavacBootclasspath", "com.google.guava:guava:33.3.1-jre") {
        isTransitive = false
    }
    add("guavaForJavacBootclasspath", "com.google.guava:failureaccess:1.0.2") {
        isTransitive = false
    }
}

listOf(
    "compileClasspath",
    "annotationProcessor",
    "runtimeClasspath",
    "testCompileClasspath",
    "testAnnotationProcessor",
    "testRuntimeClasspath",
).forEach { name ->
    configurations.findByName(name)?.resolutionStrategy?.force("com.google.guava:guava:33.3.1-jre")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.isFork = true
    val bootclasspath =
        configurations.named("guavaForJavacBootclasspath").get().files
            .sortedBy { it.name }
            .joinToString(":") { it.absolutePath.replace('\\', '/') }
    options.forkOptions.jvmArgs =
        (options.forkOptions.jvmArgs ?: emptyList()) + "-Xbootclasspath/a:$bootclasspath"
    options.compilerArgs.addAll(listOf("-Xlint:deprecation"))
    options.errorprone {
        disableWarningsInGeneratedCode.set(true)
        option("NullAway:AnnotatedPackages", "com.squinchmods.redstonebackport")
        error("NullAway")
    }
}
