package com.abcbank.filestorage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless REST APIs
            .csrf(csrf -> csrf.disable())

            // Define which endpoints are public vs protected
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/files/**",        // all file endpoints are public
                    "/public/api/v1/**",// token fetch endpoint
                    "/v3/api-docs/**",  // OpenAPI docs
                    "/swagger-ui/**"    // Swagger UI
                ).permitAll()
                .anyRequest().authenticated() // everything else requires JWT
            )

            // Enable JWT validation via OAuth2 Resource Server
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }
}
