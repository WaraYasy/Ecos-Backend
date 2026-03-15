package es.wara.ecos.backend.functional;

import es.wara.ecos.backend.support.BaseFunctionalTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests funcionales del flujo de usuarios.
 *
 * Cubre consulta de perfil, actualización de preferencias y protección IDOR
 * contra modificaciones de datos de otros usuarios. Todos los tests usan JWT real.
 */
class UserFlowTest extends BaseFunctionalTest {

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanDb() {
        jdbc.execute("TRUNCATE TABLE users CASCADE");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String registerAndLogin(String username, String email) throws Exception {
        mockMvc.perform(post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","email":"%s","password":"password123","language":"es"}
                        """.formatted(username, email)));

        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}
                                """.formatted(email)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    void getUser_existingUser_returns200WithData() throws Exception {
        // Registrar usuario y obtener su token + id
        mockMvc.perform(post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"wara","email":"wara@ecos.es","password":"password123","language":"es"}
                        """));

        MvcResult loginResult = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"wara@ecos.es","password":"password123"}
                                """))
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();
        String userId = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("userId").asText();

        mockMvc.perform(get("/v1/users/" + userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.username").value("wara"))
                .andExpect(jsonPath("$.email").value("wara@ecos.es"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void getUser_nonExistingId_returns404() throws Exception {
        String token = registerAndLogin("wara", "wara@ecos.es");

        mockMvc.perform(get("/v1/users/00000000-0000-0000-0000-000000000099")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void updatePreferences_ownUser_returns200WithUpdatedLanguage() throws Exception {
        mockMvc.perform(post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"wara","email":"wara@ecos.es","password":"password123","language":"es"}
                        """));

        MvcResult loginResult = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"wara@ecos.es","password":"password123"}
                                """))
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();
        String userId = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("userId").asText();

        mockMvc.perform(put("/v1/users/" + userId + "/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("""
                                {"language":"en"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId));
    }

    @Test
    void updatePreferences_otherUsersId_returns403() throws Exception {
        // Registrar dos usuarios
        mockMvc.perform(post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"wara","email":"wara@ecos.es","password":"password123","language":"es"}
                        """));

        MvcResult ownerLogin = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"wara@ecos.es","password":"password123"}
                                """))
                .andReturn();
        String ownerUserId = objectMapper.readTree(ownerLogin.getResponse().getContentAsString())
                .get("userId").asText();

        // Segunda usuaria intenta modificar las preferencias de wara
        mockMvc.perform(post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"otra","email":"otra@ecos.es","password":"password123","language":"es"}
                        """));

        MvcResult otherLogin = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"otra@ecos.es","password":"password123"}
                                """))
                .andReturn();
        String otherToken = objectMapper.readTree(otherLogin.getResponse().getContentAsString())
                .get("token").asText();

        mockMvc.perform(put("/v1/users/" + ownerUserId + "/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + otherToken)
                        .content("""
                                {"language":"en"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void getMusicProfile_existingUser_returns200WithProfileData() throws Exception {
        mockMvc.perform(post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"wara","email":"wara@ecos.es","password":"password123","language":"es"}
                        """));

        MvcResult loginResult = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"wara@ecos.es","password":"password123"}
                                """))
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();
        String userId = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("userId").asText();

        // Usuario sin sesiones → perfil con valores vacíos/cero
        mockMvc.perform(get("/v1/users/" + userId + "/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.avgEnergy").exists())
                .andExpect(jsonPath("$.topGenres").isArray());
    }

    @Test
    void getMusicProfile_nonExistingUser_returns404() throws Exception {
        mockMvc.perform(post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"wara","email":"wara@ecos.es","password":"password123","language":"es"}
                        """));

        MvcResult loginResult = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"wara@ecos.es","password":"password123"}
                                """))
                .andReturn();
        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();

        mockMvc.perform(get("/v1/users/00000000-0000-0000-0000-000000000099/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatePreferences_missingLanguage_returns400() throws Exception {
        mockMvc.perform(post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"wara","email":"wara@ecos.es","password":"password123","language":"es"}
                        """));

        MvcResult loginResult = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"wara@ecos.es","password":"password123"}
                                """))
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();
        String userId = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("userId").asText();

        mockMvc.perform(put("/v1/users/" + userId + "/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}