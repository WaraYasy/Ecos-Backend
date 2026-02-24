# 🗄️ Arquitectura Completa del Proyecto Ecos

## 📋 Índice
1. [Estructura de Archivos y Responsabilidades](#estructura-de-archivos)
2. [Tabla Completa de Clases por Concepto](#tabla-completa-de-clases)
3. [Diagrama de Relaciones entre Entidades](#diagrama-de-relaciones)
4. [Entidades JPA Detalladas](#entidades-detalladas)
5. [Explicación de Relaciones](#explicacion-de-relaciones)
6. [Origen de Datos por Atributo](#origen-de-datos)
7. [Flujos de Datos Completos](#flujos-de-datos)

---

## 1. Estructura de Archivos y Responsabilidades {#estructura-de-archivos}

### 📁 Organización completa del proyecto

```
backend/src/main/java/es/wara/ecos/backend/

📂 domain/                              # DOMINIO - Lógica de negocio pura
│   📂 model/                           # Modelos de dominio (SIN anotaciones técnicas)
│   │   📄 User.java
│   │   📄 Song.java
│   │   📄 AudioFeatures.java
│   │   📄 Tag.java
│   │   📄 Movie.java
│   │   📄 Playlist.java
│   │   📄 ExplorationSession.java
│   │   📄 SessionNode.java
│   │   📄 GraphNode.java               # Calculado para visualización
│   │   📄 MusicRecommendation.java     # Calculado por algoritmo
│   │   📄 MusicProfile.java            # Perfil musical del usuario
│   │
│   📂 port/                            # Interfaces (contratos del dominio)
│       📄 MusicProvider.java           # Interfaz para proveedores de música
│       📄 UserRepository.java          # Interfaz de repositorio (NO JPA)
│       📄 SongRepository.java
│       📄 PlaylistRepository.java
│       📄 SessionRepository.java

📂 application/                         # APLICACIÓN - Casos de uso
│   📂 usecase/                         # Casos de uso (orquestación)
│       📂 user/
│       │   📄 RegisterUserUseCase.java
│       │   📄 GetUserProfileUseCase.java
│       │   📄 UpdateUserPreferencesUseCase.java
│       │   📄 CalculateUserProfileUseCase.java
│       │
│       📂 search/
│       │   📄 SearchSongsUseCase.java
│       │   📄 GetSimilarSongsUseCase.java
│       │
│       📂 playlist/
│       │   📄 CreatePlaylistUseCase.java
│       │   📄 AddSongToPlaylistUseCase.java
│       │   📄 ExportPlaylistToSpotifyUseCase.java
│       │
│       📂 session/
│           📄 CreateExplorationSessionUseCase.java
│           📄 AddNodeToSessionUseCase.java
│           📄 GetSessionHistoryUseCase.java

📂 infrastructure/                      # INFRAESTRUCTURA - Detalles técnicos
│   📂 persistence/                     # Persistencia en base de datos
│   │   📂 entity/                      # Entidades JPA (mapeo a tablas)
│   │   │   📄 UserEntity.java
│   │   │   📄 SongEntity.java
│   │   │   📄 AudioFeaturesEntity.java
│   │   │   📄 TagEntity.java
│   │   │   📄 SongTagEntity.java
│   │   │   📄 SongTagId.java           # @Embeddable (PK compuesta)
│   │   │   📄 MovieEntity.java
│   │   │   📄 MovieSongEntity.java
│   │   │   📄 PlaylistEntity.java
│   │   │   📄 PlaylistSongEntity.java
│   │   │   📄 ExplorationSessionEntity.java
│   │   │   📄 SessionNodeEntity.java
│   │   │   📄 UserInteractionEntity.java
│   │   │
│   │   📂 repository/                  # Implementaciones JPA
│   │   │   📄 UserRepositoryImpl.java
│   │   │   📄 SongRepositoryImpl.java
│   │   │   📄 AudioFeaturesRepositoryImpl.java
│   │   │   📄 TagRepositoryImpl.java
│   │   │   📄 PlaylistRepositoryImpl.java
│   │   │   📄 SessionRepositoryImpl.java
│   │   │   📄 UserInteractionRepositoryImpl.java
│   │   │
│   │   📂 mapper/                      # Conversiones Entity ↔ Domain
│   │       📄 UserMapper.java
│   │       📄 SongMapper.java
│   │       📄 AudioFeaturesMapper.java
│   │       📄 TagMapper.java
│   │       📄 MovieMapper.java
│   │       📄 PlaylistMapper.java
│   │       📄 SessionMapper.java
│   │
│   📂 adapter/                         # Adaptadores a servicios externos
│       📂 spotify/
│       │   📄 SpotifyClient.java
│       │   📄 SpotifyAuthService.java
│       │   📂 dto/                     # DTOs de Spotify API
│       │       📄 SpotifyTrackDTO.java
│       │       📄 SpotifyAudioFeaturesDTO.java
│       │       📄 SpotifySearchResponseDTO.java
│       │       📄 SpotifyAuthTokenDTO.java
│       │       📄 SpotifyPlaylistDTO.java
│       │
│       📂 lastfm/
│       │   📄 LastFmClient.java
│       │   📂 dto/
│       │       📄 LastFmTagDTO.java
│       │       📄 LastFmSimilarTracksDTO.java
│       │       📄 LastFmTrackInfoDTO.java
│       │
│       📂 tmdb/
│           📄 TMDBClient.java
│           📂 dto/
│               📄 TMDBMovieDTO.java
│               📄 TMDBSearchResponseDTO.java
│               📄 TMDBCreditDTO.java

📂 interfaces/                          # INTERFACES - Puntos de entrada
    📂 rest/                            # API REST
        📂 controller/
        │   📄 AuthController.java
        │   📄 UserController.java
        │   📄 SearchController.java
        │   📄 PlaylistController.java
        │   📄 SessionController.java
        │   📄 RecommendationController.java
        │   📄 SpotifyTestController.java    # Para pruebas
        │   📄 LastFmTestController.java     # Para pruebas
        │   📄 TMDBTestController.java       # Para pruebas
        │
        📂 dto/                         # DTOs de la API REST
            📂 request/                 # Requests (entrada)
            │   📄 RegisterUserDTO.java
            │   📄 LoginRequestDTO.java
            │   📄 CreatePlaylistDTO.java
            │   📄 UpdatePlaylistDTO.java
            │   📄 AddSongToPlaylistDTO.java
            │   📄 CreateSessionDTO.java
            │   📄 SearchRequestDTO.java
            │
            📂 response/                # Responses (salida)
                📄 UserProfileDTO.java
                📄 AuthResponseDTO.java
                📄 SongDTO.java
                📄 AudioFeaturesDTO.java
                📄 TagDTO.java
                📄 MovieDTO.java
                📄 PlaylistDTO.java
                📄 PlaylistDetailDTO.java
                📄 ExplorationSessionDTO.java
                📄 SessionDetailDTO.java
                📄 GraphNodeDTO.java
                📄 RecommendationDTO.java
                📄 SearchResponseDTO.java
                📄 ErrorResponseDTO.java
```

### 🎯 Responsabilidades por capa

| Capa | Responsabilidad | NO debe conocer |
|------|-----------------|-----------------|
| **Domain** | Lógica de negocio pura, reglas, validaciones | Base de datos, HTTP, APIs externas |
| **Application** | Orquestación de casos de uso | Detalles de implementación |
| **Infrastructure** | Persistencia, APIs externas, frameworks | Lógica de negocio |
| **Interfaces** | HTTP, REST, validaciones de entrada | Lógica de negocio directa |

---

## 2. Tabla Completa de Clases por Concepto {#tabla-completa-de-clases}

### 👤 **USER (Usuario)**

| Nombre | Tipo | Ubicación | Propósito | ¿Se persiste? |
|--------|------|-----------|-----------|---------------|
| **User** | Domain Model | `domain/model/User.java` | Lógica de negocio de usuario | ❌ No |
| **UserEntity** | JPA Entity | `infrastructure/persistence/entity/UserEntity.java` | Persistencia en BD | ✅ Sí (tabla `users`) |
| **UserMapper** | Mapper | `infrastructure/persistence/mapper/UserMapper.java` | Conversión Entity ↔ Domain ↔ DTO | ❌ No |
| **UserRepository** | Port (Interface) | `domain/port/UserRepository.java` | Contrato de repositorio | ❌ No |
| **UserRepositoryImpl** | Repository | `infrastructure/persistence/repository/UserRepositoryImpl.java` | Implementación JPA | ❌ No |
| **RegisterUserDTO** | DTO Request | `interfaces/rest/dto/request/RegisterUserDTO.java` | Datos de registro | ❌ No |
| **LoginRequestDTO** | DTO Request | `interfaces/rest/dto/request/LoginRequestDTO.java` | Datos de login | ❌ No |
| **UserProfileDTO** | DTO Response | `interfaces/rest/dto/response/UserProfileDTO.java` | Perfil para frontend | ❌ No |
| **AuthResponseDTO** | DTO Response | `interfaces/rest/dto/response/AuthResponseDTO.java` | Respuesta de autenticación | ❌ No |
| **MusicProfile** | Domain Model | `domain/model/MusicProfile.java` | Perfil musical calculado | ❌ No |
| **RegisterUserUseCase** | Use Case | `application/usecase/user/RegisterUserUseCase.java` | Lógica de registro | ❌ No |
| **GetUserProfileUseCase** | Use Case | `application/usecase/user/GetUserProfileUseCase.java` | Obtener perfil | ❌ No |
| **AuthController** | Controller | `interfaces/rest/controller/AuthController.java` | Endpoints de auth | ❌ No |
| **UserController** | Controller | `interfaces/rest/controller/UserController.java` | Endpoints de usuario | ❌ No |

---

### 🎵 **SONG (Canción)**

| Nombre | Tipo | Ubicación | Propósito | ¿Se persiste? |
|--------|------|-----------|-----------|---------------|
| **Song** | Domain Model | `domain/model/Song.java` | Lógica de negocio de canción | ❌ No |
| **SongEntity** | JPA Entity | `infrastructure/persistence/entity/SongEntity.java` | Persistencia en BD | ✅ Sí (tabla `songs`) |
| **SongMapper** | Mapper | `infrastructure/persistence/mapper/SongMapper.java` | Conversión Entity ↔ Domain ↔ DTO | ❌ No |
| **SongRepository** | Port (Interface) | `domain/port/SongRepository.java` | Contrato de repositorio | ❌ No |
| **SongRepositoryImpl** | Repository | `infrastructure/persistence/repository/SongRepositoryImpl.java` | Implementación JPA | ❌ No |
| **SongDTO** | DTO Response | `interfaces/rest/dto/response/SongDTO.java` | Canción para frontend | ❌ No |
| **SpotifyTrackDTO** | DTO External | `infrastructure/adapter/spotify/dto/SpotifyTrackDTO.java` | Respuesta de Spotify API | ❌ No (temporal) |
| **SpotifyClient** | Adapter | `infrastructure/adapter/spotify/SpotifyClient.java` | Cliente de Spotify API | ❌ No |
| **SearchSongsUseCase** | Use Case | `application/usecase/search/SearchSongsUseCase.java` | Búsqueda de canciones | ❌ No |
| **SearchController** | Controller | `interfaces/rest/controller/SearchController.java` | Endpoints de búsqueda | ❌ No |

---

### 🎧 **AUDIO FEATURES (Características de Audio)**

| Nombre | Tipo | Ubicación | Propósito | ¿Se persiste? |
|--------|------|-----------|-----------|---------------|
| **AudioFeatures** | Domain Model | `domain/model/AudioFeatures.java` | Lógica de características | ❌ No |
| **AudioFeaturesEntity** | JPA Entity | `infrastructure/persistence/entity/AudioFeaturesEntity.java` | Persistencia en BD | ✅ Sí (tabla `audio_features`) |
| **AudioFeaturesMapper** | Mapper | `infrastructure/persistence/mapper/AudioFeaturesMapper.java` | Conversión Entity ↔ Domain | ❌ No |
| **AudioFeaturesRepositoryImpl** | Repository | `infrastructure/persistence/repository/AudioFeaturesRepositoryImpl.java` | Implementación JPA | ❌ No |
| **AudioFeaturesDTO** | DTO Response | `interfaces/rest/dto/response/AudioFeaturesDTO.java` | Features para frontend | ❌ No |
| **SpotifyAudioFeaturesDTO** | DTO External | `infrastructure/adapter/spotify/dto/SpotifyAudioFeaturesDTO.java` | Respuesta de Spotify | ❌ No (temporal) |

---

### 🏷️ **TAG (Etiqueta Cultural)**

| Nombre | Tipo | Ubicación | Propósito | ¿Se persiste? |
|--------|------|-----------|-----------|---------------|
| **Tag** | Domain Model | `domain/model/Tag.java` | Lógica de tag | ❌ No |
| **TagEntity** | JPA Entity | `infrastructure/persistence/entity/TagEntity.java` | Persistencia en BD | ✅ Sí (tabla `tags`) |
| **TagMapper** | Mapper | `infrastructure/persistence/mapper/TagMapper.java` | Conversión Entity ↔ Domain | ❌ No |
| **TagRepositoryImpl** | Repository | `infrastructure/persistence/repository/TagRepositoryImpl.java` | Implementación JPA | ❌ No |
| **TagDTO** | DTO Response | `interfaces/rest/dto/response/TagDTO.java` | Tag para frontend | ❌ No |
| **LastFmTagDTO** | DTO External | `infrastructure/adapter/lastfm/dto/LastFmTagDTO.java` | Respuesta de Last.fm | ❌ No (temporal) |
| **LastFmClient** | Adapter | `infrastructure/adapter/lastfm/LastFmClient.java` | Cliente de Last.fm API | ❌ No |

---

### 🔗 **SONG-TAG (Relación Canción-Tag)**

| Nombre | Tipo | Ubicación | Propósito | ¿Se persiste? |
|--------|------|-----------|-----------|---------------|
| **SongTagEntity** | JPA Entity | `infrastructure/persistence/entity/SongTagEntity.java` | Relación N:M | ✅ Sí (tabla `song_tags`) |
| **SongTagId** | Embeddable | `infrastructure/persistence/entity/SongTagId.java` | PK compuesta | ✅ Sí (parte de entity) |

---

### 🎬 **MOVIE (Película/Serie)**

| Nombre | Tipo | Ubicación | Propósito | ¿Se persiste? |
|--------|------|-----------|-----------|---------------|
| **Movie** | Domain Model | `domain/model/Movie.java` | Lógica de película | ❌ No |
| **MovieEntity** | JPA Entity | `infrastructure/persistence/entity/MovieEntity.java` | Persistencia en BD | ✅ Sí (tabla `movies`) |
| **MovieMapper** | Mapper | `infrastructure/persistence/mapper/MovieMapper.java` | Conversión Entity ↔ Domain | ❌ No |
| **MovieDTO** | DTO Response | `interfaces/rest/dto/response/MovieDTO.java` | Película para frontend | ❌ No |
| **TMDBMovieDTO** | DTO External | `infrastructure/adapter/tmdb/dto/TMDBMovieDTO.java` | Respuesta de TMDB | ❌ No (temporal) |
| **TMDBClient** | Adapter | `infrastructure/adapter/tmdb/TMDBClient.java` | Cliente de TMDB API | ❌ No |

---

### 🔗 **MOVIE-SONG (Relación Película-Canción)**

| Nombre | Tipo | Ubicación | Propósito | ¿Se persiste? |
|--------|------|-----------|-----------|---------------|
| **MovieSongEntity** | JPA Entity | `infrastructure/persistence/entity/MovieSongEntity.java` | Relación N:M (soundtracks) | ✅ Sí (tabla `movie_songs`) |

---

### 📝 **PLAYLIST (Playlist de Usuario)**

| Nombre | Tipo | Ubicación | Propósito | ¿Se persiste? |
|--------|------|-----------|-----------|---------------|
| **Playlist** | Domain Model | `domain/model/Playlist.java` | Lógica de playlist | ❌ No |
| **PlaylistEntity** | JPA Entity | `infrastructure/persistence/entity/PlaylistEntity.java` | Persistencia en BD | ✅ Sí (tabla `playlists`) |
| **PlaylistMapper** | Mapper | `infrastructure/persistence/mapper/PlaylistMapper.java` | Conversión Entity ↔ Domain | ❌ No |
| **PlaylistRepository** | Port (Interface) | `domain/port/PlaylistRepository.java` | Contrato de repositorio | ❌ No |
| **PlaylistRepositoryImpl** | Repository | `infrastructure/persistence/repository/PlaylistRepositoryImpl.java` | Implementación JPA | ❌ No |
| **CreatePlaylistDTO** | DTO Request | `interfaces/rest/dto/request/CreatePlaylistDTO.java` | Crear playlist | ❌ No |
| **UpdatePlaylistDTO** | DTO Request | `interfaces/rest/dto/request/UpdatePlaylistDTO.java` | Actualizar playlist | ❌ No |
| **PlaylistDTO** | DTO Response | `interfaces/rest/dto/response/PlaylistDTO.java` | Playlist básica | ❌ No |
| **PlaylistDetailDTO** | DTO Response | `interfaces/rest/dto/response/PlaylistDetailDTO.java` | Playlist con canciones | ❌ No |
| **CreatePlaylistUseCase** | Use Case | `application/usecase/playlist/CreatePlaylistUseCase.java` | Crear playlist | ❌ No |
| **ExportPlaylistToSpotifyUseCase** | Use Case | `application/usecase/playlist/ExportPlaylistToSpotifyUseCase.java` | Exportar a Spotify | ❌ No |
| **PlaylistController** | Controller | `interfaces/rest/controller/PlaylistController.java` | Endpoints de playlist | ❌ No |

---

### 🔗 **PLAYLIST-SONG (Canciones en Playlist)**

| Nombre | Tipo | Ubicación | Propósito | ¿Se persiste? |
|--------|------|-----------|-----------|---------------|
| **PlaylistSongEntity** | JPA Entity | `infrastructure/persistence/entity/PlaylistSongEntity.java` | Relación N:M con orden | ✅ Sí (tabla `playlist_songs`) |

---

### 🗺️ **EXPLORATION SESSION (Sesión del Grafo Neuronal)**

| Nombre | Tipo | Ubicación | Propósito | ¿Se persiste? |
|--------|------|-----------|-----------|---------------|
| **ExplorationSession** | Domain Model | `domain/model/ExplorationSession.java` | Lógica de sesión | ❌ No |
| **ExplorationSessionEntity** | JPA Entity | `infrastructure/persistence/entity/ExplorationSessionEntity.java` | Persistencia en BD | ✅ Sí (tabla `exploration_sessions`) |
| **SessionMapper** | Mapper | `infrastructure/persistence/mapper/SessionMapper.java` | Conversión Entity ↔ Domain | ❌ No |
| **SessionRepository** | Port (Interface) | `domain/port/SessionRepository.java` | Contrato de repositorio | ❌ No |
| **SessionRepositoryImpl** | Repository | `infrastructure/persistence/repository/SessionRepositoryImpl.java` | Implementación JPA | ❌ No |
| **CreateSessionDTO** | DTO Request | `interfaces/rest/dto/request/CreateSessionDTO.java` | Crear sesión | ❌ No |
| **ExplorationSessionDTO** | DTO Response | `interfaces/rest/dto/response/ExplorationSessionDTO.java` | Sesión básica | ❌ No |
| **SessionDetailDTO** | DTO Response | `interfaces/rest/dto/response/SessionDetailDTO.java` | Sesión con nodos | ❌ No |
| **CreateExplorationSessionUseCase** | Use Case | `application/usecase/session/CreateExplorationSessionUseCase.java` | Crear sesión | ❌ No |
| **SessionController** | Controller | `interfaces/rest/controller/SessionController.java` | Endpoints de sesión | ❌ No |

---

### 🔵 **SESSION NODE (Nodos del Grafo)**

| Nombre | Tipo | Ubicación | Propósito | ¿Se persiste? |
|--------|------|-----------|-----------|---------------|
| **SessionNode** | Domain Model | `domain/model/SessionNode.java` | Lógica de nodo | ❌ No |
| **SessionNodeEntity** | JPA Entity | `infrastructure/persistence/entity/SessionNodeEntity.java` | Persistencia en BD | ✅ Sí (tabla `session_nodes`) |
| **GraphNode** | Domain Model | `domain/model/GraphNode.java` | Nodo calculado para visualización | ❌ No (calculado) |
| **GraphNodeDTO** | DTO Response | `interfaces/rest/dto/response/GraphNodeDTO.java` | Nodo para renderizar | ❌ No |

---

### 📊 **USER INTERACTION (Interacciones Usuario-Canción)**

| Nombre | Tipo | Ubicación | Propósito | ¿Se persiste? |
|--------|------|-----------|-----------|---------------|
| **UserInteractionEntity** | JPA Entity | `infrastructure/persistence/entity/UserInteractionEntity.java` | Analytics de interacciones | ✅ Sí (tabla `user_interactions`) |
| **UserInteractionRepositoryImpl** | Repository | `infrastructure/persistence/repository/UserInteractionRepositoryImpl.java` | Implementación JPA | ❌ No |

---

### 🎯 **RECOMMENDATION (Recomendaciones)**

| Nombre | Tipo | Ubicación | Propósito | ¿Se persiste? |
|--------|------|-----------|-----------|---------------|
| **MusicRecommendation** | Domain Model | `domain/model/MusicRecommendation.java` | Recomendación calculada | ❌ No (calculado) |
| **RecommendationDTO** | DTO Response | `interfaces/rest/dto/response/RecommendationDTO.java` | Recomendación para frontend | ❌ No |
| **RecommendationController** | Controller | `interfaces/rest/controller/RecommendationController.java` | Endpoints de recomendaciones | ❌ No |

---

### 🔍 **SEARCH (Búsqueda)**

| Nombre | Tipo | Ubicación | Propósito | ¿Se persiste? |
|--------|------|-----------|-----------|---------------|
| **SearchRequestDTO** | DTO Request | `interfaces/rest/dto/request/SearchRequestDTO.java` | Request de búsqueda | ❌ No |
| **SearchResponseDTO** | DTO Response | `interfaces/rest/dto/response/SearchResponseDTO.java` | Resultado de búsqueda | ❌ No |
| **SpotifySearchResponseDTO** | DTO External | `infrastructure/adapter/spotify/dto/SpotifySearchResponseDTO.java` | Respuesta de Spotify | ❌ No (temporal) |

---

### 🔧 **UTILIDADES Y OTROS**

| Nombre | Tipo | Ubicación | Propósito | ¿Se persiste? |
|--------|------|-----------|-----------|---------------|
| **ErrorResponseDTO** | DTO Response | `interfaces/rest/dto/response/ErrorResponseDTO.java` | Errores estandarizados | ❌ No |
| **MusicProvider** | Port (Interface) | `domain/port/MusicProvider.java` | Contrato proveedor música | ❌ No |
| **SpotifyAuthService** | Service | `infrastructure/adapter/spotify/SpotifyAuthService.java` | OAuth con Spotify | ❌ No |
| **SpotifyAuthTokenDTO** | DTO External | `infrastructure/adapter/spotify/dto/SpotifyAuthTokenDTO.java` | Token de Spotify | ❌ No (temporal) |

---

## 📊 Resumen Estadístico

### Por tipo de objeto:

| Tipo | Cantidad | Propósito |
|------|----------|-----------|
| **Domain Models** | 13 | Lógica de negocio pura |
| **JPA Entities** | 12 | Persistencia en BD |
| **Mappers** | 7 | Conversión entre capas |
| **Ports (Interfaces)** | 5 | Contratos de dominio |
| **Repositories (Impl)** | 7 | Acceso a datos |
| **Use Cases** | ~15 | Casos de uso |
| **Controllers** | 7 | Endpoints REST |
| **DTOs Request** | 7 | Entrada de datos |
| **DTOs Response** | 14 | Salida de datos |
| **DTOs External** | 10 | APIs externas |
| **Adapters** | 3 | Clientes de APIs |
| **TOTAL** | **~100 clases** | - |

### Por capa:

| Capa | Archivos | % del total |
|------|----------|-------------|
| **Domain** | 18 | 18% |
| **Application** | 15 | 15% |
| **Infrastructure** | 39 | 39% |
| **Interfaces** | 28 | 28% |

---

## 1. Diagrama de Relaciones {#diagrama-de-relaciones}

```
┌─────────────────┐
│   UserEntity    │
└────────┬────────┘
         │
         │ 1:N
         ├──────────────────────┐
         │                      │
         │                      │
    ┌────▼─────────┐    ┌──────▼──────────────┐
    │ PlaylistEntity│    │ExplorationSession  │
    └────┬──────────┘    │     Entity         │
         │               └──────┬──────────────┘
         │ N:M                  │
         │                      │ 1:N
    ┌────▼──────────┐    ┌──────▼──────────────┐
    │PlaylistSong   │    │ SessionNodeEntity   │
    │   Entity      │    └──────┬──────────────┘
    └────┬──────────┘           │
         │                      │
         │                      │
         │              ┌───────▼────────┐
         └──────────────►  SongEntity    │◄──────────┐
                        └───────┬────────┘           │
                                │                    │
                    ┌───────────┼────────────┐       │
                    │           │            │       │
                1:1 │       N:M │        N:M │       │
                    │           │            │       │
            ┌───────▼──┐  ┌─────▼─────┐ ┌───▼──────┐│
            │AudioFeat.│  │SongTag    │ │MovieSong ││
            │ Entity   │  │Entity     │ │Entity    ││
            └──────────┘  └─────┬─────┘ └───┬──────┘│
                                │             │      │
                                │ N:1     N:1 │      │
                          ┌─────▼─────┐ ┌─────▼──────▼──┐
                          │TagEntity  │ │ MovieEntity   │
                          └───────────┘ └───────────────┘

┌─────────────────────────────────────────────────┐
│         UserInteractionEntity                   │
│  (Relación User ←→ Song para analytics)        │
└─────────────────────────────────────────────────┘
```

---

## 2. Entidades Detalladas {#entidades-detalladas}

### **1. UserEntity**

**Propósito:** Usuarios de la aplicación Ecos

```java
@Entity
@Table(name = "users")
public class UserEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                    // Auto-generado
    
    private String username;            // Asignado (registro)
    private String email;               // Asignado (registro)
    private String passwordHash;        // Asignado (registro, hasheado)
    private String spotifyUserId;       // Obtenido de Spotify OAuth
    
    // Perfil musical - CALCULADO por sistema
    private Double avgEnergy;           // Calculado: promedio de canciones escuchadas
    private Double avgValence;          // Calculado: promedio de canciones escuchadas
    private Double avgTempo;            // Calculado: promedio de BPM preferidos
    private Double ecleticismScore;     // Calculado: varianza en gustos musicales
    
    private LocalDateTime createdAt;    // Auto: timestamp registro
    private LocalDateTime updatedAt;    // Auto: timestamp última modificación
    
    // Relaciones
    @OneToMany(mappedBy = "user")
    private List<PlaylistEntity> playlists;
    
    @OneToMany(mappedBy = "user")
    private List<ExplorationSessionEntity> sessions;
    
    @OneToMany(mappedBy = "user")
    private List<UserInteractionEntity> interactions;
}
```

**Origen de datos:**
| Atributo | Origen | Tipo |
|----------|--------|------|
| id | Base de datos | Auto-generado |
| username | Usuario (frontend) | Asignado |
| email | Usuario (frontend) | Asignado |
| passwordHash | Usuario + bcrypt | Procesado |
| spotifyUserId | Spotify OAuth | API externa |
| avgEnergy | Sistema (cálculo periódico) | Calculado |
| avgValence | Sistema (cálculo periódico) | Calculado |
| avgTempo | Sistema (cálculo periódico) | Calculado |
| ecleticismScore | Sistema (algoritmo ML) | Calculado |
| createdAt | Sistema | Auto |
| updatedAt | Sistema | Auto |

**Cálculo de perfil musical:**
```java
// Ejecutado periódicamente (cron job)
avgEnergy = AVG(audio_features.energy) 
            FROM user_interactions 
            WHERE user_id = X AND played_count > 0

ecleticismScore = STDDEV(genres, moods, eras) // Cuanto más variado, más alto
```

---

### **2. SongEntity**

**Propósito:** Catálogo de canciones exploradas (caché de Spotify)

```java
@Entity
@Table(name = "songs")
public class SongEntity {
    
    @Id
    private String id;                  // Obtenido: Spotify Track ID
    
    private String title;               // Obtenido: Spotify API
    private String artist;              // Obtenido: Spotify API
    private String album;               // Obtenido: Spotify API
    private Integer releaseYear;        // Obtenido: Spotify API (parsed de release_date)
    private String coverImageUrl;       // Obtenido: Spotify API (album.images[0].url)
    private String previewUrl;          // Obtenido: Spotify API (preview_url)
    private Integer durationMs;         // Obtenido: Spotify API
    private Integer popularity;         // Obtenido: Spotify API (0-100)
    private Boolean explicit;           // Obtenido: Spotify API
    
    // Metadata interna de Ecos
    private Integer explorationCount;   // Calculado: contador interno
    
    private LocalDateTime createdAt;    // Auto: primera vez guardada
    private LocalDateTime updatedAt;    // Auto: última actualización
    
    // Relaciones
    @OneToOne(mappedBy = "song")
    private AudioFeaturesEntity audioFeatures;
    
    @OneToMany(mappedBy = "song")
    private List<SongTagEntity> songTags;
    
    @OneToMany(mappedBy = "song")
    private List<MovieSongEntity> movieSongs;
    
    @OneToMany(mappedBy = "song")
    private List<PlaylistSongEntity> playlistSongs;
}
```

**Origen de datos:**
| Atributo | Origen | API Call |
|----------|--------|----------|
| id | Spotify | `GET /v1/tracks/{id}` → response.id |
| title | Spotify | `GET /v1/tracks/{id}` → response.name |
| artist | Spotify | `GET /v1/tracks/{id}` → response.artists[0].name |
| album | Spotify | `GET /v1/tracks/{id}` → response.album.name |
| releaseYear | Spotify | `GET /v1/tracks/{id}` → parse(response.album.release_date) |
| coverImageUrl | Spotify | `GET /v1/tracks/{id}` → response.album.images[0].url |
| previewUrl | Spotify | `GET /v1/tracks/{id}` → response.preview_url |
| durationMs | Spotify | `GET /v1/tracks/{id}` → response.duration_ms |
| popularity | Spotify | `GET /v1/tracks/{id}` → response.popularity |
| explicit | Spotify | `GET /v1/tracks/{id}` → response.explicit |
| explorationCount | Sistema interno | Incrementa cada vez que se explora |
| createdAt | Sistema | Auto |
| updatedAt | Sistema | Auto |

**Ejemplo de llamada Spotify:**
```bash
GET https://api.spotify.com/v1/tracks/3n3Ppam7vgaVa1iaRUc9Lp
Authorization: Bearer {token}

Response:
{
  "id": "3n3Ppam7vgaVa1iaRUc9Lp",
  "name": "Bohemian Rhapsody",
  "artists": [{"name": "Queen"}],
  "album": {
    "name": "A Night at the Opera",
    "release_date": "1975-11-21",
    "images": [{"url": "https://..."}]
  },
  "duration_ms": 354320,
  "popularity": 89,
  "explicit": false,
  "preview_url": "https://p.scdn.co/..."
}
```

---

### **3. AudioFeaturesEntity**

**Propósito:** Características técnicas de audio (para el grafo neuronal)

```java
@Entity
@Table(name = "audio_features")
public class AudioFeaturesEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                    // Auto-generado
    
    @OneToOne
    @JoinColumn(name = "song_id")
    private SongEntity song;            // FK a SongEntity
    
    // TODAS obtenidas de Spotify Audio Features API
    private Double danceability;        // 0.0-1.0
    private Double energy;              // 0.0-1.0
    private Double valence;             // 0.0-1.0 (positividad)
    private Double acousticness;        // 0.0-1.0
    private Double instrumentalness;    // 0.0-1.0
    private Double liveness;            // 0.0-1.0
    private Double speechiness;         // 0.0-1.0
    
    private Float tempo;                // BPM (ej: 120.5)
    private Integer keyNote;            // 0-11 (C, C#, D, ..., B)
    private Integer mode;               // 0=menor, 1=mayor
    private Integer timeSignature;      // ej: 4 para 4/4
    private Float loudness;             // dB (típicamente -60 a 0)
}
```

**Origen de datos:**
| Atributo | Origen | API Call |
|----------|--------|----------|
| TODOS | Spotify Audio Features | `GET /v1/audio-features/{id}` |

**Ejemplo de llamada:**
```bash
GET https://api.spotify.com/v1/audio-features/3n3Ppam7vgaVa1iaRUc9Lp

Response:
{
  "danceability": 0.278,
  "energy": 0.424,
  "valence": 0.282,
  "acousticness": 0.345,
  "instrumentalness": 0.00387,
  "liveness": 0.204,
  "speechiness": 0.0498,
  "tempo": 144.299,
  "key": 11,  // B
  "mode": 0,  // menor
  "time_signature": 4,
  "loudness": -10.013
}
```

**Uso en el grafo neuronal:**
```java
// Calcular similitud entre dos canciones
double similarity = calculateSimilarity(
    song1.audioFeatures.energy, song2.audioFeatures.energy,
    song1.audioFeatures.valence, song2.audioFeatures.valence,
    song1.audioFeatures.tempo, song2.audioFeatures.tempo
);
```

---

### **4. TagEntity**

**Propósito:** Etiquetas culturales y géneros (de Last.fm)

```java
@Entity
@Table(name = "tags")
public class TagEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                    // Auto-generado
    
    private String name;                // Obtenido: Last.fm (ej: "cyberpunk")
    private String category;            // Asignado: categorización interna
    
    private LocalDateTime createdAt;    // Auto
    
    // Relaciones
    @OneToMany(mappedBy = "tag")
    private List<SongTagEntity> songTags;
}
```

**Origen de datos:**
| Atributo | Origen | Fuente |
|----------|--------|--------|
| name | Last.fm API | `track.getTopTags` |
| category | Sistema (clasificación) | Algoritmo interno |

**Ejemplo de llamada Last.fm:**
```bash
GET http://ws.audioscrobbler.com/2.0/
  ?method=track.getTopTags
  &artist=Queen
  &track=Bohemian Rhapsody
  &api_key={key}
  &format=json

Response:
{
  "toptags": {
    "tag": [
      {"name": "classic rock", "count": 100},
      {"name": "epic", "count": 87},
      {"name": "70s", "count": 65},
      {"name": "progressive rock", "count": 54}
    ]
  }
}
```

**Categorización automática:**
```java
String category = categorizeTag(tagName);

// Algoritmo interno
if (tagName.matches("\\d{2,4}s")) return "era";          // "70s", "1970s"
if (tagName.endsWith("core")) return "aesthetic";        // "cottagecore"
if (isGenre(tagName)) return "genre";                    // "rock", "jazz"
if (isEmotion(tagName)) return "mood";                   // "melancholic"
```

---

### **5. SongTagEntity**

**Propósito:** Relación muchos-a-muchos entre canciones y tags

```java
@Entity
@Table(name = "song_tags")
public class SongTagEntity {
    
    @EmbeddedId
    private SongTagId id;               // PK compuesta
    
    @ManyToOne
    @MapsId("songId")
    private SongEntity song;
    
    @ManyToOne
    @MapsId("tagId")
    private TagEntity tag;
    
    private Integer relevance;          // Obtenido: Last.fm "count"
}

@Embeddable
public class SongTagId implements Serializable {
    private String songId;
    private Long tagId;
}
```

**Origen de datos:**
| Atributo | Origen | Fuente |
|----------|--------|--------|
| relevance | Last.fm | Convertido de "count" a escala 0-100 |

---

### **6. MovieEntity**

**Propósito:** Películas/Series relacionadas con canciones

```java
@Entity
@Table(name = "movies")
public class MovieEntity {
    
    @Id
    private Long id;                    // Obtenido: TMDB Movie ID
    
    private String title;               // Obtenido: TMDB
    private String originalTitle;       // Obtenido: TMDB
    private Integer releaseYear;        // Obtenido: TMDB (parsed)
    private String director;            // Obtenido: TMDB credits
    private String genre;               // Obtenido: TMDB
    private String posterUrl;           // Obtenido: TMDB
    private String backdropUrl;         // Obtenido: TMDB
    private String type;                // Asignado: "movie" o "tv_show"
    
    private LocalDateTime createdAt;    // Auto
    
    // Relaciones
    @OneToMany(mappedBy = "movie")
    private List<MovieSongEntity> movieSongs;
}
```

**Origen de datos:**
| Atributo | Origen | API Call |
|----------|--------|----------|
| id | TMDB | `GET /search/movie` → response.results[0].id |
| title | TMDB | `GET /movie/{id}` → response.title |
| originalTitle | TMDB | `GET /movie/{id}` → response.original_title |
| releaseYear | TMDB | parse(`GET /movie/{id}` → response.release_date) |
| director | TMDB | `GET /movie/{id}/credits` → crew[job="Director"].name |
| genre | TMDB | `GET /movie/{id}` → response.genres[0].name |
| posterUrl | TMDB | `GET /movie/{id}` → response.poster_path |
| backdropUrl | TMDB | `GET /movie/{id}` → response.backdrop_path |
| type | Sistema | Dependiendo del endpoint usado |

**Ejemplo de llamada TMDB:**
```bash
GET https://api.themoviedb.org/3/movie/27205?api_key={key}

Response:
{
  "id": 27205,
  "title": "Inception",
  "original_title": "Inception",
  "release_date": "2010-07-16",
  "genres": [{"name": "Action"}, {"name": "Sci-Fi"}],
  "poster_path": "/qmDpIHrmpJINaRKAfWQfftjCdyi.jpg",
  "backdrop_path": "/s3TBrRGB..."
}
```

---

### **7. MovieSongEntity**

**Propósito:** Relación entre películas y canciones (soundtracks)

```java
@Entity
@Table(name = "movie_songs")
public class MovieSongEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                    // Auto-generado
    
    @ManyToOne
    @JoinColumn(name = "movie_id")
    private MovieEntity movie;
    
    @ManyToOne
    @JoinColumn(name = "song_id")
    private SongEntity song;
    
    private String sceneDescription;    // Asignado: descripción manual/crowdsourced
    private String sceneTimestamp;      // Asignado: "01:23:45"
}
```

**Origen de datos:**
| Atributo | Origen | Tipo |
|----------|--------|------|
| sceneDescription | Usuario/comunidad | Crowdsourced |
| sceneTimestamp | Usuario/comunidad | Crowdsourced |

**Nota:** Esta información NO viene de APIs. Opciones:
1. Usuarios la añaden manualmente
2. Se importa de bases de datos de soundtracks
3. Se obtiene mediante scraping (con precaución legal)

---

### **8. PlaylistEntity**

**Propósito:** Playlists creadas por usuarios

```java
@Entity
@Table(name = "playlists")
public class PlaylistEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                    // Auto-generado
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;
    
    private String name;                // Asignado: usuario
    private String description;         // Asignado: usuario
    private Boolean isPublic;           // Asignado: usuario (default false)
    private String coverImageUrl;       // Asignado: usuario (opcional)
    
    private String spotifyPlaylistId;   // Obtenido: al exportar a Spotify
    
    private LocalDateTime createdAt;    // Auto
    private LocalDateTime updatedAt;    // Auto
    
    // Relaciones
    @OneToMany(mappedBy = "playlist")
    private List<PlaylistSongEntity> songs;
}
```

**Origen de datos:**
| Atributo | Origen | Tipo |
|----------|--------|------|
| name | Usuario (frontend) | Asignado |
| description | Usuario (frontend) | Asignado |
| isPublic | Usuario (frontend) | Asignado |
| coverImageUrl | Usuario (frontend/upload) | Asignado |
| spotifyPlaylistId | Spotify API (export) | API externa |

**Exportación a Spotify:**
```bash
# Cuando usuario exporta playlist
POST https://api.spotify.com/v1/users/{user_id}/playlists
{
  "name": "Mi Playlist de Ecos",
  "description": "Creada en Ecos",
  "public": false
}

Response:
{
  "id": "37i9dQZF1DXcBWIGoYBM5M"  // Se guarda en spotifyPlaylistId
}
```

---

### **9. PlaylistSongEntity**

**Propósito:** Canciones dentro de playlists (con orden)

```java
@Entity
@Table(name = "playlist_songs")
public class PlaylistSongEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                    // Auto-generado
    
    @ManyToOne
    @JoinColumn(name = "playlist_id")
    private PlaylistEntity playlist;
    
    @ManyToOne
    @JoinColumn(name = "song_id")
    private SongEntity song;
    
    private Integer position;           // Asignado: orden en la playlist
    private LocalDateTime addedAt;      // Auto
}
```

**Origen de datos:**
| Atributo | Origen | Tipo |
|----------|--------|------|
| position | Usuario (drag & drop) | Asignado |
| addedAt | Sistema | Auto |

---

### **10. ExplorationSessionEntity**

**Propósito:** Sesiones del grafo neuronal (historial de exploración)

```java
@Entity
@Table(name = "exploration_sessions")
public class ExplorationSessionEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                    // Auto-generado
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;
    
    private String name;                // Asignado: usuario (o auto-generado)
    private String description;         // Asignado: usuario
    
    @ManyToOne
    @JoinColumn(name = "starting_song_id")
    private SongEntity startingSong;    // Asignado: primera canción explorada
    
    private String mode;                // Asignado: modo de visualización
    
    private LocalDateTime createdAt;    // Auto
    private LocalDateTime lastAccessedAt; // Auto (actualizado al abrir)
    
    // Relaciones
    @OneToMany(mappedBy = "session")
    private List<SessionNodeEntity> nodes;
}
```

**Origen de datos:**
| Atributo | Origen | Tipo |
|----------|--------|------|
| name | Auto-generado o usuario | "Sesión 15 Feb 2026" o custom |
| description | Usuario | Asignado |
| startingSong | Sistema | Primera canción en la sesión |
| mode | Usuario (selector UI) | "cinematographic", "temporal", etc. |
| createdAt | Sistema | Auto |
| lastAccessedAt | Sistema | Auto (updated on open) |

---

### **11. SessionNodeEntity**

**Propósito:** Cada nodo del grafo en una sesión

```java
@Entity
@Table(name = "session_nodes")
public class SessionNodeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                    // Auto-generado
    
    @ManyToOne
    @JoinColumn(name = "session_id")
    private ExplorationSessionEntity session;
    
    @ManyToOne
    @JoinColumn(name = "song_id")
    private SongEntity song;
    
    // Posición en el grafo visual
    private Double positionX;           // Calculado: algoritmo de layout
    private Double positionY;           // Calculado: algoritmo de layout
    
    // Árbol de navegación
    @ManyToOne
    @JoinColumn(name = "parent_node_id")
    private SessionNodeEntity parentNode; // Nodo desde el que llegó
    
    private Integer depthLevel;         // Calculado: profundidad en árbol
    
    // Metadata temporal
    private LocalDateTime visitedAt;    // Auto: cuando clickeó el nodo
    private Integer timeSpentSeconds;   // Calculado: tiempo en el nodo
}
```

**Origen de datos:**
| Atributo | Origen | Tipo |
|----------|--------|------|
| song | Usuario (click en grafo) | Acción |
| positionX | Algoritmo force-directed | Calculado |
| positionY | Algoritmo force-directed | Calculado |
| parentNode | Sistema (navegación) | Asignado |
| depthLevel | Sistema | Calculado (parent.depth + 1) |
| visitedAt | Sistema | Auto |
| timeSpentSeconds | Sistema (tracking) | Calculado |

**Algoritmo de posicionamiento:**
```java
// Ejemplo simplificado de force-directed graph
positionX = calculateForceDirectedX(song, relatedSongs, audioFeatures);
positionY = calculateForceDirectedY(song, relatedSongs, audioFeatures);

// Canciones similares están más cerca
// Canciones diferentes están más lejos
```

---

### **12. UserInteractionEntity**

**Propósito:** Analytics de interacciones usuario-canción

```java
@Entity
@Table(name = "user_interactions")
public class UserInteractionEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                    // Auto-generado
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;
    
    @ManyToOne
    @JoinColumn(name = "song_id")
    private SongEntity song;
    
    // Contadores (todos calculados/incrementados)
    private Integer playedCount;        // Incrementado: cada vez que reproduce
    private Integer addedToPlaylistCount; // Incrementado: al añadir a playlist
    private Integer skippedCount;       // Incrementado: si salta antes de 5s
    private Long totalListenTimeMs;     // Acumulado: tiempo total escuchado
    
    private LocalDateTime lastPlayedAt; // Actualizado: última reproducción
    private LocalDateTime createdAt;    // Auto: primera interacción
}
```

**Origen de datos:**
| Atributo | Origen | Tipo |
|----------|--------|------|
| playedCount | Sistema (eventos) | Calculado/incrementado |
| addedToPlaylistCount | Sistema (eventos) | Calculado/incrementado |
| skippedCount | Sistema (eventos) | Calculado/incrementado |
| totalListenTimeMs | Sistema (tracking) | Acumulado |
| lastPlayedAt | Sistema | Auto |
| createdAt | Sistema | Auto |

**Eventos que actualizan:**
```java
// Usuario reproduce preview
onPlaySong(userId, songId) {
    interaction.playedCount++;
    interaction.lastPlayedAt = now();
}

// Usuario salta canción
onSkipSong(userId, songId, timeListened) {
    if (timeListened < 5000) {  // menos de 5s
        interaction.skippedCount++;
    }
    interaction.totalListenTimeMs += timeListened;
}

// Usuario añade a playlist
onAddToPlaylist(userId, songId) {
    interaction.addedToPlaylistCount++;
}
```

---

## 3. Explicación de Relaciones {#explicacion-de-relaciones}

### **Relaciones One-to-One (1:1)**

#### **SongEntity ←→ AudioFeaturesEntity**
- **Cardinalidad:** Una canción tiene exactamente un conjunto de características de audio
- **Clave foránea:** `audio_features.song_id → songs.id`
- **Razón:** Las características de audio son únicas por canción
- **Bidireccional:** Sí
  - Desde Song: `song.getAudioFeatures()`
  - Desde AudioFeatures: `audioFeatures.getSong()`

---

### **Relaciones One-to-Many (1:N)**

#### **UserEntity → PlaylistEntity**
- **Cardinalidad:** Un usuario puede tener muchas playlists
- **Clave foránea:** `playlists.user_id → users.id`
- **Razón:** Cada playlist pertenece a un solo usuario
- **Cascade:** DELETE (si se borra usuario, se borran sus playlists)
- **Acceso:**
  - `user.getPlaylists()` → Lista de playlists del usuario
  - `playlist.getUser()` → Propietario de la playlist

#### **UserEntity → ExplorationSessionEntity**
- **Cardinalidad:** Un usuario puede tener muchas sesiones de exploración
- **Clave foránea:** `exploration_sessions.user_id → users.id`
- **Razón:** Cada sesión pertenece a un usuario
- **Cascade:** DELETE
- **Acceso:**
  - `user.getSessions()` → Historial de exploraciones
  - `session.getUser()` → Quién creó la sesión

#### **ExplorationSessionEntity → SessionNodeEntity**
- **Cardinalidad:** Una sesión contiene muchos nodos
- **Clave foránea:** `session_nodes.session_id → exploration_sessions.id`
- **Razón:** Cada nodo pertenece a una sesión específica
- **Cascade:** DELETE (si se borra sesión, se borran sus nodos)
- **Acceso:**
  - `session.getNodes()` → Todos los nodos de la sesión
  - `node.getSession()` → Sesión a la que pertenece

#### **SessionNodeEntity → SessionNodeEntity (auto-referencia)**
- **Cardinalidad:** Un nodo puede tener un nodo padre
- **Clave foránea:** `session_nodes.parent_node_id → session_nodes.id`
- **Razón:** Estructura de árbol para navegación
- **Nullable:** Sí (el primer nodo no tiene padre)
- **Acceso:**
  - `node.getParentNode()` → Nodo desde el que llegó
  - `node.getChildNodes()` → Nodos explorados desde este

---

### **Relaciones Many-to-Many (N:M)**

#### **SongEntity ←→ TagEntity** (a través de SongTagEntity)
- **Cardinalidad:** Una canción puede tener muchos tags, un tag puede estar en muchas canciones
- **Tabla intermedia:** `song_tags`
- **Razón:** Clasificación flexible (una canción puede ser "rock", "70s", "epic")
- **Atributo extra:** `relevance` (peso del tag, 0-100)
- **Acceso:**
  - `song.getSongTags()` → Tags de la canción con relevancia
  - `tag.getSongTags()` → Canciones con este tag

**Ejemplo de datos:**
```
Bohemian Rhapsody:
  - classic rock (relevance: 100)
  - epic (relevance: 87)
  - 70s (relevance: 65)
  - progressive rock (relevance: 54)
```

#### **SongEntity ←→ MovieEntity** (a través de MovieSongEntity)
- **Cardinalidad:** Una canción puede aparecer en muchas películas, una película puede tener muchas canciones
- **Tabla intermedia:** `movie_songs`
- **Razón:** Soundtracks (ej: "Bohemian Rhapsody" en Wayne's World, Suicide Squad)
- **Atributos extra:**
  - `sceneDescription`: Contexto de la escena
  - `sceneTimestamp`: Momento en la película
- **Acceso:**
  - `song.getMovieSongs()` → En qué películas aparece
  - `movie.getMovieSongs()` → Canciones del soundtrack

**Ejemplo de datos:**
```
"Bohemian Rhapsody" aparece en:
  - Wayne's World (1992)
    - Scene: "Car scene headbanging"
    - Timestamp: "00:23:45"
  - Suicide Squad (2016)
    - Scene: "Opening credits"
    - Timestamp: "00:02:15"
```

#### **PlaylistEntity ←→ SongEntity** (a través de PlaylistSongEntity)
- **Cardinalidad:** Una playlist tiene muchas canciones, una canción puede estar en muchas playlists
- **Tabla intermedia:** `playlist_songs`
- **Razón:** Organización de música
- **Atributos extra:**
  - `position`: Orden en la playlist (importante!)
  - `addedAt`: Cuándo se añadió
- **Acceso:**
  - `playlist.getSongs()` → Canciones ordenadas por position
  - `song.getPlaylists()` → En qué playlists está

**Ejemplo de datos:**
```
Playlist "Road Trip":
  1. Bohemian Rhapsody (position: 1, added: 2026-01-15)
  2. Don't Stop Me Now (position: 2, added: 2026-01-15)
  3. Sweet Child O' Mine (position: 3, added: 2026-01-20)
```

#### **UserEntity ←→ SongEntity** (a través de UserInteractionEntity)
- **Cardinalidad:** Un usuario interactúa con muchas canciones, una canción es interactuada por muchos usuarios
- **Tabla intermedia:** `user_interactions`
- **Razón:** Analytics y personalización
- **Atributos extra:**
  - `playedCount`: Cuántas veces reprodujo
  - `totalListenTimeMs`: Tiempo total escuchado
  - `skippedCount`: Cuántas veces saltó
- **Acceso:**
  - `user.getInteractions()` → Historial musical del usuario
  - `song.getInteractions()` → Quién interactuó con esta canción

**Ejemplo de datos:**
```
Usuario "wara_ecos" con "Bohemian Rhapsody":
  - playedCount: 15
  - totalListenTimeMs: 318000 (5min 18s total)
  - skippedCount: 2
  - addedToPlaylistCount: 3
  - lastPlayedAt: 2026-02-14 10:30:00
```

---

## 4. Origen de Datos por Atributo {#origen-de-datos}

### **Clasificación por origen:**

#### **🤖 Auto-generados (Base de datos)**
```
- Todos los @Id con @GeneratedValue
- Timestamps: createdAt, updatedAt, visitedAt, addedAt, lastPlayedAt
```

#### **👤 Asignados por usuario (Frontend)**
```
UserEntity:
  - username, email, password

PlaylistEntity:
  - name, description, isPublic, coverImageUrl

ExplorationSessionEntity:
  - name (opcional), description

SessionNodeEntity:
  - (indirectamente al clickear)
```

#### **🎵 Obtenidos de Spotify API**
```
SongEntity:
  - id, title, artist, album, releaseYear
  - coverImageUrl, previewUrl, durationMs
  - popularity, explicit

AudioFeaturesEntity:
  - TODOS los atributos (danceability, energy, valence, etc.)

PlaylistEntity:
  - spotifyPlaylistId (al exportar)
```

#### **🏷️ Obtenidos de Last.fm API**
```
TagEntity:
  - name

SongTagEntity:
  - relevance (convertido de "count")
```

#### **🎬 Obtenidos de TMDB API**
```
MovieEntity:
  - id, title, originalTitle, releaseYear
  - director, genre, posterUrl, backdropUrl
```

#### **📊 Calculados por sistema**
```
UserEntity:
  - avgEnergy, avgValence, avgTempo, ecleticismScore
  
SongEntity:
  - explorationCount

SessionNodeEntity:
  - positionX, positionY (algoritmo force-directed)
  - depthLevel
  - timeSpentSeconds

UserInteractionEntity:
  - playedCount, skippedCount
  - addedToPlaylistCount, totalListenTimeMs
```

#### **👥 Crowdsourced (comunidad)**
```
MovieSongEntity:
  - sceneDescription, sceneTimestamp
```

---

## 5. Flujo de Población de Datos

### **Escenario 1: Usuario busca "Bohemian Rhapsody"**

```
1. Usuario escribe "Bohemian Rhapsody" en búsqueda
   ↓
2. Backend busca en BD:
   SELECT * FROM songs WHERE title LIKE '%Bohemian%'
   ↓
3. NO encontrado → Llamar a Spotify:
   GET /v1/search?q=Bohemian%20Rhapsody&type=track
   ↓
4. Spotify devuelve: id="3n3Ppam7vgaVa1iaRUc9Lp"
   ↓
5. Obtener detalles completos:
   GET /v1/tracks/3n3Ppam7vgaVa1iaRUc9Lp
   ↓
6. Crear SongEntity y guardar ✅
   ↓
7. Obtener audio features:
   GET /v1/audio-features/3n3Ppam7vgaVa1iaRUc9Lp
   ↓
8. Crear AudioFeaturesEntity y guardar ✅
   ↓
9. Obtener tags de Last.fm:
   GET /2.0/?method=track.getTopTags&artist=Queen&track=Bohemian Rhapsody
   ↓
10. Para cada tag:
    - Buscar o crear TagEntity
    - Crear SongTagEntity con relevancia ✅
   ↓
11. Devolver al usuario (con todo cacheado para próximas búsquedas)
```

### **Escenario 2: Usuario explora el grafo neuronal**

```
1. Usuario hace click en nodo "Bohemian Rhapsody"
   ↓
2. Backend crea/actualiza ExplorationSessionEntity
   - Si es nueva sesión → crear
   - Si continúa sesión → actualizar lastAccessedAt
   ↓
3. Crear SessionNodeEntity:
   - sessionId = sesión actual
   - songId = "3n3Ppam7vgaVa1iaRUc9Lp"
   - parentNodeId = nodo previo (si existe)
   - depthLevel = parent.depth + 1
   - positionX, positionY = algoritmo de layout
   - visitedAt = now()
   ↓
4. Incrementar SongEntity.explorationCount ✅
   ↓
5. Crear/actualizar UserInteractionEntity:
   - playedCount++
   - lastPlayedAt = now()
   ↓
6. Calcular nodos relacionados (canciones similares):
   - Por audio features (energy, valence similar)
   - Por tags comunes
   - Por películas comunes
   ↓
7. Devolver grafo al frontend para visualización
```

### **Escenario 3: Sistema calcula perfil de usuario (cron job nocturno)**

```
1. Cada noche a las 03:00 → Cron job ejecuta
   ↓
2. Para cada usuario:
   ↓
3. Calcular avgEnergy:
   SELECT AVG(af.energy)
   FROM user_interactions ui
   JOIN songs s ON ui.song_id = s.id
   JOIN audio_features af ON s.id = af.song_id
   WHERE ui.user_id = X AND ui.played_count > 0
   ↓
4. Calcular avgValence (similar)
   ↓
5. Calcular avgTempo (similar)
   ↓
6. Calcular ecleticismScore:
   - Obtener géneros únicos
   - Obtener rango de años
   - Obtener varianza de audio features
   - Score = diversidad normalizada (0-1)
   ↓
7. Actualizar UserEntity ✅
```

---

## 6. Índices Recomendados

```sql
-- Búsquedas frecuentes
CREATE INDEX idx_songs_title ON songs(title);
CREATE INDEX idx_songs_artist ON songs(artist);

-- Joins comunes
CREATE INDEX idx_audio_features_song_id ON audio_features(song_id);
CREATE INDEX idx_song_tags_song_id ON song_tags(song_id);
CREATE INDEX idx_song_tags_tag_id ON song_tags(tag_id);

-- Filtros de similitud
CREATE INDEX idx_audio_features_energy ON audio_features(energy);
CREATE INDEX idx_audio_features_valence ON audio_features(valence);
CREATE INDEX idx_audio_features_tempo ON audio_features(tempo);

-- Navegación de sesiones
CREATE INDEX idx_session_nodes_session_id ON session_nodes(session_id);
CREATE INDEX idx_session_nodes_parent_id ON session_nodes(parent_node_id);

-- Analytics
CREATE INDEX idx_user_interactions_user_id ON user_interactions(user_id);
CREATE INDEX idx_user_interactions_song_id ON user_interactions(song_id);
```

---

## 7. Resumen Final

| Entidad | Datos Externos | Datos Internos | Datos Usuario |
|---------|----------------|----------------|---------------|
| UserEntity | Spotify OAuth | Perfil calculado | Email, username |
| SongEntity | Spotify | explorationCount | - |
| AudioFeaturesEntity | Spotify | - | - |
| TagEntity | Last.fm | Categorización | - |
| SongTagEntity | Last.fm | - | - |
| MovieEntity | TMDB | - | - |
| MovieSongEntity | - | - | Crowdsourced |
| PlaylistEntity | Spotify (export) | - | Nombre, descripción |
| PlaylistSongEntity | - | - | Orden |
| ExplorationSessionEntity | - | Auto-tracking | Nombre opcional |
| SessionNodeEntity | - | Posicionamiento | Clicks |
| UserInteractionEntity | - | Analytics | Acciones |

---

---

## 7. Flujos de Datos Completos {#flujos-de-datos}

### 🔄 Flujo 1: Usuario se registra

```
┌──────────────┐
│   FRONTEND   │
└──────┬───────┘
       │ POST /api/auth/register
       │ Body: RegisterUserDTO
       ↓
┌──────────────────────────────────────┐
│ AuthController.java                  │
│ (interfaces/rest/controller/)        │
└──────┬───────────────────────────────┘
       │ @Valid RegisterUserDTO
       ↓
┌──────────────────────────────────────┐
│ RegisterUserUseCase.java             │
│ (application/usecase/user/)          │
└──────┬───────────────────────────────┘
       │
       ├─→ Validar email único (UserRepository)
       ├─→ Hashear password (bcrypt)
       │
       ↓
┌──────────────────────────────────────┐
│ UserMapper.java                      │
│ (infrastructure/persistence/mapper/) │
└──────┬───────────────────────────────┘
       │ User (domain) → UserEntity
       ↓
┌──────────────────────────────────────┐
│ UserRepositoryImpl.java              │
│ (infrastructure/persistence/repo/)   │
└──────┬───────────────────────────────┘
       │ userRepository.save(entity)
       ↓
┌──────────────────────────────────────┐
│ PostgreSQL - Tabla: users            │
└──────┬───────────────────────────────┘
       │ INSERT → UserEntity (con ID)
       ↓
       │ UserEntity → User (domain)
       ↓
       │ User → UserProfileDTO
       ↓
┌──────────────────────────────────────┐
│ JSON Response → Frontend             │
│ UserProfileDTO                       │
└──────────────────────────────────────┘
```

**Archivos involucrados:**
1. `interfaces/rest/dto/request/RegisterUserDTO.java`
2. `interfaces/rest/controller/AuthController.java`
3. `application/usecase/user/RegisterUserUseCase.java`
4. `infrastructure/persistence/mapper/UserMapper.java`
5. `infrastructure/persistence/entity/UserEntity.java`
6. `infrastructure/persistence/repository/UserRepositoryImpl.java`
7. `interfaces/rest/dto/response/UserProfileDTO.java`

---

### 🔍 Flujo 2: Usuario busca "Bohemian Rhapsody"

```
┌──────────────┐
│   FRONTEND   │
└──────┬───────┘
       │ GET /api/search?query=Bohemian Rhapsody
       │ Params: SearchRequestDTO
       ↓
┌──────────────────────────────────────┐
│ SearchController.java                │
│ (interfaces/rest/controller/)        │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ SearchSongsUseCase.java              │
│ (application/usecase/search/)        │
└──────┬───────────────────────────────┘
       │
       ├─→ 1. Buscar en BD (SongRepository)
       │   ¿Existe?
       │   │
       │   NO ↓
       │   ┌──────────────────────────────┐
       │   │ SpotifyClient.java           │
       │   │ (infrastructure/adapter/)    │
       │   └──────┬───────────────────────┘
       │          │ GET /v1/search
       │          ↓
       │   ┌──────────────────────────────┐
       │   │ Spotify API                  │
       │   └──────┬───────────────────────┘
       │          │ SpotifyTrackDTO
       │          ↓
       │   ┌──────────────────────────────┐
       │   │ SpotifyClient.java           │
       │   │ getAudioFeatures()           │
       │   └──────┬───────────────────────┘
       │          │ SpotifyAudioFeaturesDTO
       │          ↓
       │   ┌──────────────────────────────┐
       │   │ LastFmClient.java            │
       │   │ getTrackTags()               │
       │   └──────┬───────────────────────┘
       │          │ LastFmTagDTO
       │          ↓
       │   ┌──────────────────────────────┐
       │   │ SongMapper.java              │
       │   │ fromSpotifyDTO()             │
       │   └──────┬───────────────────────┘
       │          │ Song (domain)
       │          ↓
       │   ┌──────────────────────────────┐
       │   │ SongMapper.toEntity()        │
       │   └──────┬───────────────────────┘
       │          │ SongEntity
       │          ↓
       │   ┌──────────────────────────────┐
       │   │ SongRepositoryImpl.save()    │
       │   └──────┬───────────────────────┘
       │          │ Guardado en BD ✅
       │          ↓
       └──────────┘
       │
       ├─→ 2. Calcular canciones similares
       │   (por audio features, tags, etc.)
       │
       ↓
┌──────────────────────────────────────┐
│ SongMapper.toDTO()                   │
└──────┬───────────────────────────────┘
       │ List<SongDTO>
       ↓
┌──────────────────────────────────────┐
│ SearchResponseDTO                    │
│ (songs + graphNodes)                 │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ JSON Response → Frontend             │
└──────────────────────────────────────┘
```

**Archivos involucrados:**
1. `interfaces/rest/controller/SearchController.java`
2. `application/usecase/search/SearchSongsUseCase.java`
3. `infrastructure/adapter/spotify/SpotifyClient.java`
4. `infrastructure/adapter/spotify/dto/SpotifyTrackDTO.java`
5. `infrastructure/adapter/spotify/dto/SpotifyAudioFeaturesDTO.java`
6. `infrastructure/adapter/lastfm/LastFmClient.java`
7. `infrastructure/adapter/lastfm/dto/LastFmTagDTO.java`
8. `infrastructure/persistence/mapper/SongMapper.java`
9. `infrastructure/persistence/entity/SongEntity.java`
10. `infrastructure/persistence/repository/SongRepositoryImpl.java`
11. `domain/model/Song.java`
12. `interfaces/rest/dto/response/SongDTO.java`
13. `interfaces/rest/dto/response/SearchResponseDTO.java`

---

### 📝 Flujo 3: Usuario crea una playlist

```
┌──────────────┐
│   FRONTEND   │
└──────┬───────┘
       │ POST /api/playlists
       │ Body: CreatePlaylistDTO
       ↓
┌──────────────────────────────────────┐
│ PlaylistController.java              │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ CreatePlaylistUseCase.java           │
└──────┬───────────────────────────────┘
       │
       ├─→ Verificar límite (100 playlists)
       │
       ↓
┌──────────────────────────────────────┐
│ Playlist (domain)                    │
│ - aplicar reglas de negocio          │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ PlaylistMapper.toEntity()            │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ PlaylistRepositoryImpl.save()        │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ PostgreSQL - Tabla: playlists        │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ PlaylistDTO → Frontend               │
└──────────────────────────────────────┘
```

**Archivos involucrados:**
1. `interfaces/rest/dto/request/CreatePlaylistDTO.java`
2. `interfaces/rest/controller/PlaylistController.java`
3. `application/usecase/playlist/CreatePlaylistUseCase.java`
4. `domain/model/Playlist.java`
5. `infrastructure/persistence/mapper/PlaylistMapper.java`
6. `infrastructure/persistence/entity/PlaylistEntity.java`
7. `infrastructure/persistence/repository/PlaylistRepositoryImpl.java`
8. `interfaces/rest/dto/response/PlaylistDTO.java`

---

### 🗺️ Flujo 4: Usuario explora el grafo neuronal

```
┌──────────────┐
│   FRONTEND   │
└──────┬───────┘
       │ POST /api/sessions/{id}/nodes
       │ Body: { songId: "abc123" }
       ↓
┌──────────────────────────────────────┐
│ SessionController.java               │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ AddNodeToSessionUseCase.java         │
└──────┬───────────────────────────────┘
       │
       ├─→ 1. Obtener Session actual
       │
       ├─→ 2. Crear SessionNode
       │   - Calcular posición (force-directed)
       │   - Asignar parentNode
       │   - Calcular depthLevel
       │
       ├─→ 3. Guardar SessionNodeEntity
       │
       ├─→ 4. Actualizar SongEntity.explorationCount++
       │
       ├─→ 5. Crear/Actualizar UserInteractionEntity
       │   - playedCount++
       │   - lastPlayedAt = now()
       │
       ├─→ 6. Calcular nodos relacionados
       │   (canciones similares para el grafo)
       │
       ↓
┌──────────────────────────────────────┐
│ GraphNodeDTO (lista)                 │
│ - nodo clickeado                     │
│ - nodos relacionados                 │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ JSON Response → Frontend             │
│ (para renderizar grafo)              │
└──────────────────────────────────────┘
```

**Archivos involucrados:**
1. `interfaces/rest/controller/SessionController.java`
2. `application/usecase/session/AddNodeToSessionUseCase.java`
3. `domain/model/ExplorationSession.java`
4. `domain/model/SessionNode.java`
5. `domain/model/GraphNode.java`
6. `infrastructure/persistence/entity/SessionNodeEntity.java`
7. `infrastructure/persistence/entity/UserInteractionEntity.java`
8. `infrastructure/persistence/repository/SessionRepositoryImpl.java`
9. `infrastructure/persistence/repository/UserInteractionRepositoryImpl.java`
10. `interfaces/rest/dto/response/GraphNodeDTO.java`

---

### 📤 Flujo 5: Exportar playlist a Spotify

```
┌──────────────┐
│   FRONTEND   │
└──────┬───────┘
       │ POST /api/playlists/{id}/export
       ↓
┌──────────────────────────────────────┐
│ PlaylistController.java              │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ ExportPlaylistToSpotifyUseCase.java  │
└──────┬───────────────────────────────┘
       │
       ├─→ 1. Obtener PlaylistEntity de BD
       │
       ├─→ 2. Obtener canciones de la playlist
       │
       ↓
┌──────────────────────────────────────┐
│ SpotifyClient.java                   │
│ createPlaylist()                     │
└──────┬───────────────────────────────┘
       │ POST /v1/users/{id}/playlists
       ↓
┌──────────────────────────────────────┐
│ Spotify API                          │
└──────┬───────────────────────────────┘
       │ SpotifyPlaylistDTO (con ID)
       ↓
┌──────────────────────────────────────┐
│ SpotifyClient.addTracksToPlaylist()  │
└──────┬───────────────────────────────┘
       │ POST /v1/playlists/{id}/tracks
       ↓
┌──────────────────────────────────────┐
│ Spotify API                          │
│ (playlist completa)                  │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ Actualizar PlaylistEntity            │
│ - spotifyPlaylistId = "..."          │
└──────┬───────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ PlaylistDTO → Frontend               │
│ (con enlace a Spotify)               │
└──────────────────────────────────────┘
```

**Archivos involucrados:**
1. `interfaces/rest/controller/PlaylistController.java`
2. `application/usecase/playlist/ExportPlaylistToSpotifyUseCase.java`
3. `infrastructure/persistence/entity/PlaylistEntity.java`
4. `infrastructure/persistence/entity/PlaylistSongEntity.java`
5. `infrastructure/adapter/spotify/SpotifyClient.java`
6. `infrastructure/adapter/spotify/dto/SpotifyPlaylistDTO.java`
7. `infrastructure/persistence/repository/PlaylistRepositoryImpl.java`
8. `interfaces/rest/dto/response/PlaylistDTO.java`

---

## 🎯 Reglas de Conversión entre Capas

### Flujo de datos típico:

```
External API → External DTO → Domain Model → Entity → DB
                    ↓              ↓           ↓
               (Adapter)      (UseCase)   (Repository)
                               
DB → Entity → Domain Model → Response DTO → JSON
       ↓           ↓              ↓
  (Repository) (Mapper)     (Controller)
```

### Mappers responsables:

| Conversión | Mapper | Método |
|------------|--------|--------|
| Entity → Domain | UserMapper | `toDomain(UserEntity)` |
| Domain → Entity | UserMapper | `toEntity(User)` |
| Domain → Response DTO | UserMapper | `toDTO(User)` |
| Request DTO → Domain | UseCase | Lógica directa |
| External DTO → Domain | Adapter + Mapper | `fromSpotifyDTO()` |

---

**Fecha de actualización:** 15 de febrero de 2026  
**Proyecto:** Ecos - Descubrimiento Musical Inteligente  
**Autor:** Wara Yasy Pacheco Arandia

