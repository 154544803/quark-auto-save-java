package com.quark.autosave.service;

import com.quark.autosave.config.AppProperties;
import com.quark.autosave.config.TaskConfigLoader;
import com.quark.autosave.model.config.TaskDefinition;
import com.quark.autosave.model.config.TaskFileConfig;
import com.quark.autosave.model.runtime.TaskExecutionSummary;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ApplicationRunnerService {

    private final AppProperties appProperties;
    private final TaskConfigLoader taskConfigLoader;
    private final TaskExecutionService taskExecutionService;

    public ApplicationRunnerService(AppProperties appProperties,
                                    TaskConfigLoader taskConfigLoader,
                                    TaskExecutionService taskExecutionService) {
        this.appProperties = appProperties;
        this.taskConfigLoader = taskConfigLoader;
        this.taskExecutionService = taskExecutionService;
    }

    public List<String> listTaskNames() {
        return loadTaskFile().getTasks().stream().map(TaskDefinition::getName).collect(Collectors.toList());
    }

    public TaskExecutionSummary runAllOnce() {
        return taskExecutionService.executeAll(loadTaskFile(), LocalDate.now());
    }

    public TaskExecutionSummary runSingle(String taskName) {
        TaskFileConfig taskFileConfig = loadTaskFile();
        TaskFileConfig filteredConfig = new TaskFileConfig();
        filteredConfig.setAccounts(taskFileConfig.getAccounts());
        filteredConfig.setTasks(taskFileConfig.getTasks().stream()
            .filter(task -> taskName.equals(task.getName()))
            .toList());
        return taskExecutionService.executeAll(filteredConfig, LocalDate.now());
    }

    private TaskFileConfig loadTaskFile() {
        return taskConfigLoader.load(Path.of(appProperties.getTaskFile()));
    }
}
