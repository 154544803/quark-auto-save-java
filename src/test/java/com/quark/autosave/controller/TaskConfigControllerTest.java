package com.quark.autosave.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quark.autosave.model.web.TaskConfigDocument;
import com.quark.autosave.service.TaskConfigFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TaskConfigControllerTest {

    private MockMvc mockMvc;
    private TaskConfigFileService taskConfigFileService;

    @BeforeEach
    void setUp() {
        taskConfigFileService = Mockito.mock(TaskConfigFileService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new TaskConfigController(taskConfigFileService)).build();
    }

    @Test
    void shouldReturnCurrentTaskConfig() throws Exception {
        when(taskConfigFileService.readCurrentConfig())
            .thenReturn(new TaskConfigDocument("accounts: []", java.util.List.of("demo-task")));

        mockMvc.perform(get("/api/config/tasks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rawYaml").value("accounts: []"))
            .andExpect(jsonPath("$.taskNames[0]").value("demo-task"));
    }

    @Test
    void shouldSaveTaskConfig() throws Exception {
        when(taskConfigFileService.save("accounts: []"))
            .thenReturn(new TaskConfigDocument("accounts: []", java.util.List.of()));

        mockMvc.perform(put("/api/config/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"rawYaml":"accounts: []"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rawYaml").value("accounts: []"));
    }
}
