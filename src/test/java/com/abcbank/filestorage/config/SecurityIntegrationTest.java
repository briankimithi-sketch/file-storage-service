package com.abcbank.filestorage.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.authentication.AuthenticationServiceException;


import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    private Jwt validJwt() {
        return Jwt.withTokenValue("valid-test-token")
                .header("alg", "RS256")
                .subject("development-test-user")
                .claim(
                        "iss",
                        "https://keycloakuat.abcthebank.com:8099/realms/FileStorage"
                )
                .build();
    }

    @Test
    void publicHomeEndpointShouldBeAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    void healthEndpointShouldRejectMissingToken() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fileEndpointShouldRejectMissingToken() throws Exception {
        mockMvc.perform(get("/files/download/sample.txt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthEndpointShouldAcceptValidJwt() throws Exception {
        when(jwtDecoder.decode("valid-test-token"))
                .thenReturn(validJwt());

        mockMvc.perform(
                        get("/health")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer valid-test-token"
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    void healthEndpointShouldRejectInvalidJwt() {
        when(jwtDecoder.decode("invalid-test-token"))
                .thenThrow(new JwtException("Invalid test token"));

        org.junit.jupiter.api.Assertions.assertThrows(
                AuthenticationServiceException.class,
                () -> mockMvc.perform(
                        get("/health")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer invalid-test-token"
                                )
                )
        );
    }
}
