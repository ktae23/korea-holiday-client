plugins {
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // Use JUnit Jupiter for testing.
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Sample dependencies from template
    api(libs.commons.math3)
    implementation(libs.guava)

    // Feign client
    implementation("io.github.openfeign:feign-core:12.5")
    implementation("io.github.openfeign:feign-okhttp:12.5")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")

    // Caffeine for caching
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // Cron-utils for scheduling
    implementation("com.cronutils:cron-utils:9.2.0")

    // SLF4J logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.9")
}

java {
    // Apply a specific Java toolchain to ease working on different environments.
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}