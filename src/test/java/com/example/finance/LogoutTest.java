package com.example.finance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class, properties = {
    "spring.config.import=",
    "spring.security.oauth2.client.registration.google.client-id=test-client",
    "spring.security.oauth2.client.registration.google.client-secret=test-secret"
})
@Import(SecurityConfig.class)
class LogoutTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @Test
    void logoutInvalidatesSessionAndReturnsJsonWithoutRedirect() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            new SecurityContextImpl(new UsernamePasswordAuthenticationToken(
                "test-user", null, Collections.emptyList())));

        mvc.perform(post("/api/auth/logout").session(session))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.message").value("登出成功"))
            .andExpect(header().doesNotExist("Location"));

        assertTrue(session.isInvalid());
    }

    @Test
    void anonymousLogoutAlsoSucceeds() throws Exception {
        mvc.perform(post("/api/auth/logout"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("登出成功"));
    }
}
