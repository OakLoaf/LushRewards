plugins {
    java
    `kotlin-dsl`
    `maven-publish`
    id("com.github.johnrengelman.shadow") version("7.1.2")
}


group = "me.dave"
version = "1.5-BETA"

repositories {
    mavenCentral()
    mavenLocal()
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://repo.dmulloy2.net/repository/public/") }
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")}
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20-R0.1-SNAPSHOT")
    compileOnly("org.geysermc.floodgate:api:2.0-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.2")
    shadow(files("libs/EnchantedStorage.jar"))
    shadow("com.github.CoolDCB:ChatColorHandler:v1.3.3")
}

java {
    configurations.shadow.get().dependencies.remove(dependencies.gradleApi())
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks.shadowJar {
    minimize()
    configurations = listOf(project.configurations.shadow.get())
    val folder = System.getenv("pluginFolder_1-20")
    if (folder != null) destinationDirectory.set(file(folder))
    archiveFileName.set("${project.name}-${project.version}.jar")
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

// Handles version variables
tasks.processResources {
    expand(project.properties)

    inputs.property("version", rootProject.version)
    filesMatching("plugin.yml") {
        expand("version" to rootProject.version)
    }
}