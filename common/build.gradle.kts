plugins {
    id("org.quiltmc.loom") version "1.15.1"
    `java-library`
}

group = project.property("mod_group") as String
version = "${project.property("mod_version")}+${project.property("minecraft_version")}"

base {
    archivesName.set("${project.property("mod_id")}-common")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${project.property("minecraft_version")}:${project.property("parchment_version")}@zip")
    })

    compileOnly("org.jetbrains:annotations:24.1.0")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    modCompileOnly("net.fabricmc:fabric-loader:${project.property("fabric_loader_version")}")
}
