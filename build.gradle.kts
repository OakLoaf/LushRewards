plugins {
    java
    `maven-publish`
    id("com.github.johnrengelman.shadow") version("8.1.1")
}

group = "me.dave"
version = "3.0.0.2-BETA"

repositories {
    mavenCentral()
    mavenLocal()
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") } // Spigot
    maven { url = uri("https://repo.opencollab.dev/main/") } // Floodgate
    maven { url = uri("https://repo.lushplugins.org/releases/") } // LushLib
    maven { url = uri("https://repo.lushplugins.org/snapshots/") } // LushLib
    maven { url = uri("https://repo.xemor.zip/releases/") } // EnchantedStorage
    maven { url = uri("https://mvn-repo.arim.space/lesser-gpl3/") } // MorePaperLib
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")} // PlaceholderAPI
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20-R0.1-SNAPSHOT")
    compileOnly("org.geysermc.floodgate:api:2.0-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.2")

    implementation("org.enchantedskies:EnchantedStorage:3.0.0")
    implementation("org.lushplugins:LushLib:0.1.7.6")
    implementation("space.arim.morepaperlib:morepaperlib:0.4.3")
    implementation("com.mysql:mysql-connector-j:8.3.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    shadowJar {
        relocate("org.enchantedskies", "me.dave.lushrewards.libraries.enchantedskies")
        relocate("org.lushplugins.lushlib", "me.dave.lushrewards.libraries.lushlib")
        relocate("space.arim.morepaperlib", "me.dave.lushrewards.libraries.morepaperlib")
        relocate("com.mysql", "me.dave.lushrewards.libraries.mysql")

        minimize()

        val folder = System.getenv("pluginFolder_1-20")
        if (folder != null) destinationDirectory.set(file(folder))
        archiveFileName.set("${project.name}-${project.version}.jar")
    }

    processResources{
        expand(project.properties)

        inputs.property("version", rootProject.version)
        filesMatching("plugin.yml") {
            expand("version" to rootProject.version)
        }
    }
}