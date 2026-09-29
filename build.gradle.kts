import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    // Java 25, Spring Boot 4.0.8 y los starters de máscara y estándar de API de Nova.
    id("pe.edu.nova.java.spring-boot") version "1.0.3"
    checkstyle
    jacoco
}

group = "pe.edu.nova.plaza"
version = findProperty("version") as String

checkstyle {
    sourceSets = listOf(project.sourceSets.main.get())
}

// Los paquetes de Nova están en GitHub Packages, un registro por repositorio.
repositories {
    val readToken = System.getenv("NOVA_PACKAGES_READ_TOKEN") ?: System.getenv("GITHUB_TOKEN")
    listOf(
        "nova-java-01-api-standard",
        "nova-java-07-architecture-rules",
        "nova-java-08-commons-spring-boot-starter",
        "nova-java-09-observability-spring-boot-starter",
        "nova-java-23-secrets",
    ).forEach { repo ->
        maven {
            name = repo.replace("-", "")
            url = uri("https://maven.pkg.github.com/ahincho/$repo")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = readToken
            }
        }
    }
}

val novaSecrets = "1.0.0"
val testcontainers = "2.0.5"

dependencies {
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))

    implementation("pe.edu.nova.java.starters:nova-observability-spring-boot-starter:2.0.1")
    implementation("pe.edu.nova.java.starters:nova-secrets-spring-boot-starter:$novaSecrets")
    runtimeOnly("pe.edu.nova.java.libs:nova-secrets-vault:$novaSecrets")

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.testcontainers:testcontainers-postgresql:$testcontainers")
    testImplementation("org.testcontainers:testcontainers-vault:$testcontainers")
    testImplementation("pe.edu.nova.java.libs:nova-architecture-rules:1.1.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
    }
}
