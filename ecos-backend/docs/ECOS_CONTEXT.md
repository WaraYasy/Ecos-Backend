# ECOS — Contexto del Proyecto para Claude Code

> Documento de referencia generado desde la Memoria del Proyecto (v2, 23/12/2025).
> Autora: Wara Yasy Pacheco Arandia — Ciclo DAM (Desarrollo de Aplicaciones Multiplataforma)
> Periodo: 25/11/2025 → 08/05/2026

---

## 1. Qué es ECOS

ECOS es una **aplicación móvil de descubrimiento musical inteligente** que conecta canciones a través de múltiples dimensiones culturales, visuales, temporales y técnicas. En lugar de mostrar listas o géneros, el usuario navega un **grafo neuronal interactivo** donde cada canción es un nodo con relaciones hacia otras canciones.

La propuesta de valor central: el usuario no "busca" música, sino que **"viaja"** a través de décadas, estéticas visuales y escenas de películas.

---

## 2. Stack tecnológico

| Capa | Tecnología |
|---|---|
| App móvil | React Native + TypeScript (iOS & Android) |
| Backend principal | Spring Boot (Java) — Arquitectura Hexagonal (Ports & Adapters) |
| Microservicio IA | FastAPI (Python) |
| Base de datos | PostgreSQL + extensión `pgvector` (modelo híbrido relacional + vectorial) |
| Infraestructura | Docker + Docker Compose |
| Control de versiones | GitHub |
| Diseño UI | Figma |
| APIs externas | Spotify Web API, TMDb API, Last.fm API |
| Modelo IA | `sentence-transformers/all-MiniLM-L6-v2` (local, open source) |

---

## 3. Arquitectura del sistema

### 3.1 Visión general

```
┌─────────────────────────────────────────────────────┐
│                 App Móvil (React Native)             │
│  Grafo neuronal · Búsqueda · Reproductor · Playlists│
└────────────────────┬────────────────────────────────┘
                     │ REST API
┌────────────────────▼────────────────────────────────┐
│         Backend Spring Boot (Hexagonal)              │
│  Domain · Application · Infrastructure               │
│  Adaptadores: Spotify · TMDb · Last.fm               │
│  Motor búsqueda multidimensional · Caché · Grafo     │
└──────────┬───────────────────┬───────────────────────┘
           │ PostgreSQL (JPA)  │ HTTP interno
┌──────────▼──────────┐  ┌────▼────────────────────────┐
│ PostgreSQL + pgvector│  │ Microservicio IA (FastAPI)  │
│ Datos estructurados  │  │ Embeddings · Semántica      │
│ Vectores embeddings  │  │ Recomendaciones · Perfiles  │
└─────────────────────┘  └─────────────────────────────┘
```

### 3.2 Arquitectura Hexagonal (Backend Spring Boot)

- **Domain**: Entidades (Song, Artist, Album, User, Playlist, Session), Value Objects (AudioFeatures, GraphNode, Metadata), casos de uso
- **Application**: Servicios de aplicación, puertos de entrada y salida
- **Infrastructure**: Adaptadores JPA, adaptadores HTTP (Spotify, TMDb, Last.fm), controladores REST, caché

### 3.3 Base de datos híbrida

- **Relacional**: canciones, artistas, álbumes, usuarios, playlists, sesiones, metadatos
- **Vectorial** (pgvector): embeddings de audio, embeddings emocionales, embeddings de texto para búsqueda semántica

---

## 4. Funcionalidades principales

### 4.1 Grafo neuronal musical (feature estrella)

- Interfaz principal: red interactiva de nodos (canciones) conectados
- Cada nodo muestra la portada del álbum como miniatura
- Al expandir un nodo: portada ampliada, título, artista, año, características de audio visualizadas, botón de preview 30s
- Desde el nodo expandido: reproducir, añadir a playlist, ver detalles técnicos, explorar relacionadas

**Modos de visualización (universos temáticos):**
| Modo | Descripción |
|---|---|
| Cinematográfico | Agrupa nodos por relación con obras audiovisuales |
| Temporal | Organización cronológica, exploración por décadas |
| Emocional | Posicionamiento por valencia, energía y emoción |
| Geográfico | Agrupación por origen cultural/regional |

- Transiciones animadas entre modos
- El sistema guarda la **ruta** de exploración del usuario (sesión visual navegable)

### 4.2 Búsqueda multidimensional

Filtros combinables:
- **Cinematográfico**: película, director, género cinematográfico (vía TMDb)
- **Estético**: tags culturales como `cyberpunk`, `cottagecore`, `dreamy` (vía Last.fm)
- **Temporal**: décadas específicas
- **Técnico**: BPM, energía, valencia, tempo (vía Spotify audio features)

### 4.3 Búsqueda semántica y simulación

- Motor NLP con embeddings (`sentence-transformers`)
- Consultas abstractas: *"una versión futurista de este artista"*, *"algo que suene a Gasteiz en los 60 pero más electrónico"*
- Búsqueda por simulación: operaciones vectoriales para explorar conceptos cercanos

### 4.4 Integración con APIs externas

| API | Uso |
|---|---|
| **Spotify Web API** | Catálogo musical, metadatos, audio features, portadas, previews 30s, exportar playlists |
| **TMDb API** | Películas/series, directores, géneros cinematográficos para filtros |
| **Last.fm API** | Tags comunitarios (géneros, moods, estéticas) para filtros semánticos |

El backend agrega datos de las tres fuentes en tiempo real con caché inteligente. No hay ingesta manual ni catálogo propio.

### 4.5 Funcionalidades de usuario

- **Playlists**: crear, editar, reordenar (drag & drop), eliminar, exportar a Spotify
- **Sesiones**: rutas de exploración guardadas automáticamente como mapas visuales navegables (≠ playlists estáticas)
- **Perfiles**: huella musical única (energía promedio, riqueza armónica, épocas favoritas, eclecticidad)
- **Grafo compartido**: dos usuarios pueden generar un grafo mezclando sus gustos
- **Recomendaciones personalizadas**: basadas en historial de interacciones, con explicabilidad ("Te recomiendo esto porque...")
- **Recomendaciones crossover**: para dos usuarios combinando perfiles

### 4.6 Reproductor de audio

- Preview Spotify de 30 segundos
- Mini reproductor flotante (siempre visible durante navegación)
- Reproductor expandido (portada grande, barra de progreso interactiva, controles prominentes)
- Control de cola de reproducción

### 4.7 Idiomas

- Español (principal), Inglés (secundario)
- Detección automática según configuración del dispositivo
- Contenido musical en idioma original

---

## 5. Estructura de tareas del proyecto

### Fase 1: Setup y fundamentos (29/12/2025 – 17/01/2026) — 60h
Instalación herramientas, inicialización proyectos Spring Boot + FastAPI + React Native, aprendizaje TypeScript/React Native/PostgreSQL, investigación APIs externas, diseño arquitectura hexagonal, modelo de dominio UML, ADRs, esquema de base de datos ER.

### Fase 2: Backend Spring Boot + APIs (20/01/2026 – 21/02/2026) — 120h
Configuración Spring Boot, migraciones Flyway/Liquibase, testing setup, entidades del dominio, Value Objects, casos de uso, cliente HTTP base, proxy para APIs externas, adaptadores Spotify/TMDb/Last.fm, sistema de agregación, caché, repositorios JPA, búsqueda básica, filtros multidimensionales, optimización, API REST, endpoints búsqueda/canciones/playlists, tests E2E, documentación técnica.

### Fase 3: Frontend React Native (24/02/2026 – 21/03/2026) — 96h
Navegación, estado global, cliente HTTP, design tokens, componentes atómicos, layout, SearchBar, filtros multidimensionales UI, lista de resultados, detalles de canción, reproductor (integración + mini-player + expandido), gestión de playlists, tests E2E mobile.

### Fase 4: Grafo neuronal (24/03/2026 – 11/04/2026) — 72h
Evaluación librerías visualización, mockups Figma, algoritmo similitud canciones, construcción grafo relaciones, posicionamiento dinámico (force-directed), API grafo backend, renderizado nodos/conexiones, gestos (pan & zoom), vista expandida nodo, exploración relaciones, 4 modos de visualización, transiciones animadas.

### Fase 5: Microservicio IA (14/04/2026 – 25/04/2026) — 48h
FastAPI setup, inicialización sentence-transformers, endpoints embeddings, integración PostgreSQL/pgvector, búsqueda semántica, búsqueda por simulación, registro interacciones, generación perfil usuario, recomendaciones personalizadas, recomendaciones crossover, integración con backend principal.

### Fase 6: Testing e integración final (28/04/2026 – 02/05/2026) — 24h
Pruebas integrales, optimización rendimiento, Dockerfiles (Spring Boot + FastAPI + PostgreSQL), Docker Compose, manual de usuario, documentación técnica completa.

### Fase 7: Buffer y contingencia (05/05/2026 – 08/05/2026)
Bugs críticos, optimizaciones adicionales, mejoras UX, preparación presentación.

---

## 6. Estructura de directorios sugerida

```
ecos/
├── docker-compose.yml
├── README.md
│
├── ecos-backend/                    # Spring Boot
│   ├── src/main/java/com/ecos/
│   │   ├── domain/
│   │   │   ├── model/               # Song, Artist, Album, User, Playlist, Session
│   │   │   ├── valueobject/         # AudioFeatures, GraphNode, Metadata, UserProfile
│   │   │   └── port/
│   │   │       ├── in/              # Puertos de entrada (casos de uso)
│   │   │       └── out/             # Puertos de salida (repositorios, APIs externas)
│   │   ├── application/
│   │   │   └── usecase/             # SearchSongs, GetSongDetails, ManagePlaylist, etc.
│   │   └── infrastructure/
│   │       ├── adapter/
│   │       │   ├── in/rest/         # Controladores REST
│   │       │   └── out/
│   │       │       ├── spotify/     # Adaptador Spotify
│   │       │       ├── tmdb/        # Adaptador TMDb
│   │       │       ├── lastfm/      # Adaptador Last.fm
│   │       │       └── persistence/ # Repositorios JPA
│   │       ├── cache/
│   │       └── graph/               # Algoritmo similitud + construcción grafo
│   ├── src/test/
│   └── Dockerfile
│
├── ecos-ia/                         # FastAPI (Python)
│   ├── app/
│   │   ├── main.py
│   │   ├── embeddings/              # sentence-transformers
│   │   ├── search/                  # Búsqueda semántica + simulación
│   │   ├── recommendations/         # Perfiles usuario + recomendaciones
│   │   └── database/                # Conexión PostgreSQL/pgvector
│   ├── requirements.txt
│   └── Dockerfile
│
└── ecos-mobile/                     # React Native + TypeScript
    ├── src/
    │   ├── navigation/
    │   ├── store/                   # Estado global (Redux/Zustand)
    │   ├── api/                     # Cliente HTTP
    │   ├── design/                  # Design tokens, theme
    │   ├── components/
    │   │   ├── atoms/               # Button, Input, Card, Icon
    │   │   ├── molecules/           # SearchBar, FilterPanel, SongCard
    │   │   └── organisms/           # MiniPlayer, ExpandedPlayer, GraphView
    │   ├── screens/
    │   │   ├── Search/
    │   │   ├── SongDetail/
    │   │   ├── Graph/               # Grafo neuronal (feature principal)
    │   │   ├── Playlists/
    │   │   └── Profile/
    │   └── graph/                   # Lógica visualización grafo
    └── package.json
```

---

## 7. Modelo de dominio (entidades principales)

```
Song
  - id, spotifyId, title, duration
  - artist: Artist
  - album: Album
  - audioFeatures: AudioFeatures
  - tags: List<String>          # Last.fm tags
  - movies: List<Movie>         # TMDb vinculaciones
  - embeddings: float[]         # pgvector

AudioFeatures (Value Object)
  - energy, tempo, valence, danceability
  - acousticness, instrumentalness, speechiness
  - loudness, key, mode, timeSignature

Artist
  - id, spotifyId, name, genres, country

Album
  - id, spotifyId, title, year, coverUrl

User
  - id, username
  - profile: UserProfile

UserProfile
  - avgEnergy, harmonicRichness, favoriteEras[]
  - eclecticity, topGenres[], topArtists[]
  - profileEmbedding: float[]

Playlist
  - id, name, description, userId
  - songs: List<Song>
  - spotifyExportId (nullable)

Session (exploration route)
  - id, userId, createdAt
  - nodes: List<GraphNode>       # ruta navegada
  - startSong, endSong

GraphNode (Value Object)
  - songId, x, y                # posición en el grafo
  - connections: List<GraphEdge>
  - similarity: float

Movie
  - id, tmdbId, title, year, director, genres[]
```

---

## 8. API REST — Endpoints principales

### Backend Spring Boot

```
# Búsqueda
GET  /api/v1/search?q=&decade=&energy=&valence=&genre=&movie=&aesthetic=&page=&size=
GET  /api/v1/search/suggestions?q=
GET  /api/v1/search/filters

# Canciones
GET  /api/v1/songs/{id}
GET  /api/v1/songs/{id}/related
GET  /api/v1/songs/{id}/audio-features
POST /api/v1/songs/{id}/play    # registrar reproducción

# Grafo
GET  /api/v1/graph?songIds=&mode=cinematographic|temporal|emotional|geographic
GET  /api/v1/graph/expand/{nodeId}

# Playlists
GET    /api/v1/playlists
POST   /api/v1/playlists
GET    /api/v1/playlists/{id}
PUT    /api/v1/playlists/{id}
DELETE /api/v1/playlists/{id}
POST   /api/v1/playlists/{id}/songs
DELETE /api/v1/playlists/{id}/songs/{songId}
POST   /api/v1/playlists/{id}/export/spotify

# Sesiones
GET  /api/v1/sessions
GET  /api/v1/sessions/{id}

# Usuarios
GET  /api/v1/users/me/profile
GET  /api/v1/users/me/graph-shared?withUserId=
```

### Microservicio IA (FastAPI)

```
POST /embeddings/single          # { text } → { embedding: float[] }
POST /embeddings/batch           # { texts[] } → { embeddings[] }

POST /search/semantic            # { query, limit } → { songs[] }
POST /search/simulate            # { query, baseArtist?, limit } → { songs[] }

POST /interactions               # { userId, songId, action, duration }

GET  /users/{userId}/profile     # perfil musical calculado
GET  /users/{userId}/recommendations
POST /users/shared-recommendations # { userId1, userId2 } → crossover
```

---

## 9. Base de datos — Tablas principales

```sql
-- Canciones y metadatos musicales
songs (id, spotify_id, title, duration_ms, preview_url, album_id, created_at)
artists (id, spotify_id, name, country)
albums (id, spotify_id, title, year, cover_url, artist_id)
song_artists (song_id, artist_id)

-- Características de audio (Spotify)
audio_features (song_id PK, energy, tempo, valence, danceability,
                acousticness, instrumentalness, speechiness, loudness,
                musical_key, mode, time_signature)

-- Tags culturales (Last.fm)
song_tags (song_id, tag, weight)

-- Contexto cinematográfico (TMDb)
movies (id, tmdb_id, title, year, director)
song_movies (song_id, movie_id)

-- Vectores para búsqueda semántica
song_embeddings (song_id PK, embedding vector(384))  -- pgvector
                                                     -- all-MiniLM-L6-v2 → 384 dims

-- Usuarios
users (id, username, email, language, created_at)
user_profiles (user_id PK, avg_energy, harmonic_richness, eclecticity,
               favorite_eras jsonb, top_genres jsonb,
               profile_embedding vector(384))

-- Playlists
playlists (id, user_id, name, description, spotify_export_id, created_at)
playlist_songs (playlist_id, song_id, position)

-- Sesiones de exploración
sessions (id, user_id, created_at, start_song_id)
session_nodes (id, session_id, song_id, x, y, visited_at, sequence_order)
session_edges (id, session_id, from_song_id, to_song_id, similarity)

-- Interacciones (para IA)
user_interactions (id, user_id, song_id, action, duration_ms, created_at)
-- action: 'play' | 'skip' | 'add_playlist' | 'expand_node'

-- Índices clave
CREATE INDEX ON songs USING GIN(to_tsvector('english', title));
CREATE INDEX ON song_embeddings USING ivfflat (embedding vector_cosine_ops);
CREATE INDEX ON user_profiles USING ivfflat (profile_embedding vector_cosine_ops);
```

---

## 10. Configuración Docker

```yaml
# docker-compose.yml (esqueleto)
services:
  postgres:
    image: pgvector/pgvector:pg16
    environment:
      POSTGRES_DB: ecos_db
      POSTGRES_USER: ecos
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ecos"]

  spring-api:
    build: ./ecos-backend
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/ecos_db
      SPOTIFY_CLIENT_ID: ${SPOTIFY_CLIENT_ID}
      SPOTIFY_CLIENT_SECRET: ${SPOTIFY_CLIENT_SECRET}
      TMDB_API_KEY: ${TMDB_API_KEY}
      LASTFM_API_KEY: ${LASTFM_API_KEY}
      IA_SERVICE_URL: http://fastapi-ia:8000
    depends_on:
      postgres:
        condition: service_healthy

  fastapi-ia:
    build: ./ecos-ia
    environment:
      DATABASE_URL: postgresql://ecos:${DB_PASSWORD}@postgres:5432/ecos_db
      MODEL_NAME: all-MiniLM-L6-v2
    depends_on:
      postgres:
        condition: service_healthy

volumes:
  postgres_data:
```

---

## 11. Variables de entorno necesarias

```env
# .env (NO commitear)
DB_PASSWORD=

# Spotify (https://developer.spotify.com)
SPOTIFY_CLIENT_ID=
SPOTIFY_CLIENT_SECRET=

# TMDb (https://www.themoviedb.org/settings/api)
TMDB_API_KEY=

# Last.fm (https://www.last.fm/api/account/create)
LASTFM_API_KEY=
LASTFM_API_SECRET=
```

---

## 12. Algoritmo de similitud entre canciones (para el grafo)

La similitud se calcula combinando múltiples dimensiones ponderadas:

```
similarity(A, B) =
  0.35 * audio_similarity(A.features, B.features)  # coseno sobre vector normalizado
  + 0.25 * semantic_similarity(A.embedding, B.embedding)  # pgvector coseno
  + 0.20 * tag_similarity(A.tags, B.tags)           # Jaccard sobre conjuntos de tags
  + 0.10 * era_proximity(A.year, B.year)             # decaimiento gaussiano
  + 0.10 * movie_overlap(A.movies, B.movies)         # Jaccard sobre películas compartidas
```

Cada nodo del grafo tiene máximo **5-8 conexiones** para mantener legibilidad visual.
El posicionamiento usa un algoritmo de **force-directed layout** (fuerzas de atracción/repulsión).

---

## 13. Búsqueda semántica y simulación (IA)

### Búsqueda semántica
1. Texto de consulta → embedding via `all-MiniLM-L6-v2`
2. Búsqueda de vecinos más cercanos en `song_embeddings` con pgvector
3. Ranking por similitud coseno + boost por popularidad

### Búsqueda por simulación
Consultas como *"versión futurista de X"* o *"sonido de los 80 pero electrónico"*:
1. Identificar canción/artista base → su embedding `v_base`
2. Interpretar modificador semántico → `v_mod` (embedding del concepto)
3. Vector resultado: `v_result = v_base + α * v_mod`  (operación vectorial)
4. Buscar vecinos más cercanos a `v_result`

### Perfil de usuario
Calculado como promedio ponderado de embeddings de canciones interactuadas:
```python
profile_embedding = sum(embedding_i * weight_i) / sum(weight_i)
# weight: play=1.0, add_playlist=1.5, skip=-0.3
```

---

## 14. Convenciones de código

- **Backend**: Clean Architecture estricto, tests >70% cobertura, sin lógica de negocio en controladores
- **Frontend**: TypeScript estricto (`strict: true`), componentes funcionales con hooks, no clases
- **Python/IA**: Type hints en todas las funciones, Pydantic para validación
- **Commits**: Conventional Commits (`feat:`, `fix:`, `docs:`, `test:`, `refactor:`)
- **Idioma código**: inglés (variables, funciones, comentarios)
- **Idioma docs**: español

---

## 15. Riesgos identificados

| Riesgo | Nivel | Mitigación |
|---|---|---|
| Rendimiento grafo en móvil (FPS) | Alto | Renderizado selectivo, evaluar librerías antes de implementar |
| Rate limits / cambios en APIs externas | Alto | Caché agresiva, circuit breaker, mock en tests |
| Calidad de recomendaciones IA | Medio/Alto | Modelo pre-entrenado probado, ajuste fino posterior |
| Inconsistencia metadatos entre APIs | Medio | Algoritmos de normalización y matching |
| Diseño de caché ineficiente | Medio | Políticas TTL por tipo de dato, métricas |
| Curva de aprendizaje React Native | Medio | Fase 1 dedicada a aprendizaje antes de desarrollar |

---

## 16. Presupuesto total del proyecto

| Fase | Total |
|---|---|
| Setup y fundamentos | 792 € |
| Backend Spring Boot + APIs | 1.687 € |
| Frontend React Native | 1.316 € |
| Grafo neuronal | 1.083 € |
| Microservicio IA | 800 € |
| Testing e integración | 267 € |
| Buffer y contingencia | 265 € |
| Planificación y administración | 484 € |
| Presentación | 162,50 € |
| **TOTAL** | **6.856,50 €** |

*Nota: presupuesto académico estimado. La mayoría de herramientas son gratuitas (open source, cuentas developer gratuitas).*

---

*Generado desde: DM_ECOS_2_MEMORIA_V2.pdf — Para uso con Claude Code*
