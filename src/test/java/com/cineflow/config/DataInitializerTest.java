package com.cineflow.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DataInitializerTest {

    @Test
    void dataInitializerDoesNotContainLegacyFakeMovieTitles() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/cineflow/config/DataInitializer.java"));

        assertThat(source).doesNotContain(
                "시간의 궤도",
                "보이스 노이즈",
                "블랙아웃 시티",
                "심해 항로",
                "리버스 코드",
                "극야의 기록",
                "일리시스"
        );
    }
}
