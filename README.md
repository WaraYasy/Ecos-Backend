# ECOS — Backend

> Backend de la aplicación móvil **ECOS**: descubrimiento musical inteligente a través de un grafo neuronal interactivo.

---

## ¿Qué es ECOS?

ECOS es una app móvil donde el usuario no "busca" música, sino que **viaja** a través de ella. Cada canción es un nodo en un grafo interactivo con conexiones hacia otras canciones basadas en dimensiones culturales, técnicas, temporales y cinematográficas.

Este repositorio contiene el **backend Spring Boot** que alimenta la app.

---

## Stack

| Capa | Tecnología |
|---|---|
| Framework | Spring Boot 4.0.2 · Java 21 |
| Base de datos | PostgreSQL 16 + extensión `pgvector` |
| Migraciones | Flyway (V1–V27) |
| APIs externas | Spotify · Last.fm · TMDb · MusicBrainz |
| Seguridad | JWT (jjwt 0.12.6) · BCrypt · Spring Security · OAuth2 Spotify |
| Caché | Caffeine (in-memory, TTL por capa) |
| Tests | JUnit 5 · Mockito · Testcontainers · MockRestServiceServer |
| Docs API | SpringDoc OpenAPI (Swagger UI) |

---

## Requisitos previos

- **Java 21** o superior
- **Docker Desktop** en ejecución (para PostgreSQL local)
- Cuentas de desarrollador con claves de API:
  - [Spotify for Developers](https://developer.spotify.com)
  - [The Movie Database (TMDb)](https://www.themoviedb.org/settings/api)
  - [Last.fm API](https://www.last.fm/api/account/create)

---

## Arrancar en local

### 1. Clonar el repositorio

```bash
git clone <url-del-repo>
cd Ecos-Backend
```

### 2. Configurar variables de entorno

Crear un fichero `.env` en la raíz (no se sube a Git):

```env
# Base de datos
DB_PASSWORD=tu_password_local

# Spotify (https://developer.spotify.com/dashboard)
SPOTIFY_CLIENT_ID=tu_client_id
SPOTIFY_CLIENT_SECRET=tu_client_secret
SPOTIFY_REDIRECT_URI=http://127.0.0.1:3000/callback/spotify

# TMDb (https://www.themoviedb.org/settings/api)
TMDB_API_KEY=tu_api_key
TMDB_API_TOKEN=tu_read_access_token

# Last.fm (https://www.last.fm/api/account/create)
LASTFM_API_KEY=tu_api_key

# JWT (cadena aleatoria >= 32 caracteres)
JWT_SECRET=cambia_esto_por_una_cadena_aleatoria_larga

# Cifrado AES para tokens OAuth en BD
ENCRYPTION_KEY=tu_clave_cifrado
ENCRYPTION_SALT=deadbeefdeadbeef

# Admin (protege /v1/admin/**)
ADMIN_API_KEY=clave_secreta_larga
```

### 3. Iniciar PostgreSQL con Docker

```bash
docker run -d \
  --name ecos_postgres \
  -e POSTGRES_DB=ecos_db \
  -e POSTGRES_USER=ecos \
  -e POSTGRES_PASSWORD=tu_password_local \
  -p 5432:5432 \
  pgvector/pgvector:pg16
```

Flyway aplica las migraciones automáticamente al arrancar.

### 4. Arrancar la aplicación

```bash
./mvnw spring-boot:run
```

La API queda disponible en `http://localhost:3000`.  
Documentación interactiva: `http://localhost:3000/swagger-ui.html`

---

## Endpoints principales

```
# Autenticación
POST /v1/auth/login                        body: {email, password} → {token, userId}
GET  /v1/auth/spotify                      login con Spotify (público)
POST /v1/users                             body: {username, email, password, language}

# Usuarios
GET    /v1/users/{id}
PATCH  /v1/users/{id}                      body: {username?, language?}
PATCH  /v1/users/{id}/password             body: {currentPassword, newPassword}

# Búsqueda multidimensional (cuadrantes combinables)
GET  /v1/search?q=&aesthetic=&decade=&movie=&genre=&country=&instrumental=&...

# Canciones
GET  /v1/songs/{id}
GET  /v1/songs/{id}/related

# Playlists
GET    /v1/playlists
POST   /v1/playlists
GET    /v1/playlists/{id}
DELETE /v1/playlists/{id}
POST   /v1/playlists/{id}/songs
DELETE /v1/playlists/{id}/songs/{songId}
POST   /v1/playlists/{id}/export/spotify

# Grafo
GET  /v1/graph?songId=

# OAuth Spotify
GET  /v1/auth/spotify                      inicia login con Spotify (público)
GET  /v1/auth/spotify/login                vincula cuenta Spotify (🔒)
GET  /callback/spotify                     callback OAuth (solo Spotify → backend)

# Admin
POST /v1/admin/songs/import                importar CSV (requiere X-Admin-Key)

# Diagnóstico
GET  /health
```

Todos los endpoints excepto login, registro, `/v1/auth/spotify` y health requieren:
```
Authorization: Bearer <token>
```

Documentación completa: `docs/API_ENDPOINTS.md`

---

## Tests

```bash
# Suite completa (requiere Docker para Testcontainers)
./mvnw test
```

| Suite | Tests |
|---|---|
| Use cases (Mockito) | 24 |
| Controllers (`@WebMvcTest`) | 46 |
| SpotifyClient (MockRestServiceServer) | 5 |
| Funcionales (Testcontainers) | 54 |
| BackendApplicationTests | 1 |
| **Total** | **273** |

Los tests funcionales arrancan un contenedor PostgreSQL real. Requieren Docker Desktop en ejecución.

---

## Arquitectura

El backend sigue la **arquitectura hexagonal (Ports & Adapters)**:

```
interfaces/rest/   ← Controllers, DTOs, GlobalExceptionHandler
application/       ← Use cases, MusicAggregationService
domain/            ← Models, Ports — sin Spring ni JPA
infrastructure/    ← JPA, Adapters (Spotify/Last.fm/TMDb/MusicBrainz), JWT, Caché
```

El dominio no depende de nada externo. La infraestructura implementa los Ports del dominio (inversión de dependencia).

---

## Variables de entorno

| Variable | Descripción | Requerida |
|---|---|---|
| `DB_PASSWORD` | Contraseña de PostgreSQL | Sí |
| `SPOTIFY_CLIENT_ID` | Client ID de Spotify | Sí |
| `SPOTIFY_CLIENT_SECRET` | Client Secret de Spotify | Sí |
| `SPOTIFY_REDIRECT_URI` | URI de callback OAuth (default: `http://127.0.0.1:3000/callback/spotify`) | No |
| `TMDB_API_KEY` | API Key de TMDb | Sí |
| `TMDB_API_TOKEN` | Read Access Token de TMDb | Sí |
| `LASTFM_API_KEY` | API Key de Last.fm | Sí |
| `JWT_SECRET` | Clave HMAC-SHA256 para tokens JWT (mín. 32 chars) | Sí |
| `JWT_EXPIRATION_MS` | Duración del token en ms (default: `86400000` = 24 h) | No |
| `ENCRYPTION_KEY` | Clave para cifrado AES-256 de tokens OAuth en BD | Sí |
| `ENCRYPTION_SALT` | Salt hexadecimal de exactamente 16 chars (ej: `deadbeefdeadbeef`) | Sí |
| `ADMIN_API_KEY` | Clave para proteger `/v1/admin/**` (mín. 32 chars recomendado) | Sí |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos separados por coma | No (default: localhost) |
| `APP_DEEP_LINK_SCHEME` | Scheme del deep link móvil (default: `ecos`) | No |
