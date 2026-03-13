package com.quark.autosave.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quark.autosave.model.runtime.TaskExecutionItem;
import com.quark.autosave.model.runtime.TaskExecutionSummary;
import com.quark.autosave.service.ApplicationRunnerService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TaskControllerTest {

    private MockMvc mockMvc;
    private ApplicationRunnerService applicationRunnerService;

    @BeforeEach
    void setUp() {
        applicationRunnerService = Mockito.mock(ApplicationRunnerService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new TaskController(applicationRunnerService)).build();
    }

    @Test
    void shouldReturnTaskNames() throws Exception {
        when(applicationRunnerService.listTaskNames()).thenReturn(List.of("task-1", "task-2"));

        mockMvc.perform(get("/api/tasks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0]").value("task-1"));
    }

    @Test
    void shouldRunAllTasks() throws Exception {
        when(applicationRunnerService.runAllOnce()).thenReturn(buildSummary());

        mockMvc.perform(post("/api/tasks/run"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.successCount").value(1))
            .andExpect(jsonPath("$.failureCount").value(0))
            .andExpect(jsonPath("$.skipCount").value(0));
    }

    @Test
    void shouldRunSingleTask() throws Exception {
        when(applicationRunnerService.runSingle("task-1")).thenReturn(buildSummary());

        mockMvc.perform(post("/api/tasks/run/task-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.successCount").value(1));
    }

    private TaskExecutionSummary buildSummary() {
        TaskExecutionSummary summary = new TaskExecutionSummary();
        summary.addItem(TaskExecutionItem.success("task-1", "执行成功"));
        return summary;
    }
}
