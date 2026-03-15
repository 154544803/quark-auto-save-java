package com.quark.autosave.service;

import com.quark.autosave.config.AppProperties;
import com.quark.autosave.config.TaskConfigLoader;
import com.quark.autosave.model.config.TaskDefinition;
import com.quark.autosave.model.config.TaskFileConfig;
import com.quark.autosave.model.runtime.TaskExecutionSummary;
import com.quark.autosave.model.web.TaskView;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
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

    public List<TaskView> listTasks() {
        return loadTaskFile().getTasks().stream()
            .map(this::toTaskView)
            .toList();
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

    private TaskView toTaskView(TaskDefinition taskDefinition) {
        return new TaskView(
            taskDefinition.getName(),
            taskDefinition.getAccount(),
            taskDefinition.getSavePath(),
            taskDefinition.isEnabled(),
            taskDefinition.getRunWeek(),
            taskDefinition.getEndDate()
        );
    }
}
