package com.quark.autosave.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quark.autosave.model.web.TaskView;
import com.quark.autosave.model.runtime.TaskExecutionItem;
import com.quark.autosave.model.runtime.TaskExecutionSummary;
import com.quark.autosave.service.ApplicationRunnerService;
import com.quark.autosave.service.ExecutionGuardService;
import com.quark.autosave.service.ExecutionHistoryService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TaskControllerTest {

    private MockMvc mockMvc;
    private ApplicationRunnerService applicationRunnerService;
    private ExecutionGuardService executionGuardService;
    private ExecutionHistoryService executionHistoryService;

    @BeforeEach
    void setUp() {
        applicationRunnerService = Mockito.mock(ApplicationRunnerService.class);
        executionGuardService = Mockito.mock(ExecutionGuardService.class);
        executionHistoryService = Mockito.mock(ExecutionHistoryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
            new TaskController(applicationRunnerService, executionGuardService, executionHistoryService)
        ).build();
    }

    @Test
    void shouldReturnStructuredTasks() throws Exception {
        when(applicationRunnerService.listTasks()).thenReturn(List.of(
            new TaskView("task-1", "primary", "/动漫/任务1", true, List.of(1, 3), null),
            new TaskView("task-2", "backup", "/动漫/任务2", false, List.of(), null)
        ));

        mockMvc.perform(get("/api/tasks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].name").value("task-1"))
            .andExpect(jsonPath("$[0].account").value("primary"))
            .andExpect(jsonPath("$[0].savePath").value("/动漫/任务1"))
            .andExpect(jsonPath("$[0].enabled").value(true));
    }

    @Test
    void shouldRunAllTasks() throws Exception {
        when(executionGuardService.tryAcquire()).thenReturn(true);
        TaskExecutionSummary summary = buildSummary();
        when(applicationRunnerService.runAllOnce()).thenReturn(summary);

        mockMvc.perform(post("/api/tasks/run"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.successCount").value(1))
            .andExpect(jsonPath("$.failureCount").value(0))
            .andExpect(jsonPath("$.skipCount").value(0));

        verify(executionHistoryService).record("ALL", summary);
    }

    @Test
    void shouldRunSingleTask() throws Exception {
        when(executionGuardService.tryAcquire()).thenReturn(true);
        when(applicationRunnerService.runSingle("task-1")).thenReturn(buildSummary());

        mockMvc.perform(post("/api/tasks/run/task-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.successCount").value(1));
    }

    @Test
    void shouldReturnConflictWhenExecutionIsAlreadyRunning() throws Exception {
        when(executionGuardService.tryAcquire()).thenReturn(false);

        mockMvc.perform(post("/api/tasks/run"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("任务正在执行中，请稍后再试"));
    }

    private TaskExecutionSummary buildSummary() {
        TaskExecutionSummary summary = new TaskExecutionSummary();
        summary.addItem(TaskExecutionItem.success("task-1", "执行成功"));
        return summary;
    }
}
