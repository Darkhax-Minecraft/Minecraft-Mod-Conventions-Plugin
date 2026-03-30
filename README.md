# Minecraft Mod Conventions

This is a highly opinionated conventions plugin for developing and publishing 
Minecraft mods that are compatible with multiple mod loaders.

## Feature Overview

- Configure your entire build using only the `./build-config.json`
  - name, id, license, client/server support.
  - CurseForge & Modrinth publishing details.
  - Dependencies for Maven, Curseforge, and Modrinth.
- Version number derived from target Minecraft version and CI build number.
- Multiloader project structure, supporting NeoForge and Fabric.
- Java version matches Minecraft version automatically.
- Optionally, fetch Patreon supporters and include them in mod files.
- Publish artifacts to CurseForge and Modrinth.
- Generate changelog from git commits.
- Validate project structure.
- Consistent Game Targets.
- Minify JSON resources.

## Configuration

This plugin is configured using the `./build-config.json` file in the root of
your project folder. This file is meant to be a portable replacement to 
`gradle.properties` or other config systems. 

### Configure Mod

The `mod` section of the config file defines your project itself.

- `name` - A display name for your mod. Spaces are allowed, but best avoided.
- `group` - The group your mod is published under.
- `authors` - An array of authors for the mod.
- `id` - The unique ID of your mod.
- `license` - The license your mod is published under.
- `description` - A brief description of your mod.
- `repo` - A link to the repo for your mod.
- `client` - A boolean that determines if your mod is client side only.

### Configure Dependencies

The `dependencies` section controls maven dependencies, and dependencies when
your project is published to platforms like CurseForge and Modrinth. For this
reason, each dependency must define as much information as possible.

- `name` - A display name for the dependency. This can be arbitrary.
- `type` - required, optional, or misc. A misc dependency is only used at dev time.
- `maven` - A map of maven coordinates for each supported platform.
- `curseforge` - The CurseForge project metadata for the dependency.
- `modrinth` - The Modrinth project metadata for the dependency.

### CurseForge & Modrinth Publishing
The `curseforge` and `modrinth` properties define the publishing metadata for
your project on each platform. They require the same properties.

- `slug` - The slug for your project on that platform.
- `id` - The project ID for your project on that platform.

### Example Config

The following is an example `./build-config.json` file.

```json
{
  "mod": {
    "name": "Example",
    "group": "net.darkhax.example",
    "authors": [
      "Darkhax"
    ],
    "id": "example",
    "license": "ARR",
    "description": "An example mod.",
    "repo": "https://github.com/Darkhax-Minecraft/ExampleMod",
    "client": false
  },
  "dependencies": [
    {
      "name": "Nature's Compass",
      "type": "required",
      "maven": {
        "neoforge": "curse.maven:natures-compass-252848:7808597",
        "fabric": "curse.maven:natures-compass-252848:7808592"
      },
      "curseforge": {
        "slug": "natures-compass",
        "id": "252848"
      },
      "modrinth": {
        "slug": "natures-compass",
        "id": "fPetb5Kh"
      }
    },
    {
      "name": "Collective",
      "type": "optional",
      "maven": {
        "neoforge": "curse.maven:collective-342584:7813912",
        "fabric": "curse.maven:collective-342584:7813912"
      },
      "curseforge": {
        "slug": "collective",
        "id": "342584"
      },
      "modrinth": {
        "slug": "collective",
        "id": "e0M1UDsY"
      }
    },
    {
      "name": "Resourceful Config",
      "type": "optional",
      "maven": {
        "fabric": "curse.maven:resourcefulconfig-714059:7808343"
      },
      "curseforge": {
        "slug": "resourceful-config",
        "id": "714059"
      },
      "modrinth": {
        "slug": "resourceful-config",
        "id": "M1953qlQ"
      }
    },
    {
      "name": "Journey Map",
      "type": "optional",
      "maven": {
        "neoforge": "curse.maven:jmap-32274:7812823"
      },
      "curseforge": {
        "slug": "journeymap",
        "id": "32274"
      },
      "modrinth": {
        "slug": "journeymap",
        "id": "lfHFW1mp"
      }
    }
  ],
  "curseforge": {
    "slug": "examplemod",
    "id": "469122"
  },
  "modrinth": {
    "slug": "dh-examplemod",
    "id": "OeyGScIr"
  }
}
```

## Secret Properties
This plugin requires several publishing secrets to be available. If 
`secretFileV2` or `secretFile` are defined as a path to a JSON file, the 
contents of that JSON file will be loaded as build properties. Only entries
that don't start with `_` and don't end with `_comment` are loaded.

The following properties are used by this plugin.

- mavenURL - The Maven to publish artifacts to.
- versionTrackerAPI - The version tracker API you use. Must accept BlameJared data format.
- versionTrackerUsername - Username for the version tracker API.
- versionTrackerKey - Auth key for the version tracker API.
- curse_auth - CurseForge publishing key.
- modrinth_auth - Modrinth publishing key.
- patreon_campaign_id - Campaign on Patreon.
- patreon_auth_token - Auth token for Patreon API. Must be v2.

## Project Validation
All projects must meet the following criteria.

- Contain `common`, `neoforge`, and `fabric` subprojects.
- `LICENSE` file in root project.
- Common must contain `src/main/resources/logo_modid.png`
- Common, NeoForge, and Fabric must contain `modid.platform.mixins.json` file.

## JSON Minify
To reduce wasted file space, all JSON resources are minified when copied over
to compiled jars. This means all superfluous whitespace and newlines are 
removed. This can help save a lot of space if your project is data heavy. The 
source files are untouched.

## Build Properties
Projects can use the following property tokens in their project resources.

- version - The project version.
- group - The project group.
- platform - The platform the project is targeting.
- minecraft_version - The target minecraft version.
- mod_name - The name of the mod.
- mod_author - A single string of all authors, separated by a `,`.
- mod_id - The unique id for the mod.
- mod_repo - The repo defined for the mod.
- mod_license - The license for the mod.
- mod_description - A short description of the mod.
- neoforge_version - The target NeoForge version.
- fabric_version - The target Fabric API version.
- fabric_loader_version - The target Fabric Loader version.
- java_version - The target Java version.
- curse_project - The project ID for the project on CurseForge.
- curse_page - The project page on CurseForge.
- modrinth_project - The project ID for the project on Modrinth.
- modrinth_page - The project page on Modrinth.
- mod_target_environment - Sided information for Fabric projects.

## Project Setup

This plugin aims to make plugin setup very simple, inlining most of the setup
logic into the plugin itself.

### Root Project

**settings.gradle.kts**
```kotlin
rootProject.name = "YourProjectName"
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
    }
}
include("common", "neoforge", "fabric")
```

**build.gradle.kts**
```kotlin
plugins {
    id("net.darkhax.mmc") version "26.1.0.7"
}
```

**build-config.json**
```json
{
  "mod": {
    "name": "YourProjectName",
    "group": "com.example.you",
    "authors": [
      "It's you!"
    ],
    "id": "your-id",
    "license": "A license!",
    "description": "Describe your mod!",
    "repo": "https://github.com/you/reponame",
    "client": false
  },
  "dependencies": [],
  "curseforge": {
    "slug": "your-id",
    "id": "0000"
  },
  "modrinth": {
    "slug": "your-id",
    "id": "0000"
  }
}
```

### Common

**build.gradle**
```groovy
```

**Required Files**
- src/main/resources/logo_modid.png
- src/main/resources/modid.common.mixins.json
- src/main/resources/pack.mcmeta

### Fabric

**build.gradle**
```groovy
```

**Required Files**
- src/main/resources/modid.fabric.mixins.json
- src/main/resources/fabric.mod.json

### NeoForge

**build.gradle**
```groovy
```

**Required Files**
- src/main/resources/modid.neoforge.mixins.json
- src/main/resources/META-INF/neoforge.mods.toml