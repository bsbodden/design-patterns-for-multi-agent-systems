plugins {
    java
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.redis.demos"
version = "0.0.1-SNAPSHOT"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")

    // LangChain4J core
    implementation("dev.langchain4j:langchain4j:1.11.0")
    implementation("dev.langchain4j:langchain4j-open-ai:1.11.0")

    // LangChain4J Agentic (beta — provides AgenticScope, workflows, supervisor)
    implementation("dev.langchain4j:langchain4j-agentic:1.11.0-beta19")

    // Redis OM Spring — type-safe Redis JSON documents + RediSearch queries
    implementation("com.redis.om:redis-om-spring:1.0.7")
    annotationProcessor("com.redis.om:redis-om-spring:1.0.7")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("com.redis:testcontainers-redis:2.2.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Load env vars from code/.env for bootRun and test tasks
val envFile = file("../.env")
val envVars = if (envFile.exists()) {
    envFile.readLines()
        .filter { it.contains("=") && !it.startsWith("#") }
        .associate { line ->
            val (key, value) = line.split("=", limit = 2)
            key.trim() to value.trim()
        }
} else emptyMap()

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    envVars.forEach { (key, value) -> environment(key, value) }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
    maxParallelForks = 1
    jvmArgs = listOf("-Xmx2g")
    envVars.forEach { (key, value) -> environment(key, value) }
}
