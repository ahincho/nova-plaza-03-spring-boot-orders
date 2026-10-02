# plaza-orders

**El servicio de pedidos de [Plaza](https://github.com/ahincho/nova-plaza-01-shared-platform)**, la
plataforma de compras construida sobre Nova. Está hecho en Spring Boot y es dueño de los pedidos y de
su estado.

Lo llama solo el BFF, que valida el token de Keycloak y pasa el cliente en `X-Customer-Id`. El
precio de cada línea es el que devolvió la reserva del catálogo, nunca el del cliente.

## API

| Método | Ruta | Qué hace |
|---|---|---|
| `POST` | `/v1/orders` | crea un pedido pendiente; lleva `Idempotency-Key`, y repetir la compra devuelve la misma respuesta |
| `GET` | `/v1/orders/{id}` | devuelve un pedido del cliente; el de otro cliente es un 404, igual que uno que no existe |
| `GET` | `/v1/orders` | una página de los pedidos del cliente, del más nuevo al más viejo, para un scroll infinito; ver abajo |

La compra es idempotente con la capacidad de Nova
([ADR-047](https://github.com/ahincho/nova-shared-01-docs/blob/main/adrs/shared/ADR-047-idempotencia-detras-de-un-contrato.md)).
La clave vale por cliente, el que llega en `X-Customer-Id`:

| Situación | Respuesta |
|---|---|
| La compra se repite con la misma clave | la misma respuesta, con `Idempotent-Replayed: true`, sin crear otro pedido |
| Otro cliente usa la misma clave | es otra compra |
| La misma clave llega con otro pedido | 422, `IDEMPOTENCY_KEY_REUSED` |
| La primera compra sigue en curso | 409, `IDEMPOTENCY_KEY_IN_USE`, con `Retry-After` |
| Falta la clave | 400, `IDEMPOTENCY_KEY_REQUIRED` |

El pedido y la respuesta guardada se confirman en el mismo commit, así que una caída entre los dos no
crea un segundo pedido. Los registros viven en la tabla `idempotency_record`, que crea la migración `V2`.

Los errores siguen el modelo por capas de Nova
([ADR-031](https://github.com/ahincho/nova-shared-01-docs/blob/main/adrs/shared/ADR-031-modulo-de-errores-por-capas-con-trazabilidad.md)):
el servicio lanza lo que salió mal y el starter de Nova lo responde con el sobre, `metadata.traceId` y
una línea de log.

| Situación | Respuesta |
|---|---|
| El pedido no existe o es de otro cliente | 404, `ORDER_NOT_FOUND` (un `DomainError`) |
| El pedido no es válido | 400, `BAD_REQUEST`, con un error por cada campo |

Confirmar y cancelar llegan después, y una transición inválida va a responder 409 con un
`DomainError.conflict`.

### El listado, por cursor

`GET /v1/orders` no devuelve todos los pedidos de una vez: devuelve una página, con la persistencia de Nova
([ADR-054](https://github.com/ahincho/nova-shared-01-docs/blob/main/adrs/shared/ADR-054-persistencia-reutilizable-con-paginacion-por-cursor.md)).
Se pide con `?limit=`, 20 por defecto y 100 como máximo, y la siguiente con el `?cursor=` que devolvió la
anterior, tal como llegó:

```json
{
  "success": true,
  "status": 200,
  "data": {
    "items": [ { "id": "…", "status": "PENDING", "items": [ … ] } ],
    "nextCursor": "eyJ2IjoxLCJzIjoibmV3ZXN0Iiwia…",
    "hasNext": true
  }
}
```

En la última página, `hasNext` es `false` y `nextCursor` es `null`. El orden es `createdAt` descendente y el
`id` como desempate, así que un pedido nuevo mientras se navega no hace repetir ni saltar ninguno. Un cursor
inválido es un 400 con el campo `cursor`, y un límite fuera de rango un 400 con el campo `limit`. El índice
`orders_customer_created_id`, de la migración `V4`, sigue ese mismo orden.

Cada pedido guarda además quién y cuándo lo creó y lo cambió, y su versión: `Order` hereda de
`AuditableEntity`, y el actor es el mismo cliente que registra la auditoría del bus. Si dos cambios pisan la
misma versión, o un dato choca con uno que ya existe, la respuesta es 409, con `CONCURRENT_MODIFICATION` o
`DATA_CONFLICT`, en lugar de un 500.

## Lo que usa de Nova

| Pieza | Para qué |
|---|---|
| `pe.edu.nova.java.spring-boot-service` | el toolchain de Java: Spring Boot con el estándar de API, formato, Checkstyle, cobertura, validación de commits, OWASP y la imagen |
| `nova-secrets-spring-boot-starter` y `nova-secrets-vault` | las credenciales de la base salen de Vault, del secreto `plaza/orders/db` |
| `nova-observability-spring-boot-starter` | trazas, logs y métricas por OTLP |
| `nova-idempotency-spring-boot-starter` | la compra idempotente, con el almacén en la misma base de pedidos |
| `nova-cqrs-spring-boot-starter` | los comandos y las consultas, con su auditoría, validación y transacción |
| `nova-persistence-spring-boot-starter` | la página por cursor del listado, la entidad auditable y los 409 de la base |
| `nova-architecture-rules` | las reglas de capas, como una prueba más |

## Comandos y consultas

El controlador no tiene lógica: cada operación es un mensaje que entrega al `CommandBus` o al
`QueryBus` de [`nova-java-27-cqrs`](https://github.com/ahincho/nova-java-27-cqrs)
([ADR-053](https://github.com/ahincho/nova-shared-01-docs/blob/main/adrs/shared/ADR-053-cqrs-con-command-bus-y-query-bus.md)).

| Operación | Mensaje | Devuelve | Handler |
|---|---|---|---|
| `POST /v1/orders` | `PlaceOrder`, un comando | el identificador del pedido | `PlaceOrderHandler` |
| `GET /v1/orders/{id}` | `FindOrder`, una consulta | `OrderResponse` | `FindOrderHandler` |
| `GET /v1/orders` | `ListOrders`, una consulta | `CursorPage<OrderResponse>` | `ListOrdersHandler` |

- **La compra devuelve solo el identificador**, y la vista sale de `FindOrder`. Las dos corren en la
  transacción que abre la idempotencia: el bus usa propagación `REQUIRED` y se suma a ella.
- **Una consulta corre de solo lectura**, y arma la vista dentro de su transacción: la entidad nunca
  sale del servicio.
- **Cada mensaje se audita**, comandos y consultas, en el logger `nova.audit`. El actor es el cliente
  de `X-Customer-Id`: `CustomerActorResolver` reemplaza al de Nova, que lo tomaría de Spring Security.

```
COMMAND PlaceOrder by customer-7: SUCCEEDED - in 14 ms
QUERY FindOrder by customer-7: SUCCEEDED - in 3 ms
```

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
