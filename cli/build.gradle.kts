repositories {
    mavenCentral()
}

val targetJavaVersion = 17

dependencies {
    compileOnly(project(":core"))
    compileOnly("org.jetbrains:annotations:24.0.0")
}

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}
