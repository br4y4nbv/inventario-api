# Inventario API

API REST de inventario con Spring Boot, autenticación JWT, PostgreSQL y Docker.

## Tecnologías

- Java 21 y Spring Boot 4
- Spring Data JPA, Spring Security (JWT)
- PostgreSQL y Flyway
- JUnit 5, Mockito y Testcontainers
- Docker y GitHub Actions

## Documentación de la API

Con la aplicación en marcha, la documentación interactiva está en
`http://localhost:8080/swagger-ui.html` y la especificación OpenAPI en `/v3/api-docs`.

Para probar los endpoints protegidos: inicia sesión en `POST /api/auth/login`, copia el
`accessToken`, pulsa **Authorize** y pégalo sin el prefijo "Bearer".

Listados: todos aceptan `page`, `size` (máximo 100), `sort` y `dir`. Ordenar por un campo
no permitido devuelve 400.

## Pruebas

```bash
./mvnw verify
```

- **Unitarias** (Mockito, sin base de datos): reglas de negocio de los servicios y lógica de reintentos.
- **Integración** (Testcontainers con PostgreSQL real): migraciones, API, seguridad con tokens reales y concurrencia.
- **Cobertura**: informe en `target/site/jacoco/index.html`; el build falla si la cobertura de líneas baja del 75 %.

Requiere Docker en ejecución para las pruebas de integración.

## Estado

En desarrollo.