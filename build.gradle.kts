plugins {
    // El toolchain de Java de Nova (ADR-044): Java 25, Spring Boot con los starters de Nova, formato,
    // Checkstyle, cobertura, validación de commits, OWASP, el SBOM y la imagen. Desde la 2.1.0 trae también
    // spring-boot-starter-webmvc-test para las pruebas con MockMvc.
    id("pe.edu.nova.java.spring-boot-service") version "3.0.0"
}

group = "pe.edu.nova.plaza"
version = findProperty("version") as String

val novaSecrets = "1.2.0"

dependencies {
    implementation("pe.edu.nova.java.starters:nova-observability-spring-boot-starter:3.0.0")
    implementation("pe.edu.nova.java.starters:nova-secrets-spring-boot-starter:$novaSecrets")
    implementation("pe.edu.nova.java.starters:nova-idempotency-spring-boot-starter:0.1.1")
    // Los comandos y las consultas del servicio, con su auditoría, validación y transacción (ADR-053).
    implementation("pe.edu.nova.java.starters:nova-cqrs-spring-boot-starter:0.1.0")
    runtimeOnly("pe.edu.nova.java.libs:nova-secrets-vault:$novaSecrets")

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-vault")
}
