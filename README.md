# Redstone Backport

Early-release multi-loader (Forge + Fabric + Quilt) workspace for the
`redstone_backport` mod on Minecraft 1.20.1.

This mod is intended to backport redstone additions from later Minecraft
versions to older ones over time. The project is still in an early release
state, so scope and implementation details may change quickly.

## Structure

- `common`: Shared gameplay logic, resources, and vanilla-first abstractions.
- `forge`: Forge-specific bootstrap, registrations, and capability adapters.
- `fabric`: Fabric-specific bootstrap, registrations, and entrypoints.
- `quilt`: Quilt-specific bootstrap, metadata, and entrypoints.

## Useful Commands

### Building

The preferred way to build the mod for all loaders is to use the monorepo
build helper:

```sh
# From the monorepo root
./mods/minecraft/build redstone-backport
```

Alternatively, you can run Gradle directly from this folder:

```sh
./gradlew assemble
```

### Forge Development

- `./gradlew :forge:runClient` - launch the Forge client
- `./gradlew :forge:runServer` - launch the Forge server

### Fabric Development

- `./gradlew :fabric:runClient` - launch the Fabric client
- `./gradlew :fabric:runServer` - launch the Fabric server

### Quilt Development

- `./gradlew :quilt:runClient` - launch the Quilt client
- `./gradlew :quilt:runServer` - launch the Quilt server

### General

- `./gradlew spotlessApply` - format all Java sources
