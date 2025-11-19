plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("org.springframework.cloud.contract") version "4.1.4"
    `maven-publish`
}

// Platform Core 버전 관리
val platformCoreVersion = "1.2.5"
// Contract Hub 버전 (Avro 이벤트 스키마)
val contractHubVersion = "v1.0.0"
// Confluent Platform 버전 (Schema Registry)
val confluentVersion = "7.5.1"
// Spring Cloud Contract 버전
val springCloudContractVersion = "4.1.4"

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Spring
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Contract Hub - Avro Event Schemas (JitPack)
    implementation("com.github.GroomC4:c4ang-contract-hub:$contractHubVersion")

    // Apache Avro
    implementation("org.apache.avro:avro:1.11.3")

    // Confluent Schema Registry & Avro Serializer
    implementation("io.confluent:kafka-avro-serializer:$confluentVersion")
    // Spring Security 제거: Istio API Gateway가 인증/인가 처리
    // implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.retry:spring-retry")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.4.0")

    // Spring Cloud BOM (Spring Boot 3.3.4와 호환)
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2023.0.3"))

    // Spring Cloud OpenFeign (버전은 BOM에서 관리)
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")

    // Redisson (Redis 클라이언트 with 원자적 연산 지원)
    implementation("org.redisson:redisson-spring-boot-starter:3.24.3")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.7.3")

    // Platform Core - DataSource (Production)
    implementation("com.groom.platform:datasource-starter:$platformCoreVersion")

    // Platform Core - Testcontainers (테스트 전용)
    testImplementation("com.groom.platform:testcontainers-starter:$platformCoreVersion")

    // Spring Cloud Contract (Provider-side testing)
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-verifier:$springCloudContractVersion")
    testImplementation("io.rest-assured:rest-assured:5.3.2")
    testImplementation("io.rest-assured:spring-mock-mvc:5.3.2")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Spring Security Test 제거: Istio가 인증 처리
    // testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.mockk:mockk:1.14.5")

    // K3s Module 추가
    testImplementation("org.testcontainers:k3s:1.19.7")
    testImplementation("io.fabric8:kubernetes-client:6.10.0")
    testImplementation("org.bouncycastle:bcpkix-jdk18on:1.78")
}

// 모든 Test 태스크에 공통 설정 적용
tasks.withType<Test> {
    // 메모리 설정 (통합테스트 Testcontainers 실행을 위해)
    minHeapSize = "512m"
    maxHeapSize = "2048m"

    systemProperty("user.timezone", "KST")
    jvmArgs("--add-opens", "java.base/java.time=ALL-UNNAMED")

    // 테스트 실행 로깅
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

tasks.test {
    useJUnitPlatform()
}

// 통합 테스트 전용 태스크 (Docker Compose 기반)
val integrationTest by tasks.registering(Test::class) {
    description = "Runs integration tests with Docker Compose"
    group = "verification"

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    useJUnitPlatform {
        includeTags("integration-test")
    }

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }

    shouldRunAfter(tasks.test)
}

// Docker Compose 연동 태스크 추가
val dockerComposeUp by tasks.registering(Exec::class) {
    group = "docker"
    description = "Run docker compose up for local infrastructure."
    commandLine(
        "sh",
        "-c",
        "command -v docker >/dev/null 2>&1 && docker compose up -d || echo 'docker not found, skipping docker compose up'",
    )
    workingDir = project.projectDir
}
val dockerComposeDown by tasks.registering(Exec::class) {
    group = "docker"
    description = "Run docker compose down for local infrastructure."
    commandLine(
        "sh",
        "-c",
        "command -v docker >/dev/null 2>&1 && docker compose down || echo 'docker not found, skipping docker compose down'",
    )
    workingDir = project.projectDir
}
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    dependsOn(dockerComposeUp)
    finalizedBy(dockerComposeDown)
}

// Spring Cloud Contract 설정
contracts {
    testMode.set(org.springframework.cloud.contract.verifier.config.TestMode.MOCKMVC)
    baseClassForTests.set("com.groom.payment.common.ContractTestBase")
    contractsDslDir.set(file("src/test/resources/contracts"))
}

// Contract Stub 발행 설정 (Consumer가 사용할 수 있도록 GitHub Packages에 발행)
publishing {
    publications {
        create<MavenPublication>("stubs") {
            groupId = "com.groom"
            artifactId = "payment-service-contract-stubs"
            version = project.version.toString()

            // Contract Stub JAR을 발행
            artifact(tasks.named("verifierStubsJar"))
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/GroomC4/c4ang-payment-service")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as String?
                password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key") as String?
            }
        }
    }
}
