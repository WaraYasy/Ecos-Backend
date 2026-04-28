package es.wara.ecos.backend.functional;

import es.wara.ecos.backend.support.BaseFunctionalTest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests funcionales de seguridad end-to-end.
 *
 * Cubre cuatro áreas de seguridad con JWT real activo:
 *   1. Rechazo de tokens JWT malformados o manipulados
 *   2. Protección del endpoint de administración mediante API key
 *   3. Protección IDOR en endpoints de usuario y playlist
 *   4. Manejo seguro de payloads con intentos de inyección SQL
 */
class SecurityFlowTest extends BaseFunctionalTest {

    static final String TEST_ADMIN_KEY = "test-admin-secret-key-for-security-tests";
    static final String TEST_JWT_SECRET = "test-jwt-secret-key-minimum-256-bits-for-testing-purposes-only";

    @DynamicPropertySource
    static void adminKeyProperty(DynamicPropertyRegistry registry) {
        registry.add("admin.api-key", () -> TEST_ADMIN_KEY);
    }

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanDb() {
        jdbc.execute("TRUNCATE TABLE users CASCADE");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void register(String username, String email) throws Exception {
        mockMvc.perform(post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","email":"%s","password":"password123","language":"es"}
                        """.formatted(username, email)));
    }

    private String loginAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}
                                """.formatted(email)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    private String loginAndGetUserId(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}
                                """.formatted(email)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("userId").asText();
    }

    /** Token firmado con una clave distinta a la del servidor. */
    private String tokenSignedWithWrongKey() {
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "wrong-secret-completely-different-key-that-is-long-enough"
                        .getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86_400_000))
                .signWith(wrongKey)
                .compact();
    }

    /** Token expirado (emitido y expirado 1 hora en el pasado). */
    private String expiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        long oneHourAgo = System.currentTimeMillis() - 3_600_000;
        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .issuedAt(new Date(oneHourAgo - 1000))
                .expiration(new Date(oneHourAgo))
                .signWith(key)
                .compact();
    }

    /**
     * Ataque alg:none — JWT con algoritmo "none" y sin firma.
     * El header codificado es {"alg":"none","typ":"JWT"}.
     */
    private String algNoneToken() {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        long future = System.currentTimeMillis() + 86_400_000;
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"sub\":\"" + UUID.randomUUID() + "\",\"exp\":" + (future / 1000) + "}}")
                        .getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. JWT — rechazo de tokens inválidos
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class JwtAuthentication {

        @Test
        void noAuthorizationHeader_returns401() throws Exception {
            mockMvc.perform(get("/v1/playlists"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        }

        @Test
        void authHeader_withoutBearerPrefix_returns401() throws Exception {
            mockMvc.perform(get("/v1/playlists")
                            .header("Authorization", "Basic dXNlcjpwYXNz"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void bearerToken_signedWithWrongSecret_returns401() throws Exception {
            mockMvc.perform(get("/v1/playlists")
                            .header("Authorization", "Bearer " + tokenSignedWithWrongKey()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void bearerToken_expired_returns401() throws Exception {
            mockMvc.perform(get("/v1/playlists")
                            .header("Authorization", "Bearer " + expiredToken()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void bearerToken_algNoneAttack_returns401() throws Exception {
            mockMvc.perform(get("/v1/playlists")
                            .header("Authorization", "Bearer " + algNoneToken()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void bearerToken_randomString_returns401() throws Exception {
            mockMvc.perform(get("/v1/playlists")
                            .header("Authorization", "Bearer not.a.jwt"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void bearerToken_emptyToken_returns401() throws Exception {
            mockMvc.perform(get("/v1/playlists")
                            .header("Authorization", "Bearer "))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void validToken_grantesAccess() throws Exception {
            register("wara", "wara@ecos.es");
            String token = loginAndGetToken("wara@ecos.es");

            mockMvc.perform(get("/v1/playlists")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Admin API key — protección del endpoint de importación CSV
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class AdminApiKey {

        private MockMultipartFile minimalCsv() {
            return new MockMultipartFile("file", "test.csv", "text/csv",
                    "header1,header2\nval1,val2".getBytes(StandardCharsets.UTF_8));
        }

        @Test
        void noAdminKey_returns401() throws Exception {
            mockMvc.perform(multipart("/v1/admin/songs/import")
                            .file(minimalCsv()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        }

        @Test
        void wrongAdminKey_returns401() throws Exception {
            mockMvc.perform(multipart("/v1/admin/songs/import")
                            .file(minimalCsv())
                            .header("X-Admin-Key", "wrong-key"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        }

        @Test
        void emptyAdminKey_returns401() throws Exception {
            mockMvc.perform(multipart("/v1/admin/songs/import")
                            .file(minimalCsv())
                            .header("X-Admin-Key", ""))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void correctAdminKey_reachesHandler() throws Exception {
            // Llega al controller (CSV inválido → 500, pero no 401)
            mockMvc.perform(multipart("/v1/admin/songs/import")
                            .file(minimalCsv())
                            .header("X-Admin-Key", TEST_ADMIN_KEY))
                    .andExpect(result ->
                            org.assertj.core.api.Assertions.assertThat(
                                    result.getResponse().getStatus())
                                    .isNotEqualTo(401));
        }

        @Test
        void adminEndpoint_withJwtOnly_returns401() throws Exception {
            // JWT no es suficiente para acceder al admin endpoint
            register("wara", "wara@ecos.es");
            String token = loginAndGetToken("wara@ecos.es");

            mockMvc.perform(multipart("/v1/admin/songs/import")
                            .file(minimalCsv())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. IDOR — protección de recursos de usuario
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class UserIdor {

        @Test
        void updatePreferences_forOtherUser_returns403() throws Exception {
            register("wara", "wara@ecos.es");
            register("otra", "otra@ecos.es");
            String tokenWara = loginAndGetToken("wara@ecos.es");
            String otraId = loginAndGetUserId("otra@ecos.es");

            mockMvc.perform(put("/v1/users/" + otraId + "/preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + tokenWara)
                            .content("""
                                    {"language":"en"}
                                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        }

        @Test
        void patchUser_forOtherUser_returns403() throws Exception {
            register("wara", "wara@ecos.es");
            register("otra", "otra@ecos.es");
            String tokenWara = loginAndGetToken("wara@ecos.es");
            String otraId = loginAndGetUserId("otra@ecos.es");

            mockMvc.perform(patch("/v1/users/" + otraId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + tokenWara)
                            .content("""
                                    {"username":"hackeado"}
                                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        }

        @Test
        void changePassword_forOtherUser_returns403() throws Exception {
            register("wara", "wara@ecos.es");
            register("otra", "otra@ecos.es");
            String tokenWara = loginAndGetToken("wara@ecos.es");
            String otraId = loginAndGetUserId("otra@ecos.es");

            mockMvc.perform(patch("/v1/users/" + otraId + "/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + tokenWara)
                            .content("""
                                    {"currentPassword":"password123","newPassword":"hacked456"}
                                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        }

        @Test
        void updatePreferences_forOwnUser_returns200() throws Exception {
            register("wara", "wara@ecos.es");
            String tokenWara = loginAndGetToken("wara@ecos.es");
            String waraId = loginAndGetUserId("wara@ecos.es");

            mockMvc.perform(put("/v1/users/" + waraId + "/preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + tokenWara)
                            .content("""
                                    {"language":"en"}
                                    """))
                    .andExpect(status().isOk());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Inyección SQL — validación de que JPA parametriza las consultas
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class SqlInjection {

        @Test
        void searchWithSqlInjectionPayload_doesNotCrash() throws Exception {
            register("wara", "wara@ecos.es");
            String token = loginAndGetToken("wara@ecos.es");

            // Un intento de SQL injection clásico en el parámetro de búsqueda
            // Verificar que no devuelve 500 (crash) ni 403 (falso negativo de auth)
            mockMvc.perform(get("/v1/search")
                            .param("q", "' OR '1'='1")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(result ->
                            org.assertj.core.api.Assertions.assertThat(
                                    result.getResponse().getStatus())
                                    .isNotIn(500, 403));
        }

        @Test
        void loginWithSqlInjectionInEmail_returns401OrBadRequest() throws Exception {
            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"' OR 1=1 --","password":"irrelevant"}
                                    """))
                    .andExpect(result ->
                            org.assertj.core.api.Assertions.assertThat(
                                    result.getResponse().getStatus())
                                    .isIn(400, 401));
        }

        @Test
        void registerWithSqlInjectionInUsername_returns400OrCreated() throws Exception {
            // La validación de Bean Validation puede rechazar esto antes de que llegue a JPA
            mockMvc.perform(post("/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"admin'--","email":"inject@test.es","password":"password123","language":"es"}
                                    """))
                    .andExpect(result ->
                            org.assertj.core.api.Assertions.assertThat(
                                    result.getResponse().getStatus())
                                    .isIn(201, 400));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Rutas públicas — accesibles sin token
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class PublicEndpoints {

        @Test
        void healthEndpoint_publicAccess_returns200() throws Exception {
            mockMvc.perform(get("/health"))
                    .andExpect(status().isOk());
        }

        @Test
        void register_publicAccess_returns201() throws Exception {
            mockMvc.perform(post("/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"publico","email":"publico@ecos.es","password":"password123","language":"es"}
                                    """))
                    .andExpect(status().isCreated());
        }

        @Test
        void login_publicAccess_noTokenRequired() throws Exception {
            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"noexiste@ecos.es","password":"cualquiera"}
                                    """))
                    .andExpect(result ->
                            org.assertj.core.api.Assertions.assertThat(
                                    result.getResponse().getStatus())
                                    .isNotEqualTo(403));
        }
    }
}