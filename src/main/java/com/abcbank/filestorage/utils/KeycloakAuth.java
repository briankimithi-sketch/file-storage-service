package com.abcbank.filestorage.utils;

import com.abcbank.filestorage.dto.AccessTokenDto;
import com.abcbank.filestorage.dto.AuthTokenResponse;
import com.abcbank.filestorage.exceptions.AuthenticationFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Slf4j
@Service
public class KeycloakAuth {

    @Value("${config.params.keycloak.url}")
    private String keyCloakEndpoint;

    @Value("${config.params.keycloak.client-id}")
    private String keyCloakClientId;

    private final KeycloakTokenService keycloakTokenService;

    public KeycloakAuth(KeycloakTokenService keycloakTokenService) {
        this.keycloakTokenService = keycloakTokenService;
    }

    public AccessTokenDto getAuthToken(String username, String password) {

        log.info("Requesting Keycloak access token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", keyCloakClientId);
        map.add("username", username);
        map.add("password", password);
        map.add("grant_type", "password");

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(map, headers);

        AuthTokenResponse body = keycloakTokenService.requestToken(
                keyCloakEndpoint,
                request
        );

        if (body == null || body.getAccessToken() == null
                || body.getAccessToken().isBlank()) {
            throw new AuthenticationFailedException(
                    "Keycloak returned an empty token response"
            );
        }

        log.info("Keycloak authentication successful");

        return new AccessTokenDto(body.getAccessToken());
    }
}
