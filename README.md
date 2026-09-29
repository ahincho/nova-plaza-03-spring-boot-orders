# plaza-orders

**El servicio de pedidos de [Plaza](https://github.com/ahincho/nova-plaza-01-shared-platform)**, la
plataforma de compras construida sobre Nova. Está hecho en Spring Boot y es dueño de los pedidos y de
su estado.

Lo llama solo el BFF, que valida el token de Keycloak y pasa el cliente en `X-Customer-Id`. El
precio de cada línea es el que devolvió la reserva del catálogo, nunca el del cliente.

## API

| Método | Ruta | Qué hace |
|---|---|---|
| `POST` | `/v1/orders` | crea un pedido pendiente; lleva `Idempotency-Key`, y repetir la clave devuelve el mismo pedido con 200 |
| `GET` | `/v1/orders/{id}` | devuelve un pedido del cliente; el de otro cliente es un 404 |
| `GET` | `/v1/orders` | lista los pedidos del cliente, del más nuevo al más viejo |

Confirmar y cancelar llegan con el módulo de errores de Nova, porque una transición inválida tiene
que responder 409.

## Lo que usa de Nova

| Pieza | Para qué |
|---|---|
| `pe.edu.nova.java.spring-boot-service` | el toolchain de Java: Spring Boot con el estándar de API, formato, Checkstyle, cobertura, validación de commits, OWASP y la imagen |
| `nova-secrets-spring-boot-starter` y `nova-secrets-vault` | las credenciales de la base salen de Vault, del secreto `plaza/orders/db` |
| `nova-observability-spring-boot-starter` | trazas, logs y métricas por OTLP |
| `nova-architecture-rules` | las reglas de capas, como una prueba más |

## Correrlo en local

Levantar Postgres y Vault desde
[`nova-plaza-01-shared-platform`](https://github.com/ahincho/nova-plaza-01-shared-platform) con
`docker compose up -d --wait`, y después:

```bash
export VAULT_ADDR=http://localhost:8200 VAULT_TOKEN=plaza-local-root
./gradlew bootRun
```

Escucha en el puerto 8081. Las dependencias de Nova están en GitHub Packages, así que Gradle necesita
`GITHUB_ACTOR` y un `GITHUB_TOKEN` con `read:packages`.

## Pruebas y calidad

```bash
./gradlew build
./gradlew novaFormat
./gradlew novaDocker
```

`build` corre lo mismo que el CI: el formato, Checkstyle, las pruebas y una cobertura mínima del 80 %
de líneas. `novaFormat` corrige el formato. El primer build instala un hook que valida cada mensaje de
commit con Conventional Commits, y el CI valida los commits de cada PR.

Las pruebas levantan un Postgres y un Vault reales con Testcontainers, así que piden Docker. El
servicio lee sus credenciales de Vault igual que en producción.

## Licencia

[Eclipse Public License 2.0](LICENSE).
