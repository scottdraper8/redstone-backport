plugins {
    id("org.quiltmc.loom") version "1.15.1"
    `java-library`
}

group = project.property("mod_group") as String
version = "${project.property("mod_version")}+${project.property("minecraft_version")}"

base {
    archivesName.set("${project.property("mod_id")}-quilt")
}

loom {
    mods {
        create(project.property("mod_id") as String) {
            sourceSet("main")
        }
    }
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

    modImplementation("org.quiltmc:quilt-loader:${project.property("quilt_loader_version")}")
    modImplementation("org.quiltmc.quilted-fabric-api:quilted-fabric-api:${project.property("quilted_fabric_api_version")}")
}

val expandProps = mapOf(
    "version" to project.property("mod_version"),
    "group" to project.property("mod_group"),
    "minecraft_version" to project.property("minecraft_version"),
    "quilt_loader_version" to project.property("quilt_loader_version"),
    "quilted_fabric_api_version" to project.property("quilted_fabric_api_version"),
)

tasks {
    processResources {
        inputs.properties(expandProps)
        filesMatching(listOf("quilt.mod.json")) {
            expand(expandProps)
        }
    }
}
