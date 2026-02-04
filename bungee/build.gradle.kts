repositories {
    mavenCentral()
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

    compileOnly(project(":core"))
    compileOnly(kyoriPlatform(KyoriModule["BUNGEE"]!!))
    compileOnly("net.md-5:bungeecord-api:1.21-R0.2")
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
