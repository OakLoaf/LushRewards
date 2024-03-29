plugins {
    java
    `maven-publish`
    id("com.github.johnrengelman.shadow") version("8.1.1")
}

group = "me.dave"
version = "2.1.8.1"

repositories {
    mavenCentral()
    mavenLocal()
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://repo.opencollab.dev/main/") } // Floodgate
    maven { url = uri("https://mvn-repo.arim.space/lesser-gpl3/") } // MorePaperLib
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")} // PlaceholderAPI
    maven { url = uri("https://repo.dmulloy2.net/repository/public/") }
    maven { url = uri("https://jitpack.io") } // ChatColorHandler
}

dependencies {
    compileOnly("org.spigotmc:spigot:1.20-R0.1-SNAPSHOT")
    compileOnly("org.geysermc.floodgate:api:2.0-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.2")
    implementation("space.arim.morepaperlib:morepaperlib:0.4.3")
    implementation(files("libs/EnchantedStorage-3.0.0.jar"))
    implementation("com.github.CoolDCB:ChatColorHandler:v2.5.1")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    shadowJar {
        relocate("space.arim", "me.dave.activityrewarder.libraries.paperlib")
        relocate("org.enchantedskies", "me.dave.activityrewarder.libraries.enchantedskies")
        relocate("me.dave.chatcolorhandler", "me.dave.activityrewarder.libraries.chatcolor")

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