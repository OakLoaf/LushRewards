plugins {
    java
    `maven-publish`
    id("com.gradleup.shadow") version("8.3.0")
}

group = "org.lushplugins"
version = "3.3.1"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // Spigot
    maven("https://repo.opencollab.dev/main/") // Floodgate
    maven("https://repo.lushplugins.org/releases/") // LushLib
    maven("https://repo.lushplugins.org/snapshots/") // LushLib
    maven("https://mvn-repo.arim.space/lesser-gpl3/") // MorePaperLib
    maven("https://repo.helpch.at/releases/") // PlaceholderAPI
}

dependencies {
    // Dependencies
    compileOnly("org.spigotmc:spigot-api:${findProperty("minecraftVersion")}-R0.1-SNAPSHOT")

    // Soft Dependencies
    compileOnly("org.geysermc.floodgate:api:${findProperty("floodgateVersion")}-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:${findProperty("placeholderapiVersion")}")

    // Libraries
    implementation("org.bstats:bstats-bukkit:${findProperty("bStatsVersion")}")
    implementation("org.lushplugins:LushLib:${findProperty("lushLibVersion")}")
    implementation("space.arim.morepaperlib:morepaperlib:${findProperty("morePaperLibVersion")}")
    implementation("com.zaxxer:HikariCP:${findProperty("hikariCPVersion")}")
    implementation("com.mysql:mysql-connector-j:${findProperty("mysqlConnectorVersion")}")
    implementation("org.xerial:sqlite-jdbc:${findProperty("sqliteConnectorVersion")}")
    implementation("org.postgresql:postgresql:${findProperty("postgresqlVersion")}")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    shadowJar {
        relocate("org.bstats", "org.lushplugins.lushrewards.libraries.bstats")
        relocate("org.enchantedskies", "org.lushplugins.lushrewards.libraries.enchantedskies")
        relocate("org.lushplugins.lushlib", "org.lushplugins.lushrewards.libraries.lushlib")
        relocate("space.arim.morepaperlib", "org.lushplugins.lushrewards.libraries.morepaperlib")
        relocate("com.mysql", "org.lushplugins.lushrewards.libraries.mysql")

        minimize {
            exclude(dependency("com.mysql:.*:.*"))
            exclude(dependency("org.postgresql:.*:.*"))
        }

        val folder = System.getenv("pluginFolder")
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
