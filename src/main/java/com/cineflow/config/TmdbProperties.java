package com.cineflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@Getter
@Setter
@ConfigurationProperties(prefix = "tmdb")
public class TmdbProperties {

    private static final String DEFAULT_LANGUAGE = "ko-KR";

    private String baseUrl;
    private String imageBaseUrl;
    private String language = DEFAULT_LANGUAGE;
    private String bearerToken;

    public String resolveBaseUrl() {
        return trimToNull(baseUrl);
    }

    public String resolveImageBaseUrl() {
        return trimToNull(imageBaseUrl);
    }

    public String resolveLanguage() {
        String trimmedLanguage = trimToNull(language);
        return trimmedLanguage != null ? trimmedLanguage : DEFAULT_LANGUAGE;
    }

    public String resolveBearerToken() {
        return trimToNull(bearerToken);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
