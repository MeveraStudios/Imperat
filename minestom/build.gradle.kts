repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "25"
    targetCompatibility = "25"
}

dependencies {
    compileOnly(project(":core"))
    api(project(":adventure"))
    implementation("net.minestom:minestom:2026.01.08-1.21.11")
}

val targetJavaVersion = 25
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
