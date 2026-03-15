package com.quark.autosave.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quark.autosave.model.runtime.TaskExecutionSummary;
import com.quark.autosave.model.web.ExecutionHistoryEntry;
import com.quark.autosave.service.ExecutionHistoryService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ExecutionHistoryControllerTest {

    private MockMvc mockMvc;
    private ExecutionHistoryService executionHistoryService;

    @BeforeEach
    void setUp() {
        executionHistoryService = Mockito.mock(ExecutionHistoryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ExecutionHistoryController(executionHistoryService)).build();
    }

    @Test
    void shouldReturnRecentHistory() throws Exception {
        when(executionHistoryService.listRecent()).thenReturn(List.of(
            new ExecutionHistoryEntry(LocalDateTime.of(2026, 3, 15, 11, 0), "ALL", new TaskExecutionSummary())
        ));

        mockMvc.perform(get("/api/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].trigger").value("ALL"));
    }
}
