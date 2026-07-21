plugins {
    `java-library`
    `maven-publish`
    signing
}

group = "io.github.ktae23"
version = providers.gradleProperty("libraryVersion").getOrElse("1.1.0")

base {
    archivesName.set("korea-holiday-client")
}

repositories {
    mavenCentral()
}

dependencies {
    // 공개 API에 노출되지 않으므로 implementation 으로 둔다.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).apply {
        charSet = "UTF-8"
        docEncoding = "UTF-8"
        addStringOption("Xdoclint:none", "-quiet")
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

// ────────────────────────── 배포 ──────────────────────────

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "korea-holiday-client"
            from(components["java"])

            pom {
                name.set("korea-holiday-client")
                description.set(
                    "대한민국 공휴일·영업일 계산 자바 클라이언트. " +
                    "한국천문연구원 특일 정보 API를 실시간 조회하고 캐싱한다."
                )
                url.set("https://github.com/ktae23/korea-holiday-client")
                inceptionYear.set("2025")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("ktae23")
                        name.set("Kyungtae Park")
                        url.set("https://github.com/ktae23")
                    }
                }
                scm {
                    url.set("https://github.com/ktae23/korea-holiday-client")
                    connection.set("scm:git:https://github.com/ktae23/korea-holiday-client.git")
                    developerConnection.set("scm:git:ssh://git@github.com/ktae23/korea-holiday-client.git")
                }
                issueManagement {
                    system.set("GitHub Issues")
                    url.set("https://github.com/ktae23/korea-holiday-client/issues")
                }
            }
        }
    }

    repositories {
        // Central Portal 업로드용 번들을 만들 로컬 스테이징 저장소
        maven {
            name = "localStaging"
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

/**
 * 서명 키는 아래 순서로 찾는다. 없으면 서명을 건너뛴다(로컬 빌드용).
 *
 *  1. gradle.properties 의 signingInMemoryKey
 *  2. 환경변수 ORG_GRADLE_PROJECT_signingInMemoryKey
 *  3. 환경변수 SIGNING_KEY (CI 용)
 *
 * ASCII armored 개인키 전문을 넣는다:
 *   export ORG_GRADLE_PROJECT_signingInMemoryKey="$(gpg --armor --export-secret-keys KEYID)"
 */
fun secret(name: String, envFallback: String): String? =
    (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() }
        ?: System.getenv(envFallback)?.takeIf { it.isNotBlank() }

val signingKey = secret("signingInMemoryKey", "SIGNING_KEY")
val signingPassword = secret("signingInMemoryKeyPassword", "SIGNING_PASSWORD")

signing {
    isRequired = signingKey != null
    if (signingKey != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["maven"])
    } else {
        logger.lifecycle("⚠️  서명 키가 없어 GPG 서명을 건너뜁니다. Maven Central 업로드에는 서명이 필수입니다.")
    }
}

/**
 * Central Portal(central.sonatype.com)에 업로드할 번들 zip 생성.
 *
 *   ./gradlew :lib:centralBundle
 *   → lib/build/distributions/central-bundle.zip
 *
 * 이 파일을 Central Portal 의 "Publish Component" 에 업로드한다.
 */
tasks.register<Zip>("centralBundle") {
    group = "publishing"
    description = "Maven Central Portal 업로드용 번들 zip 생성"

    dependsOn("publishMavenPublicationToLocalStagingRepository")
    from(layout.buildDirectory.dir("staging-deploy"))
    archiveFileName.set("central-bundle.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
}
