plugins {
    `java-library`
    id("com.vanniktech.maven.publish") version "0.33.0"
}

val baseVersion = "3.0.0"
val releaseSnapshots = true
val isSnapshot = System.getenv("SNAPSHOT_BUILD") == "true"

tasks.register("printReleaseSnapshots") {
    doLast {
        println("releaseSnapshots=$releaseSnapshots")
    }
}

tasks.register("printVersion") {
    doLast {
        println("baseVersion=$baseVersion")
    }
}

allprojects {
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    group = "studio.mevera"
    version = baseVersion

    if (isSnapshot && releaseSnapshots) {
        version = "$version-SNAPSHOT"
    }

    extra.apply {
        val kyoriVersion = "4.24.0"
        val kyoriPlatformVersion = "4.4.1"

        set("kyori", fun(module: String): String {
            return "net.kyori:adventure-$module:$kyoriVersion"
        })

        set("kyoriPlatform", fun(module: String): String {
            return "net.kyori:adventure-$module:$kyoriPlatformVersion"
        })

        set("KyoriModule", mapOf(
            "API" to "api",
            "MINI_MESSAGE" to "text-minimessage",
            "BUKKIT" to "platform-bukkit",
            "BUNGEE" to "platform-bungeecord",
            "SPONGE" to "platform-spongeapi"
        ))
    }
}

subprojects {
    apply(plugin = "java-library")

    if (project.name == "paper") {
        return@subprojects
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    apply(plugin = "com.vanniktech.maven.publish")

    configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
        coordinates(group as String, "imperat-${name}", version as String)

        pom {
            name.set("Imperat")
            description.set("A modern customizable command framework.")
            inceptionYear.set("2024")
            url.set("https://github.com/MeveraStudios/Imperat/")
            licenses {
                license {
                    name.set("MIT")
                    url.set("https://opensource.org/licenses/MIT")
                    distribution.set("https://mit-license.org/")
                }
            }
            developers {
                developer {
                    id.set("mqzn")
                    name.set("Mqzn")
                    url.set("https://github.com/Mqzn/")
                }
                developer {
                    id.set("iiahmedyt")
                    name.set("iiAhmedYT")
                    url.set("https://github.com/iiAhmedYT/")
                }
            }
            scm {
                url.set("https://github.com/MeveraStudios/Imperat/")
                connection.set("scm:git:git://github.com/MeveraStudios/Imperat.git")
                developerConnection.set("scm:git:ssh://git@github.com/MeveraStudios/Imperat.git")
            }
        }

        if (!gradle.startParameter.taskNames.any { it == "publishToMavenLocal" }
            && (!isSnapshot || (isSnapshot && releaseSnapshots))) {
            publishToMavenCentral()
            signAllPublications()
        }

        tasks.withType<JavaCompile> {
            options.encoding = "UTF-8"
            options.compilerArgs.add("-parameters")
        }
    }
}
