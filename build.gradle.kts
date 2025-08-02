plugins {
    java
    `maven-publish`
    id("com.gradleup.shadow") version("8.3.0")
    id("xyz.jpenilla.run-paper") version("2.2.4")
}

group = "org.lushplugins"
version = "4.0.0-alpha1"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // Spigot
    maven("https://repo.opencollab.dev/main/") // Floodgate
    maven("https://repo.lushplugins.org/snapshots/") // LushLib, RewardsAPI
    maven("https://repo.helpch.at/releases/") // PlaceholderAPI
}

dependencies {
    // Dependencies
    compileOnly("org.spigotmc:spigot-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("com.mysql:mysql-connector-j:8.3.0")
    compileOnly("org.xerial:sqlite-jdbc:3.46.0.0")

    // Soft Dependencies
    compileOnly("org.geysermc.floodgate:api:2.0-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.5")

    // Libraries
    implementation("org.lushplugins:RewardsAPI:2.0.0-alpha1")
    implementation("org.lushplugins:LushLib:0.10.79")
    implementation("io.github.revxrsal:lamp.common:4.0.0-rc.12")
    implementation("io.github.revxrsal:lamp.bukkit:4.0.0-rc.12")
    implementation("org.lushplugins:PlaceholderHandler:1.0.0-alpha6")
    implementation("org.lushplugins.pluginupdater:PluginUpdater-API:1.0.3")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.bstats:bstats-bukkit:3.0.2")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

    shadowJar {
        relocate("org.bstats", "org.lushplugins.lushrewards.libraries.bstats")
        relocate("org.lushplugins.lushlib", "org.lushplugins.lushrewards.libraries.lushlib")
        relocate("space.arim.morepaperlib", "org.lushplugins.lushrewards.libraries.morepaperlib")
        relocate("com.mysql", "org.lushplugins.lushrewards.libraries.mysql")

        minimize {
            exclude(dependency("com.mysql:.*:.*"))
            exclude(dependency("org.postgresql:.*:.*"))
        }

        archiveFileName.set("${project.name}-${project.version}.jar")
    }

    processResources{
        expand(project.properties)

        inputs.property("version", rootProject.version)
        filesMatching("plugin.yml") {
            expand("version" to rootProject.version)
        }
    }

    runServer {
        minecraftVersion("1.21.1")
    }
}

tasks.withType(xyz.jpenilla.runtask.task.AbstractRun::class) {
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
