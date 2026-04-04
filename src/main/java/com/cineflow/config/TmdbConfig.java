package com.cineflow.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(TmdbProperties.class)
public class TmdbConfig {

    @Bean
    public RestClient tmdbRestClient(RestClient.Builder builder, TmdbProperties tmdbProperties) {
        RestClient.Builder restClientBuilder = builder
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        if (StringUtils.hasText(tmdbProperties.resolveBaseUrl())) {
            restClientBuilder = restClientBuilder.baseUrl(tmdbProperties.resolveBaseUrl());
        }

        return restClientBuilder.build();
    }
}
