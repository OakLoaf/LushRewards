plugins {
    java
    `maven-publish`
    id("com.github.johnrengelman.shadow") version("8.1.1")
}

group = "org.lushplugins"
version = "3.0.0"

repositories {
    mavenLocal()
    mavenCentral()
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
    compileOnly("org.spigotmc:spigot-api:${findProperty("minecraftVersion")}-R0.1-SNAPSHOT")
    compileOnly("org.geysermc.floodgate:api:${findProperty("floodgateVersion")}-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:${findProperty("placeholderapiVersion")}")

    implementation("org.enchantedskies:EnchantedStorage:${findProperty("enchantedStorageVersion")}")
    implementation("org.lushplugins:LushLib:${findProperty("lushLibVersion")}")
    implementation("space.arim.morepaperlib:morepaperlib:${findProperty("morePaperLibVersion")}")
    implementation("com.mysql:mysql-connector-j:${findProperty("mysqlConnectorVersion")}")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    shadowJar {
        relocate("org.enchantedskies", "org.lushplugins.lushrewards.libraries.enchantedskies")
        relocate("org.lushplugins.lushlib", "org.lushplugins.lushrewards.libraries.lushlib")
        relocate("space.arim.morepaperlib", "org.lushplugins.lushrewards.libraries.morepaperlib")
        relocate("com.mysql", "org.lushplugins.lushrewards.libraries.mysql")

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