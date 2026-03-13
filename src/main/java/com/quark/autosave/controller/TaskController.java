package com.quark.autosave.controller;

import com.quark.autosave.model.runtime.TaskExecutionSummary;
import com.quark.autosave.service.ApplicationRunnerService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final ApplicationRunnerService applicationRunnerService;

    public TaskController(ApplicationRunnerService applicationRunnerService) {
        this.applicationRunnerService = applicationRunnerService;
    }

    @GetMapping
    public List<String> listTasks() {
        return applicationRunnerService.listTaskNames();
    }

    @PostMapping("/run")
    public TaskExecutionSummary runAll() {
        return applicationRunnerService.runAllOnce();
    }

    @PostMapping("/run/{taskName}")
    public TaskExecutionSummary runSingle(@PathVariable String taskName) {
        return applicationRunnerService.runSingle(taskName);
    }
}
