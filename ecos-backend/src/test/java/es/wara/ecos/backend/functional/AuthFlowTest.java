package es.wara.ecos.backend.functional;

import es.wara.ecos.backend.support.BaseFunctionalTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests funcionales del flujo de autenticación.
 *
 * Cubre registro, login y acceso a endpoints protegidos con JWT real.
 * JwtAuthFilter está activo — no hay mocks de seguridad.
 */
class AuthFlowTest extends BaseFunctionalTest {

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanDb() {
        jdbc.execute("TRUNCATE TABLE users CASCADE");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void register(String username, String email, String password) throws Exception {
        mockMvc.perform(post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","email":"%s","password":"%s","language":"es"}
                        """.formatted(username, email, password)));
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    void register_validData_returns201WithUserId() throws Exception {
        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"wara","email":"wara@ecos.es","password":"password123","language":"es"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("wara"))
                .andExpect(jsonPath("$.email").value("wara@ecos.es"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        register("wara", "wara@ecos.es", "password123");

        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"otra","email":"wara@ecos.es","password":"password123","language":"es"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void register_missingUsername_returns400() throws Exception {
        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"wara@ecos.es","password":"password123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        register("wara", "wara@ecos.es", "password123");

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"wara@ecos.es","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.userId").exists());
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        register("wara", "wara@ecos.es", "password123");

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"wara@ecos.es","password":"wrongpassword"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void login_nonExistentEmail_returns401() throws Exception {
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"noexiste@ecos.es","password":"password123"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/playlists"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void protectedEndpoint_withValidToken_returns200() throws Exception {
        register("wara", "wara@ecos.es", "password123");
        String token = login("wara@ecos.es", "password123");

        mockMvc.perform(get("/v1/playlists")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/playlists")
                        .header("Authorization", "Bearer token.invalido.falso"))
                .andExpect(status().isUnauthorized());
    }
}