package com.quark.autosave.controller;

import com.quark.autosave.model.runtime.TaskExecutionSummary;
import com.quark.autosave.model.web.TaskView;
import com.quark.autosave.service.ApplicationRunnerService;
import com.quark.autosave.service.ExecutionGuardService;
import com.quark.autosave.service.ExecutionHistoryService;
import java.util.Map;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final ApplicationRunnerService applicationRunnerService;
    private final ExecutionGuardService executionGuardService;
    private final ExecutionHistoryService executionHistoryService;

    public TaskController(ApplicationRunnerService applicationRunnerService,
                          ExecutionGuardService executionGuardService,
                          ExecutionHistoryService executionHistoryService) {
        this.applicationRunnerService = applicationRunnerService;
        this.executionGuardService = executionGuardService;
        this.executionHistoryService = executionHistoryService;
    }

    @GetMapping
    public List<TaskView> listTasks() {
        return applicationRunnerService.listTasks();
    }

    @PostMapping("/run")
    public TaskExecutionSummary runAll() {
        if (!executionGuardService.tryAcquire()) {
            throw new IllegalStateException("任务正在执行中，请稍后再试");
        }
        try {
            TaskExecutionSummary summary = applicationRunnerService.runAllOnce();
            executionHistoryService.record("ALL", summary);
            return summary;
        } finally {
            executionGuardService.release();
        }
    }

    @PostMapping("/run/{taskName}")
    public TaskExecutionSummary runSingle(@PathVariable String taskName) {
        if (!executionGuardService.tryAcquire()) {
            throw new IllegalStateException("任务正在执行中，请稍后再试");
        }
        try {
            TaskExecutionSummary summary = applicationRunnerService.runSingle(taskName);
            executionHistoryService.record(taskName, summary);
            return summary;
        } finally {
            executionGuardService.release();
        }
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", exception.getMessage()));
    }
}
