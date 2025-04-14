plugins {
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // ✅ 테스트 라이브러리 (JUnit Jupiter)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // ✅ HTTP 클라이언트 (경량화: Feign 제거, OkHttp 직접 사용 가능)
    implementation("com.squareup.okhttp3:okhttp:4.12.0") // 최신 OkHttp 사용

    // ✅ JSON 직렬화/역직렬화
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")

    // ✅ 로컬 캐싱
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // ✅ 스케줄링
     implementation("com.cronutils:cron-utils:9.2.0")

    // ✅ 로깅 API만 제공 (구현체는 사용자가 선택)
    api("org.slf4j:slf4j-api:2.0.9")

    // ❌ 구현체는 포함하지 않음 (사용자 측에서 선택하도록)
    // runtimeOnly("org.slf4j:slf4j-simple:2.0.9")
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