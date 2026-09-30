plugins {
    // El toolchain de Java de Nova (ADR-044): Java 25, Spring Boot con los starters de Nova, formato,
    // Checkstyle, cobertura, validación de commits, OWASP, el SBOM y la imagen.
    id("pe.edu.nova.java.spring-boot-service") version "1.3.1"
}

group = "pe.edu.nova.plaza"
version = findProperty("version") as String

val novaSecrets = "1.0.0"

dependencies {
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

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-vault")
}
