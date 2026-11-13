package com.borae.auth;

import com.borae.identity.RestaurantMembershipRepository;
import com.borae.identity.RestaurantRepository;
import com.borae.identity.UserAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private RestaurantMembershipRepository membershipRepository;

    @BeforeEach
    void cleanDatabase() {
        membershipRepository.deleteAll();
        restaurantRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterUserAndReturnJwtAndCurrentProfile() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        String body = objectMapper.writeValueAsString(Map.of(
                "fullName", "Pessoa Teste",
                "email", email,
                "password", "senha-segura-123"
        ));

        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(response).get("accessToken").asText();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Pessoa Teste"))
                .andExpect(jsonPath("$.email").value(email));

        mockMvc.perform(get("/api/v1/users/me/restaurants")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/v1/system/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void shouldCreateRestaurantAndAutomaticallyAssignOwnerRole() throws Exception {
        String email = "owner-" + UUID.randomUUID() + "@example.com";
        String registration = objectMapper.writeValueAsString(Map.of(
                "fullName", "Pessoa Proprietária",
                "email", email,
                "password", "senha-segura-123"
        ));

        String authResponse = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(authResponse).get("accessToken").asText();

        String restaurantRequest = objectMapper.writeValueAsString(Map.of(
                "name", "Café São Paulo",
                "description", "Café e brunch"
        ));

        mockMvc.perform(post("/api/v1/restaurants")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(restaurantRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.restaurantName").value("Café São Paulo"))
                .andExpect(jsonPath("$.restaurantSlug").value(org.hamcrest.Matchers.startsWith("cafe-sao-paulo-")))
                .andExpect(jsonPath("$.role").value("OWNER"));

        mockMvc.perform(get("/api/v1/users/me/restaurants")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].role").value("OWNER"))
                .andExpect(jsonPath("$[0].restaurantName").value("Café São Paulo"));
    }

    @Test
    void shouldRejectRestaurantCreationWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Restaurante Teste"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectRestaurantWithBlankName() throws Exception {
        String email = "invalid-" + UUID.randomUUID() + "@example.com";
        String registration = objectMapper.writeValueAsString(Map.of(
                "fullName", "Pessoa Teste",
                "email", email,
                "password", "senha-segura-123"
        ));
        String authResponse = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(authResponse).get("accessToken").asText();

        mockMvc.perform(post("/api/v1/restaurants")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "   "))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {
        String email = "duplicate-" + UUID.randomUUID() + "@example.com";
        String body = objectMapper.writeValueAsString(Map.of(
                "fullName", "Pessoa Teste",
                "email", email,
                "password", "senha-segura-123"
        ));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectInvalidCredentials() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "nao-existe@example.com",
                "password", "senha-incorreta"
        ));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRequireJwtForProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/system/status"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users/me/restaurants"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldInviteAcceptManageAndRemoveRestaurantMember() throws Exception {
        String ownerEmail = "owner-" + UUID.randomUUID() + "@example.com";
        String ownerToken = registerAndGetToken("Pessoa Proprietária", ownerEmail);

        String restaurantResponse = mockMvc.perform(post("/api/v1/restaurants")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Restaurante da Equipe"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andReturn().getResponse().getContentAsString();
        String restaurantId = objectMapper.readTree(restaurantResponse).get("restaurantId").asText();

        String memberEmail = "staff-" + UUID.randomUUID() + "@example.com";
        String invitationResponse = mockMvc.perform(post("/api/v1/restaurants/" + restaurantId + "/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", memberEmail, "role", "STAFF"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(memberEmail))
                .andExpect(jsonPath("$.role").value("STAFF"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String invitationToken = objectMapper.readTree(invitationResponse).get("token").asText();

        String memberToken = registerAndGetToken("Pessoa da Equipe", memberEmail);
        String acceptedResponse = mockMvc.perform(post("/api/v1/restaurant-invitations/accept")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", invitationToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STAFF"))
                .andReturn().getResponse().getContentAsString();
        String membershipId = objectMapper.readTree(acceptedResponse).get("membershipId").asText();

        mockMvc.perform(get("/api/v1/restaurants/" + restaurantId + "/members")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(patch("/api/v1/restaurants/" + restaurantId + "/members/" + membershipId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MANAGER"));

        mockMvc.perform(delete("/api/v1/restaurants/" + restaurantId + "/members/" + membershipId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/restaurants/" + restaurantId + "/members")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldNotAllowInvitingOwnerRole() throws Exception {
        String ownerEmail = "owner-role-" + UUID.randomUUID() + "@example.com";
        String ownerToken = registerAndGetToken("Pessoa Proprietária", ownerEmail);
        String restaurantResponse = mockMvc.perform(post("/api/v1/restaurants")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Restaurante Seguro"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String restaurantId = objectMapper.readTree(restaurantResponse).get("restaurantId").asText();

        mockMvc.perform(post("/api/v1/restaurants/" + restaurantId + "/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "another-" + UUID.randomUUID() + "@example.com",
                                "role", "OWNER"
                        ))))
                .andExpect(status().isBadRequest());
    }

    private String registerAndGetToken(String fullName, String email) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "fullName", fullName,
                "email", email,
                "password", "senha-segura-123"
        ));
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

}
