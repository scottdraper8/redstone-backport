plugins {
    id("org.quiltmc.loom") version "1.15.1"
    `java-library`
}

group = project.property("mod_group") as String
version = "${project.property("mod_version")}+${project.property("minecraft_version")}"

base {
    archivesName.set("${project.property("mod_id")}-fabric")
}

sourceSets {
    main {
        resources {
            srcDir(project(":common").file("src/main/resources"))
        }
    }
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

    modImplementation("net.fabricmc:fabric-loader:${project.property("fabric_loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_api_version")}")

    implementation(project(path = ":common", configuration = "namedElements"))
}

val expandProps = mapOf(
    "version" to project.property("mod_version"),
)

tasks {
    processResources {
        inputs.properties(expandProps)
        filesMatching(listOf("fabric.mod.json")) {
            expand(expandProps)
        }
    }
}
