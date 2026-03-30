package com.quark.autosave.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quark.autosave.model.web.EditableAccountView;
import com.quark.autosave.model.web.EditableTaskView;
import com.quark.autosave.model.web.TaskConfigDocument;
import com.quark.autosave.model.web.StructuredTaskConfigDocument;
import com.quark.autosave.service.TaskConfigFileService;
import java.util.List;
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
        when(taskConfigFileService.readStructuredConfig())
            .thenReturn(new StructuredTaskConfigDocument(
                List.of(new EditableAccountView("primary", "cookie", true, 1)),
                List.of(new EditableTaskView(
                    "demo-task",
                    "primary",
                    "https://pan.quark.cn/s/demo",
                    "/demo",
                    ".*",
                    "",
                    true,
                    false,
                    List.of(1, 3, 5),
                    null)),
                new TaskConfigDocument("accounts: []", List.of("demo-task"))));

        mockMvc.perform(get("/api/config/tasks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accounts[0].name").value("primary"))
            .andExpect(jsonPath("$.tasks[0].name").value("demo-task"))
            .andExpect(jsonPath("$.advanced.rawYaml").value("accounts: []"));
    }

    @Test
    void shouldSaveStructuredTaskConfig() throws Exception {
        when(taskConfigFileService.saveStructured(Mockito.any()))
            .thenReturn(new StructuredTaskConfigDocument(
                List.of(new EditableAccountView("primary", "cookie", true, 1)),
                List.of(new EditableTaskView(
                    "demo-task",
                    "primary",
                    "https://pan.quark.cn/s/demo",
                    "/folder",
                    ".*",
                    "",
                    true,
                    false,
                    List.of(1, 3),
                    null)),
                new TaskConfigDocument("accounts: []", List.of("demo-task"))));

        mockMvc.perform(put("/api/config/tasks/structured")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"accounts":[{"name":"primary","cookie":"cookie"}],
                     "tasks":[{"name":"demo-task","account":"primary","shareUrl":"https://pan.quark.cn/s/demo","savePath":"/folder","pattern":".*","replace":"","enabled":true,"ignoreExtension":false,"runWeek":[1,3],"endDate":null}]}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tasks[0].savePath").value("/folder"));
    }

    @Test
    void shouldSaveAdvancedTaskConfig() throws Exception {
        when(taskConfigFileService.save("accounts: []"))
            .thenReturn(new TaskConfigDocument("accounts: []", List.of()));

        mockMvc.perform(put("/api/config/tasks/advanced")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"rawYaml":"accounts: []"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rawYaml").value("accounts: []"));
    }
}
