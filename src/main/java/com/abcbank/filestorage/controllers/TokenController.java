package com.abcbank.filestorage.controllers;


import com.abcbank.filestorage.dto.AccessTokenDto;
import com.abcbank.filestorage.dto.UserPassDto;
import com.abcbank.filestorage.exceptions.AuthenticationFailedException;
import com.abcbank.filestorage.utils.KeycloakAuth;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@AllArgsConstructor
@RestController
@Slf4j
@RequestMapping("/public/api/v1")
public class TokenController {

    private final KeycloakAuth keycloakAuth;

    @PostMapping("/auth-token")
    public ResponseEntity<AccessTokenDto> getAuthToken(@RequestBody UserPassDto userPass) {
        AccessTokenDto token = keycloakAuth.getAuthToken(userPass.getUsername(), userPass.getPassword());
        return ResponseEntity.ok(token);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<Map<String, String>> handleAuthFailure(AuthenticationFailedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
    }
}
