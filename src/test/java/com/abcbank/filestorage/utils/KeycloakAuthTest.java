package com.abcbank.filestorage.utils;

import com.abcbank.filestorage.dto.AccessTokenDto;
import com.abcbank.filestorage.dto.AuthTokenResponse;
import com.abcbank.filestorage.exceptions.AuthenticationFailedException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KeycloakAuthTest {

    private KeycloakTokenService tokenService;
    private KeycloakAuth keycloakAuth;

    private static final String ENDPOINT =
            "http://localhost:8080/token";

    @BeforeEach
    void setUp() {
        tokenService = mock(KeycloakTokenService.class);
        keycloakAuth = new KeycloakAuth(tokenService);

        ReflectionTestUtils.setField(
                keycloakAuth, "keyCloakEndpoint", ENDPOINT
        );

        ReflectionTestUtils.setField(
                keycloakAuth, "keyCloakClientId", "file-storage"
        );
    }

    @Test
    void shouldReturnAccessTokenWhenAuthenticationSucceeds() {
        when(tokenService.requestToken(eq(ENDPOINT), any()))
                .thenReturn(new AuthTokenResponse("test-access-token"));

        AccessTokenDto result =
                keycloakAuth.getAuthToken("brian", "password");

        assertNotNull(result);
        assertEquals("test-access-token", result.getAccessToken());

        verify(tokenService, times(1))
                .requestToken(eq(ENDPOINT), any());
    }

    @Test
    void shouldRejectEmptyTokenResponse() {
        when(tokenService.requestToken(eq(ENDPOINT), any()))
                .thenReturn(new AuthTokenResponse(""));

        assertThrows(
                AuthenticationFailedException.class,
                () -> keycloakAuth.getAuthToken("brian", "password")
        );

        verify(tokenService, times(1))
                .requestToken(eq(ENDPOINT), any());
    }

    @Test
    void shouldRejectNullTokenResponse() {
        when(tokenService.requestToken(eq(ENDPOINT), any()))
                .thenReturn(null);

        assertThrows(
                AuthenticationFailedException.class,
                () -> keycloakAuth.getAuthToken("brian", "password")
        );
    }

    @Test
    void shouldNotRetryClientErrorsInAuthenticationLayer() {
        HttpClientErrorException exception =
                HttpClientErrorException.create(
                        HttpStatus.BAD_REQUEST,
                        "Bad Request",
                        null,
                        null,
                        null
                );

        when(tokenService.requestToken(eq(ENDPOINT), any()))
                .thenThrow(exception);

        assertThrows(
                HttpClientErrorException.class,
                () -> keycloakAuth.getAuthToken(
                        "brian", "wrong-password"
                )
        );

        verify(tokenService, times(1))
                .requestToken(eq(ENDPOINT), any());
    }
}
