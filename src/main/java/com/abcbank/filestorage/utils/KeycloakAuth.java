package com.abcbank.filestorage.utils;

import com.abcbank.filestorage.dto.AccessTokenDto;
import com.abcbank.filestorage.dto.AuthTokenResponse;
import com.abcbank.filestorage.exceptions.AuthenticationFailedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class KeycloakAuth {

    @Value("${config.params.keycloak.url}")
    private String keyCloakEndpoint;

    @Value("${config.params.keycloak.client-id}")
    private String keyCloakClientId;

    private final RestTemplate restTemplate = new RestTemplate();

    public AccessTokenDto getAuthToken(String username, String password) {
        log.info("Getting keycloak access token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", keyCloakClientId);
        map.add("username", username);
        map.add("password", password);
        map.add("grant_type", "password");

        try {
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<AuthTokenResponse> response =
                restTemplate.postForEntity(keyCloakEndpoint, request, AuthTokenResponse.class);

            AuthTokenResponse body = response.getBody();
            if (body == null || body.getAccessToken() == null) {
                throw new AuthenticationFailedException("Keycloak returned empty token response");
            }

            return new AccessTokenDto(body.getAccessToken());
        } catch (AuthenticationFailedException e) {
            throw e;
        } catch(Exception e) {
            log.error("Failed to get auth token from Keycloak", e);
            throw new AuthenticationFailedException("Authentication failed: " + e.getMessage());
        }
    }
}
