package com.quark.autosave.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "app.web-console.username=admin",
    "app.web-console.password=secret"
})
@AutoConfigureMockMvc
class WebConsoleAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRejectAnonymousApiRequest() throws Exception {
        mockMvc.perform(post("/api/tasks/run"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCreateSessionAfterSuccessfulLogin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"admin","password":"secret"}
                    """))
            .andExpect(status().isOk())
            .andExpect(request().sessionAttribute("webConsoleUser", "admin"))
            .andExpect(jsonPath("$.authenticated").value(true));
    }
}
