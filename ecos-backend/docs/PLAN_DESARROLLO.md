# Plan de Desarrollo — Ecos Backend (Fase 2)

> Basado en: `ECOS_CONTEXT.md` (Memoria del proyecto v2, 23/12/2025)
> Periodo Fase 2: 20/01/2026 – 21/02/2026 · 120h estimadas
> Autora: Wara Yasy Pacheco Arandia

---

## Contexto del proyecto

ECOS es una app móvil de descubrimiento musical que navega un **grafo neuronal interactivo** donde cada canción es un nodo. El backend en Spring Boot con arquitectura hexagonal agrega datos de **Spotify + TMDb + Last.fm** en tiempo real, con PostgreSQL + pgvector para búsqueda semántica vectorial.

**Stack del backend:**
- Spring Boot 4 (Java 21) — Arquitectura Hexagonal (Ports & Adapters)
- PostgreSQL 16 + extensión `pgvector` (relacional + vectorial)
- APIs externas: Spotify Web API · TMDb API · Last.fm API
- Microservicio IA (FastAPI, Python) — comunicación HTTP interna

---

## Estado actual del proyecto (22/02/2026)

| Componente | Estado | Notas |
|---|---|---|
| Spring Boot + config base | Parcial | Falta Actuator, perfiles, BD activa |
| Modelos de dominio | Parcial | Faltan `Artist` y `Album` |
| Entidades JPA | Parcial | Faltan `artists`, `albums`, `song_embeddings`, `session_edges` |
| Ports + Use cases | Parcial | Estructura creada, sin implementar |
| Adaptadores Spotify / TMDb / Last.fm + DTOs | Parcial | Estructura + DTOs, sin lógica real |
| Excepciones personalizadas | Hecho | |
| Mappers + Repository implementations | Parcial | Estructura creada, sin lógica real |
| Migraciones BD con Flyway + pgvector | **Pendiente** | |
| Testing setup | **Pendiente** | Solo test básico |
| Cliente HTTP centralizado con retry | **Pendiente** | |
| Sistema de caché con TTL por tipo | **Pendiente** | |
| Algoritmo de similitud multidimensional | **Pendiente** | |
| Motor de búsqueda multidimensional | **Pendiente** | |
| Endpoints API REST `/v1/` | **Pendiente** | |
| Endpoints de grafo | **Pendiente** | |

---

## Orden de ejecución

```
A → B ─┐
A → C ─┼→ D → E → F → G
```

Los grupos B y C pueden trabajarse en paralelo una vez que A esté listo.

---

## Grupo A — Infraestructura base
> Prerequisito de todo lo demás.

| Tarea | Estado |
|---|---|
| 2.1 Configuración de Spring Boot | ✅ Completado |
| 2.2 Migraciones de base de datos | ✅ Completado |
| 2.3 Setup de testing | ✅ Completado |

### 2.1 Configuración de Spring Boot ✅
**Implementado el 24/02/2026**
- `pom.xml`: activado JPA, driver PostgreSQL, Actuator y `spring-boot-starter-test`. Limpiado duplicado comentado de JPA
- `application.properties`: perfil activo `dev`, `ddl-auto=validate`, Actuator solo expone `/actuator/health`
- `application-dev.properties`: PostgreSQL local, SQL visible en consola
- `application-prod.properties`: toda la config desde variables de entorno

### 2.2 Migraciones de base de datos ✅
**Implementado el 24/02/2026**
- `pom.xml`: añadido `flyway-core` y `flyway-database-postgresql`
- Scripts en `src/main/resources/db/migration/`:
  - `V1` artists, albums
  - `V2` songs, song_artists
  - `V3` audio_features
  - `V4` song_tags
  - `V5` movies, song_movies
  - `V6` users, user_profiles (con JSONB)
  - `V7` playlists, playlist_songs
  - `V8` sessions, session_nodes, session_edges
  - `V9` user_interactions (con ENUM interaction_action)
  - `V10` índices GIN full-text + índices para filtros y JOINs

> **Pendiente (Fase 5):** V11 pgvector extension + song_embeddings, V12 índices ivfflat

---
_Descripción original de la tarea:_
- Añadir Flyway al `pom.xml`
- Usar imagen Docker `pgvector/pgvector:pg16` (ya definida en `ECOS_CONTEXT.md`)
- Scripts SQL versionados:

```
V1__create_extension_pgvector.sql     → CREATE EXTENSION IF NOT EXISTS vector;
V2__create_artists_albums.sql         → artists, albums, song_artists
V3__create_songs.sql                  → songs (con FK a albums)
V4__create_audio_features.sql         → audio_features
V5__create_tags.sql                   → song_tags (song_id, tag, weight)
V6__create_movies.sql                 → movies, song_movies
V7__create_embeddings.sql             → song_embeddings (embedding vector(384))
V8__create_users.sql                  → users, user_profiles (con vector(384))
V9__create_playlists.sql              → playlists, playlist_songs
V10__create_sessions.sql              → sessions, session_nodes, session_edges
V11__create_interactions.sql          → user_interactions
V12__create_indexes.sql               → índices GIN (texto), ivfflat (vectores)
```

Índices clave definidos en el contexto:
```sql
CREATE INDEX ON songs USING GIN(to_tsvector('english', title));
CREATE INDEX ON song_embeddings USING ivfflat (embedding vector_cosine_ops);
CREATE INDEX ON user_profiles USING ivfflat (profile_embedding vector_cosine_ops);
```

### 2.3 Setup de testing ✅
**Implementado el 24/02/2026**
- `pom.xml`: añadido `spring-boot-testcontainers`, `testcontainers/junit-jupiter` y `testcontainers/postgresql`
- `src/test/resources/application-test.properties`: logs limpios, Flyway en INFO
- `BaseIntegrationTest`: clase base con `@Testcontainers` + `@ServiceConnection` usando `pgvector/pgvector:pg16`. Contenedor compartido (static) entre todos los tests
- `BackendApplicationTests`: extiende `BaseIntegrationTest`, verifica que el contexto arranca con BD real y migraciones aplicadas

---
_Descripción original de la tarea:_
- Añadir dependencia **Testcontainers** al `pom.xml` (módulos `junit-jupiter` + `postgresql`)
- Crear clase base `@SpringBootTest` con `@Testcontainers` que levanta `pgvector/pgvector:pg16` automáticamente
- Añadir `application-test.properties` apuntando al contenedor (Testcontainers gestiona el puerto dinámico)
- Verificar que `BackendApplicationTests` levanta correctamente con el contenedor

**Estrategia de tests:**
```
Tests unitarios (use cases, VOs, lógica pura)  → sin BD, solo mocks
Tests de integración (repositorios, JPA)       → Testcontainers
Tests E2E (flujos completos)                   → Testcontainers + WireMock
```

---

## Grupo B — Dominio funcional
> Depende de A para tests con BD. Paralelo a C.

| Tarea | Estado |
|---|---|
| 2.4 Implementación de entidades | Pendiente |
| 2.5 Implementación de Value Objects | Pendiente |
| 2.6 Implementación de casos de uso | Pendiente |

### 2.4 Implementación de entidades
**Entidades que faltan y deben crearse** (no existen en el código actual):
- `Artist`: `id`, `spotifyId`, `name`, `country`, `genres[]`
- `Album`: `id`, `spotifyId`, `title`, `year`, `coverUrl`, `artistId`
- `ArtistEntity` + `AlbumEntity` en infraestructura JPA
- `ArtistMapper` + `AlbumMapper`

**Entidades existentes que deben completarse:**
- `SongEntity`: añadir FK a `albums`, añadir `previewUrl`, `spotifyId`
- `UserEntity`: añadir `email`, `language`
- `ExplorationSessionEntity`: añadir `startSongId`
- `SessionNodeEntity`: añadir `xPosition`, `yPosition`, `visitedAt`, `sequenceOrder`
- Crear `SessionEdgeEntity`: `id`, `sessionId`, `fromSongId`, `toSongId`, `similarity`
- Crear `SongEmbeddingEntity`: `songId`, `embedding` (para pgvector)
- Crear `UserProfileEntity`: `userId`, `avgEnergy`, `harmonicRichness`, `eclecticity`, `favoriteEras` (jsonb), `topGenres` (jsonb), `profileEmbedding` (vector)

Añadir validaciones con `@NotNull`, `@Size`, `@Min`, `@Max` donde corresponda.
Tests unitarios por entidad.

### 2.5 Implementación de Value Objects

**`AudioFeatures`** — completar con método de similitud coseno:
```
audio_similarity(A, B) = coseno sobre vector normalizado de:
  [energy, tempo_norm, valence, danceability, acousticness,
   instrumentalness, speechiness, loudness_norm]
```

**`GraphNode`** — completar con:
- Posición `(x, y)` para el layout force-directed
- Lista de conexiones `GraphEdge` (máximo 5-8 por nodo)
- Campo `similarity: float`

**Fórmula de similitud entre canciones** (del contexto, sección 12):
```
similarity(A, B) =
  0.35 * audio_similarity(A.features, B.features)     // coseno normalizado
  + 0.25 * semantic_similarity(A.embedding, B.embedding) // pgvector coseno
  + 0.20 * tag_similarity(A.tags, B.tags)             // Jaccard
  + 0.10 * era_proximity(A.year, B.year)               // decaimiento gaussiano
  + 0.10 * movie_overlap(A.movies, B.movies)           // Jaccard películas
```

Tests unitarios para cada componente de la fórmula.

### 2.6 Implementación de casos de uso
Dar implementación real a los use cases existentes:

**Búsqueda:**
- `SearchSongsUseCase`: texto + filtros multidimensionales (decade, energy, valence, genre, movie, aesthetic)
- `GetSimilarSongsUseCase`: calcular similitud con la fórmula anterior, devolver top 5-8

**Playlist:**
- `CreatePlaylistUseCase`, `AddSongToPlaylistUseCase`
- `ExportPlaylistToSpotifyUseCase`: crear playlist en Spotify vía adaptador

**Sesión:**
- `CreateExplorationSessionUseCase`: crear sesión con `startSong`
- `AddNodeToSessionUseCase`: añadir nodo + edge al grafo de sesión
- `GetSessionHistoryUseCase`: devolver ruta navegada completa

**Usuario:**
- `RegisterUserUseCase`, `GetUserProfileUseCase`
- `CalculateUserProfileUseCase`: calcular perfil musical (energía promedio, épocas favoritas, etc.)
- `UpdateUserPreferencesUseCase`

Tests de comportamiento con mocks para todos los use cases.

---

## Grupo C — Integraciones externas
> Depende de A. Paralelo a B.

| Tarea | Estado |
|---|---|
| 2.7 Cliente HTTP base | Pendiente |
| 2.8 Configuración de proxy | Pendiente |
| 2.9 Adaptador de Spotify | Pendiente |
| 2.10 Adaptador de TMDb | Pendiente |
| 2.11 Adaptador de Last.fm | Pendiente |

### 2.7 Cliente HTTP base
- Centralizar configuración de `RestTemplate` o migrar a `WebClient`
- Añadir: timeouts configurables por perfil, retry automático (máx. 3 intentos), logging de requests/responses
- **Circuit breaker** para rate limits (riesgo Alto identificado en el contexto):
  - HTTP 429 → esperar y reintentar con backoff exponencial
  - HTTP 401 → renovar token (Spotify) y reintentar
  - HTTP 5xx → circuit breaker con fallback

### 2.8 Configuración de proxy
- Soporte de proxy configurable por perfil de entorno
- Integrar en el cliente HTTP base
- Compatible con WireMock en tests automatizados

### 2.9 Adaptador de Spotify
Datos que provee (del contexto):
- Catálogo: tracks, artistas, álbumes, portadas, previews 30s
- Audio features: energy, tempo, valence, danceability, acousticness, instrumentalness, speechiness, loudness, key, mode, timeSignature
- Exportar playlists del usuario

Implementación:
- Autenticación **Client Credentials** (ya existe `SpotifyAuthService`) + renovación automática de token
- Completar `SpotifyClient`: `searchTracks()`, `getAudioFeatures()`, `getTrack()`, `createPlaylist()`, `addTracksToPlaylist()`
- Mapear `SpotifyTrackDTO` + `SpotifyAudioFeaturesDTO` → modelos de dominio (`Song`, `AudioFeatures`)
- Manejo de rate limit 429 con retry
- Tests con WireMock (mock del servidor Spotify)

### 2.10 Adaptador de TMDb
Datos que provee:
- Películas y series: título, año, director, géneros cinematográficos
- Para filtros cinematográficos y vinculación canción-película

Implementación:
- Completar `TmdbClient`: `searchMovies()`, `getMovieCredits()`, `getMovieGenres()`
- Mapear `TMDBMovieDTO` + `TMDBCreditDTO` → modelo `Movie`
- Normalización: extraer director de créditos, mapear géneros a categorías internas
- Caché de películas (larga duración: días)
- Tests con WireMock

### 2.11 Adaptador de Last.fm
Datos que provee:
- Tags comunitarios por canción: géneros, moods, estéticas (`cyberpunk`, `cottagecore`, `dreamy`, etc.)
- Tracks similares
- Pesos de relevancia por tag

Implementación:
- Completar `LastFmClient`: `getTrackTags()`, `getSimilarTracks()`
- Mapear `LastFmTagDTO` + `LastFmTrackInfoDTO` → modelo `Tag` con peso
- Normalizar y deduplicar tags (lowercase, trim)
- Caché de tags por canción (media duración: horas)
- Tests con WireMock

---

## Grupo D — Servicios transversales
> Depende de C.

| Tarea | Estado |
|---|---|
| 2.12 Sistema de agregación | Pendiente |
| 2.13 Sistema de caché | Pendiente |

### 2.12 Sistema de agregación
El backend agrega datos de las tres fuentes **en tiempo real sin catálogo propio** (del contexto):

- Crear `MusicAggregationService`:
  1. Buscar en Spotify → obtener track + audio features
  2. Buscar en Last.fm → obtener tags con pesos
  3. Buscar en TMDb → obtener películas relacionadas (si aplica)
  4. Resolver conflictos de metadatos entre fuentes (normalización de nombres)
  5. Combinar en modelo de dominio `Song` completo
- Soporte para procesamiento asíncrono (`CompletableFuture`) para llamadas paralelas a las tres APIs
- Manejo de fuente parcialmente no disponible (degradación elegante)

### 2.13 Sistema de caché
Políticas TTL definidas según tipo de dato (riesgo identificado en contexto):

| Tipo de dato | TTL | Justificación |
|---|---|---|
| Audio features (Spotify) | 7 días | Datos estáticos, raramente cambian |
| Tags (Last.fm) | 6 horas | Pueden actualizarse por la comunidad |
| Películas (TMDb) | 24 horas | Metadatos estables |
| Resultados de búsqueda | 15 minutos | Varían con nuevos datos |
| Token Spotify | Según `expires_in` | Siempre < 1 hora |

- Añadir Spring Cache (`@EnableCaching`) + configuración en `pom.xml`
- Implementar `@Cacheable` en los métodos de los adaptadores
- Métricas de cache hit/miss
- Tests que verifiquen que el caché evita llamadas repetidas a las APIs

---

## Grupo E — Persistencia y búsqueda
> Depende de A + B.

| Tarea | Estado |
|---|---|
| 2.14 Repositorio JPA base | Pendiente |
| 2.15 Búsqueda básica | Pendiente |
| 2.16 Filtros multidimensionales | Pendiente |
| 2.17 Optimización de búsquedas | Pendiente |

### 2.14 Repositorio JPA base
- Completar implementaciones existentes con queries reales (actualmente vacías)
- Añadir paginación con `Pageable` en todos los métodos de listado
- Implementar queries nativas para pgvector (búsqueda por similitud coseno):
  ```sql
  SELECT * FROM song_embeddings
  ORDER BY embedding <=> :queryEmbedding
  LIMIT :limit;
  ```
- Tests de integración con Testcontainers (`pgvector/pgvector:pg16`) — PostgreSQL real desde el principio

### 2.15 Búsqueda básica
Endpoint: `GET /v1/search?q=&page=&size=`

- Búsqueda por texto en título, artista, álbum (índice GIN de PostgreSQL)
- Filtros simples: `decade` (ej. `1980s`), `energy` (rango 0-1), `valence` (rango 0-1)
- Paginación en resultados

### 2.16 Filtros multidimensionales
Endpoint completo: `GET /v1/search?q=&decade=&energy=&valence=&genre=&movie=&aesthetic=&page=&size=`

Filtros combinables definidos en el contexto:
- **Cinematográfico**: `movie` (nombre), `director`, `genre` cinematográfico (vía TMDb)
- **Estético**: `aesthetic` = tags culturales Last.fm (`cyberpunk`, `cottagecore`, `dreamy`)
- **Temporal**: `decade` (ej. `1980s`, `1990s`)
- **Técnico**: `energy`, `valence`, `bpmMin`, `bpmMax` (Spotify audio features)

Implementar con JPA Criteria API o Specifications para queries dinámicas combinables.

Endpoints adicionales:
- `GET /v1/search/suggestions?q=` — autocompletado
- `GET /v1/search/filters` — listar filtros disponibles con valores

### 2.17 Optimización de búsquedas
- Identificar queries lentas habilitando logs SQL de Hibernate en perfil `dev`
- Verificar que los índices del migration V12 están siendo usados (EXPLAIN ANALYZE)
- Caché de resultados de búsqueda frecuentes (15 min TTL)
- Medir tiempo de respuesta antes/después

---

## Grupo F — API REST completa
> Depende de D + E.

| Tarea | Estado |
|---|---|
| 2.18 Configuración de API REST | Pendiente |
| 2.19 Endpoints de búsqueda | Pendiente |
| 2.20 Endpoints de canciones | Pendiente |
| 2.21 Endpoints de playlists | Pendiente |

### 2.18 Configuración de API REST
- Crear `GlobalExceptionHandler` con `@ControllerAdvice`:
  - `404` → recurso no encontrado
  - `400` → validación fallida
  - `429` → rate limit externo propagado
  - `500` → error interno con log
- Estandarizar respuestas de error en JSON
- Configurar CORS para el cliente React Native
- Añadir **SpringDoc / OpenAPI 3** (Swagger UI) al `pom.xml`
- Configurar serialización JSON: fechas ISO-8601, ignorar nulls
- Prefijo base: `/v1/`

### 2.19 Endpoints de búsqueda
```
GET  /v1/search?q=&decade=&energy=&valence=&genre=&movie=&aesthetic=&page=&size=
GET  /v1/search/suggestions?q=
GET  /v1/search/filters
```

### 2.20 Endpoints de canciones
```
GET  /v1/songs/{id}                # detalles completos (con audio features, tags, movies)
GET  /v1/songs/{id}/related        # canciones relacionadas (algoritmo similitud)
GET  /v1/songs/{id}/audio-features # solo características de audio
POST /v1/songs/{id}/play           # registrar reproducción (para IA)
```

### 2.21 Endpoints de playlists
```
GET    /v1/playlists
POST   /v1/playlists
GET    /v1/playlists/{id}
PUT    /v1/playlists/{id}
DELETE /v1/playlists/{id}
POST   /v1/playlists/{id}/songs
DELETE /v1/playlists/{id}/songs/{songId}
PUT    /v1/playlists/{id}/songs/reorder
POST   /v1/playlists/{id}/export/spotify  # exportar a Spotify
```

### Endpoints adicionales (preparar en Fase 2, usados en Fase 4)
```
# Grafo (necesario para la feature principal de Fase 4)
GET /v1/graph?songIds=&mode=cinematographic|temporal|emotional|geographic
GET /v1/graph/expand/{nodeId}

# Sesiones
GET /v1/sessions
GET /v1/sessions/{id}

# Usuarios
GET /v1/users/me/profile
GET /v1/users/me/graph-shared?withUserId=
```

---

## Grupo G — Calidad y cierre
> Depende de todo lo anterior.

| Tarea | Estado |
|---|---|
| 2.22 Tests E2E backend | Pendiente |
| 2.23 Documentación técnica backend | Pendiente |

### 2.22 Tests E2E backend
- Configurar **Testcontainers** con imagen `pgvector/pgvector:pg16` para tests con BD real
- Configurar **WireMock** para simular Spotify, TMDb y Last.fm
- Tests de flujos completos:
  - Búsqueda → detalles canción → canciones relacionadas
  - Crear playlist → añadir canciones → exportar a Spotify
  - Iniciar sesión → navegar nodos → recuperar historial
- Cobertura mínima objetivo: >70% (convención del proyecto)

### 2.23 Documentación técnica backend
- Arquitectura hexagonal: diagrama y descripción de cada capa
- Cómo añadir una nueva integración externa (paso a paso)
- Variables de entorno necesarias (referencia a sección 11 del contexto)
- Estrategias de testing: unitario / integración / E2E
- Solución de problemas frecuentes (rate limits, pgvector en local, etc.)

---

## Notas transversales

### Convenciones del proyecto (del contexto)
- Código en **inglés** (variables, funciones, comentarios)
- Documentación en **español**
- Commits: Conventional Commits (`feat:`, `fix:`, `docs:`, `test:`, `refactor:`)
- Sin lógica de negocio en controladores
- Tests >70% cobertura

### Riesgos a tener en cuenta
| Riesgo | Nivel | Estrategia en el código |
|---|---|---|
| Rate limits APIs externas | Alto | Circuit breaker + caché agresiva + WireMock en tests |
| Inconsistencia metadatos entre APIs | Medio | Algoritmos de normalización en el agregador |
| Caché ineficiente | Medio | TTL diferenciado por tipo, métricas hit/miss |

### Docker local para desarrollo
```yaml
# Imagen PostgreSQL con pgvector (del contexto)
postgres:
  image: pgvector/pgvector:pg16
  environment:
    POSTGRES_DB: ecos_db
    POSTGRES_USER: ecos
    POSTGRES_PASSWORD: ${DB_PASSWORD}
```