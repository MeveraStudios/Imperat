repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.codemc.io/repository/nms/")
    }
    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")

        content {
            includeGroup("org.bukkit")
            includeGroup("org.spigotmc")
        }
    }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/central") }
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        url = uri("https://libraries.minecraft.net")
    }
}

fun kyoriPlatform(module: String): String {
    return (rootProject.extra["kyoriPlatform"] as (String) -> String).invoke(module)
}

@Suppress("UNCHECKED_CAST")
val KyoriModule = rootProject.extra["KyoriModule"] as Map<String, String>

dependencies {
    api(project(":adventure"))
    api(project(":brigadier"))

    compileOnly(project(":core"))
    compileOnly(project(":paper"))

    compileOnly("com.mojang:brigadier:1.0.18")
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot:1.13.2-R0.1-SNAPSHOT")

    compileOnly(kyoriPlatform(KyoriModule["BUKKIT"]!!))
}

tasks.processTestResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val targetJavaVersion = 17
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
    }
}
