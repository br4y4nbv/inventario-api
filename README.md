# Inventario API

API REST de inventario con Spring Boot, autenticación JWT, PostgreSQL y Docker.
![CI](https://github.com/br4y4nbv/inventario-api/actions/workflows/ci.yml/badge.svg)

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

## Ejecutar con Docker

Requisitos: Docker y Docker Compose.

```bash
git clone https://github.com/br4y4nbv/inventario-api.git
cd inventario-api
cp .env.example .env     
```

Edita `.env`: define `DB_PASSWORD`, una `JWT_SECRET` aleatoria de al menos 32 caracteres
(por ejemplo, `openssl rand -base64 48`) y, si quieres un administrador inicial,
`ADMIN_EMAIL` y `ADMIN_PASSWORD`. Después:

```bash
docker compose -f compose.prod.yaml up --build
```

La API queda en http://localhost:8080 (Swagger en `/swagger-ui.html`) con PostgreSQL y
datos persistentes. `docker compose -f compose.prod.yaml down -v` borra también la base de datos.

## Ejecutar en desarrollo

Requisitos: JDK 21 y Docker. Spring Boot levanta PostgreSQL automáticamente desde `compose.yaml`.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

El perfil `dev` crea el administrador `admin@inventario.local` (credenciales solo de desarrollo local).

## Estado

En desarrollo.