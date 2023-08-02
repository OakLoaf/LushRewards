plugins {
    java
    `kotlin-dsl`
    `maven-publish`
    id("com.github.johnrengelman.shadow") version("7.1.2")
}

allprojects {
    plugins.apply("java")
    plugins.apply("maven-publish")
    plugins.apply("com.github.johnrengelman.shadow")

    group = "me.dave"
    version = "1.5-BETA"

    repositories {
        mavenCentral()
        mavenLocal()
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
        maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
        maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
        maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")}
        maven { url = uri("https://repo.dmulloy2.net/repository/public/") }
        maven { url = uri("https://jitpack.io") }
    }


    java {
        configurations.shadow.get().dependencies.remove(dependencies.gradleApi())
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    }

    tasks {
        withType<JavaCompile> {
            options.encoding = "UTF-8"
        }
        shadowJar {
            minimize()
            configurations = listOf(project.configurations.shadow.get())
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
}

dependencies {
    implementation(project(":bukkit"))
    implementation(project(":folia"))
}