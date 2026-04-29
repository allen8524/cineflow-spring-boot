package com.cineflow.controller;

import com.cineflow.dto.SignupRequestDto;
import com.cineflow.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.instanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class AuthSupportControllerPageTest {

    @Mock
    private UserService userService;

    private MockMvc supportMockMvc;
    private MockMvc authMockMvc;

    @BeforeEach
    void setUp() {
        supportMockMvc = MockMvcBuilders.standaloneSetup(new SupportController()).build();
        authMockMvc = MockMvcBuilders.standaloneSetup(new AuthController(userService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void supportRendersSuccessfully() throws Exception {
        supportMockMvc.perform(get("/support"))
                .andExpect(status().isOk())
                .andExpect(view().name("support/index"));
    }

    @Test
    void legacySupportRedirectsToCanonicalRoute() throws Exception {
        supportMockMvc.perform(get("/support.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/support"));
    }

    @Test
    void loginRendersSuccessfully() throws Exception {
        authMockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void signupRendersSuccessfullyWithFormObject() throws Exception {
        authMockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/signup"))
                .andExpect(model().attribute("signupRequest", instanceOf(SignupRequestDto.class)));
    }
}
