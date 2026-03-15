package com.quark.autosave.controller;

import com.quark.autosave.model.web.SaveTaskConfigRequest;
import com.quark.autosave.model.web.TaskConfigDocument;
import com.quark.autosave.service.TaskConfigFileService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config/tasks")
public class TaskConfigController {

    private final TaskConfigFileService taskConfigFileService;

    public TaskConfigController(TaskConfigFileService taskConfigFileService) {
        this.taskConfigFileService = taskConfigFileService;
    }

    @GetMapping
    public TaskConfigDocument getCurrentConfig() {
        return taskConfigFileService.readCurrentConfig();
    }

    @PutMapping
    public TaskConfigDocument saveConfig(@RequestBody SaveTaskConfigRequest request) {
        return taskConfigFileService.save(request.rawYaml());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> handleConfigException(RuntimeException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
