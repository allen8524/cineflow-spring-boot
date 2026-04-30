package com.cineflow.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LegacyTemplateControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new LegacyTemplateController()).build();
    }

    @Test
    void legacyMovieListPagesRedirectToMovies() throws Exception {
        mockMvc.perform(get("/moviegrid.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movies"));

        mockMvc.perform(get("/moviegrid_light.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movies"));

        mockMvc.perform(get("/moviegridfw.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movies"));

        mockMvc.perform(get("/moviegridfw_light.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movies"));

        mockMvc.perform(get("/movielist_light.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movies"));
    }

    @Test
    void legacyMovieDetailPageRedirectsUsingIdWhenProvided() throws Exception {
        mockMvc.perform(get("/moviesingle_light.html").param("id", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movies/1"));
    }

    @Test
    void legacyMovieDetailPageRedirectsToMoviesWhenIdMissing() throws Exception {
        mockMvc.perform(get("/moviesingle_light.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movies"));
    }

    @Test
    void legacyHomeAndLandingPagesRedirectToRoot() throws Exception {
        mockMvc.perform(get("/homev2.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        mockMvc.perform(get("/index_light.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        mockMvc.perform(get("/comingsoon.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void unsupportedLegacyPagesReturnNotFound() throws Exception {
        mockMvc.perform(get("/bloggrid.html"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/celebritylist.html"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/userprofile.html"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/seriessingle.html"))
                .andExpect(status().isNotFound());
    }
}
