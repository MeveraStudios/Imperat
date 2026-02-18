plugins {
    id("com.gradleup.shadow") version "8.3.9"
    kotlin("jvm") version "2.3.0"
}

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

    // Test dependencies
    testImplementation(project(":core"))
    testImplementation(project(":adventure"))
    testImplementation(project(":brigadier"))
    testImplementation(project(":paper"))
    testImplementation("com.mojang:brigadier:1.0.18")
    testImplementation("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    testImplementation(kyoriPlatform(KyoriModule["BUKKIT"]!!))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.29.0")
}

tasks.processTestResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.test {
    useJUnitPlatform()
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

// MockBukkit v1.21 requires Java 21 at runtime and compile time
tasks.named<JavaCompile>("compileTestJava") {
    options.release.set(21)
    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
}

tasks.test {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
}

kotlin {
    jvmToolchain(17)
}
