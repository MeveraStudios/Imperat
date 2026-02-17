import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    kotlin("jvm") version "2.3.0"
    id("com.gradleup.shadow") version "8.3.9"
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.1.0")
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    compileOnly("org.jetbrains.kotlin:kotlin-reflect:2.3.10")

    annotationProcessor("org.jetbrains:annotations:24.1.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
    testImplementation("org.junit.platform:junit-platform-suite:1.10.1")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.8.0")

    testImplementation("org.jetbrains:annotations:24.1.0")
    testAnnotationProcessor("org.jetbrains:annotations:24.1.0")
}

tasks.javadoc {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()

    // Test execution configuration
    maxParallelForks = Runtime.getRuntime().availableProcessors()

    // System properties for tests
    systemProperty("imperat.test.mode", "true")
    systemProperty("imperat.debug.enabled", "false")

    // Test output configuration
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = true
        showCauses = true
        showExceptions = true
        showStackTraces = true
    }

    // Fail fast on first test failure
    failFast = false

    // Test filtering (exclude performance tests)
    filter {
        excludeTestsMatching("*PerformanceTest*")
        excludeTestsMatching("*SlowTest*")
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
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

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(targetJavaVersion.toString()))
    }
}

// Task for running only fast tests during development
tasks.register<Test>("fastTest") {
    useJUnitPlatform()

    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath

    include("**/basics/**")
    include("**/arguments/**")
    include("**/flags/**")
    include("**/parameters/**")
    include("**/errors/**")
    include("**/integration/**")
    include("**/events/**")
    include("**/enhanced/**")

    // Test execution configuration
    maxParallelForks = Runtime.getRuntime().availableProcessors()

    // System properties for tests
    systemProperty("imperat.test.mode", "true")
    systemProperty("imperat.debug.enabled", "false")

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.SHORT
    }

    // Ensure tests are detected and executed properly
    isScanForTestClasses = true

    // Prevent test result caching issues
    outputs.upToDateWhen { false }

    description = "Runs fast unit tests for development"
    group = "verification"
}

// Task for running event system tests
tasks.register<Test>("eventsTest") {
    useJUnitPlatform()

    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath

    include("**/events/**")

    // Test execution configuration
    maxParallelForks = 1

    // System properties for tests
    systemProperty("imperat.test.mode", "true")
    systemProperty("imperat.debug.enabled", "false")

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = true
        showCauses = true
        showExceptions = true
        showStackTraces = true
    }

    // Ensure tests are detected and executed
    isScanForTestClasses = true

    // Prevent test result caching issues
    outputs.upToDateWhen { false }

    description = "Runs event system tests"
    group = "verification"
}

// Task for running integration tests
tasks.register<Test>("integrationTest") {
    useJUnitPlatform()

    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath

    include("**/integration/**")

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = true
    }

    description = "Runs integration tests"
    group = "verification"
}

// Task for running all functional tests (no performance)
tasks.register<Test>("functionalTest") {
    useJUnitPlatform()

    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath

    exclude("**/performance/**")

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
    }

    description = "Runs all functional tests (excludes performance tests)"
    group = "verification"
}

kotlin {
    jvmToolchain(17)
}