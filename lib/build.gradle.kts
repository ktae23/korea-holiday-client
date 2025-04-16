plugins {
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0") // 최신 OkHttp 사용

    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")

    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("com.cronutils:cron-utils:9.2.0")
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