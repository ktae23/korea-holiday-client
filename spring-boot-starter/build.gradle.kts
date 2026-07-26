import com.vanniktech.maven.publish.SonatypeHost

plugins {
    `java-library`
    id("com.vanniktech.maven.publish") version "0.30.0"
}

repositories {
    mavenCentral()
}

val springBootVersion = "3.3.4"

dependencies {
    api(project(":lib"))
    api("org.springframework.boot:spring-boot-autoconfigure:$springBootVersion")

    compileOnly("org.springframework.boot:spring-boot-configuration-processor:$springBootVersion")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:$springBootVersion")

    testImplementation("org.springframework.boot:spring-boot-test:$springBootVersion")
    testImplementation("org.springframework:spring-context:6.1.13")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = false)
    signAllPublications()

    coordinates(project.group.toString(), "korea-holiday-spring-boot-starter", project.version.toString())

    pom {
        name.set("korea-holiday-spring-boot-starter")
        description.set("korea-holiday-client를 스프링 부트에서 자동설정으로 사용하는 스타터")
        url.set("https://github.com/ktae23/korea-holiday-client")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("ktae23")
                name.set("ktae23")
                url.set("https://github.com/ktae23")
            }
        }
        scm {
            url.set("https://github.com/ktae23/korea-holiday-client")
            connection.set("scm:git:git://github.com/ktae23/korea-holiday-client.git")
            developerConnection.set("scm:git:ssh://git@github.com/ktae23/korea-holiday-client.git")
        }
    }
}
