package com.quark.autosave.controller;

import com.quark.autosave.model.web.ExecutionHistoryEntry;
import com.quark.autosave.service.ExecutionHistoryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/history")
public class ExecutionHistoryController {

    private final ExecutionHistoryService executionHistoryService;

    public ExecutionHistoryController(ExecutionHistoryService executionHistoryService) {
        this.executionHistoryService = executionHistoryService;
    }

    @GetMapping
    public List<ExecutionHistoryEntry> listRecentHistory() {
        return executionHistoryService.listRecent();
    }
}
