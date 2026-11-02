package com.abcbank.filestorage.controllers;

import com.abcbank.filestorage.dto.AccessTokenDto;
import com.abcbank.filestorage.utils.KeycloakAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TokenControllerTest {

    private KeycloakAuth keycloakAuth;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        keycloakAuth = mock(KeycloakAuth.class);

        TokenController controller = new TokenController(keycloakAuth);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void shouldRejectMissingUsernameAndPassword() throws Exception {
        mockMvc.perform(post("/public/api/v1/auth-token")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(keycloakAuth);
    }

    @Test
    void shouldRejectMissingPassword() throws Exception {
        mockMvc.perform(post("/public/api/v1/auth-token")
                        .contentType("application/json")
                        .content("{\"username\":\"brian\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(keycloakAuth);
    }

    @Test
    void shouldRejectBlankUsername() throws Exception {
        mockMvc.perform(post("/public/api/v1/auth-token")
                        .contentType("application/json")
                        .content("{\"username\":\" \",\"password\":\"secret\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(keycloakAuth);
    }

    @Test
    void shouldCallKeycloakWhenCredentialsAreProvided() throws Exception {
        when(keycloakAuth.getAuthToken("brian", "secret"))
                .thenReturn(new AccessTokenDto("test-access-token"));

        mockMvc.perform(post("/public/api/v1/auth-token")
                        .contentType("application/json")
                        .content("{\"username\":\"brian\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-access-token"));

        verify(keycloakAuth, times(1))
                .getAuthToken("brian", "secret");
    }
}
