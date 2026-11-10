package com.abcbank.filestorage.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${keycloak.http.connect-timeout:5000}")
    private long connectTimeout;

    @Value("${keycloak.http.read-timeout:5000}")
    private long readTimeout;

    @Value("${keycloak.http.connection-request-timeout:5000}")
    private long connectionRequestTimeout;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS"
                )
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }

    @Bean
    public RestTemplate restTemplate() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofMilliseconds(readTimeout))
                .build();

        var httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        factory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        factory.setConnectionRequestTimeout(
                Duration.ofMillis(connectionRequestTimeout)
        );

        return new RestTemplate(factory);
    }
}
