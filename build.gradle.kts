plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "2.3.21"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Web
    implementation("org.springframework.boot:spring-boot-starter-web")

    // JPA & H2
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.h2database:h2")

    // Tsid
    implementation("io.hypersistence:hypersistence-utils-hibernate-71:3.15.4")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Development
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Test support
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    testImplementation("io.kotest:kotest-assertions-core:6.1.11")

    // Logging
    implementation("org.springframework.boot:spring-boot-starter-log4j2")
    implementation("io.github.oshai:kotlin-logging-jvm:6.0.9")
}

configurations.all {
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

val asyncApiSnippetsDir = layout.buildDirectory.dir("asyncapi-snippets")
val asyncApiOutputDir = layout.buildDirectory.dir("asyncapi")

tasks.withType<Test> {
    useJUnitPlatform()
}

// 통합 테스트가 통과해야 build/asyncapi/asyncapi.yaml 이 생기고,
// 그 결과물을 static/ 로 복사해 런타임에 앱이 정적 리소스로 서빙한다 (테스트 통과 = 문서 존재).
val cleanAsyncApiArtifacts = tasks.register<Delete>("cleanAsyncApiArtifacts") {
    delete(asyncApiSnippetsDir, asyncApiOutputDir)
}

tasks.test {
    dependsOn(cleanAsyncApiArtifacts)
}

val copyAsyncApiSpec = tasks.register<Copy>("copyAsyncApiSpec") {
    dependsOn(tasks.test)
    from(asyncApiOutputDir.map { it.file("asyncapi.yaml") })
    into(layout.buildDirectory.dir("resources/main/static"))
}

tasks.register("generateDocs") {
    group = "documentation"
    description = "통합 테스트 실행 → AsyncAPI 3.0 YAML 생성 → static/ 복사"
    dependsOn(copyAsyncApiSpec)
}

tasks.named("resolveMainClassName") { dependsOn(copyAsyncApiSpec) }
tasks.bootJar { dependsOn(copyAsyncApiSpec) }
tasks.bootRun { dependsOn(copyAsyncApiSpec) }

tasks.named<Jar>("jar") {
    enabled = false
}
