package com.quark.autosave.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.quark.autosave.config.AppProperties;
import com.quark.autosave.config.TaskConfigLoader;
import com.quark.autosave.model.config.AccountConfig;
import com.quark.autosave.model.config.TaskDefinition;
import com.quark.autosave.model.config.TaskFileConfig;
import com.quark.autosave.model.web.EditableAccountRequest;
import com.quark.autosave.model.web.EditableAccountView;
import com.quark.autosave.model.web.EditableTaskRequest;
import com.quark.autosave.model.web.EditableTaskView;
import com.quark.autosave.model.web.SaveStructuredTaskConfigRequest;
import com.quark.autosave.model.web.StructuredTaskConfigDocument;
import com.quark.autosave.model.web.TaskConfigDocument;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class TaskConfigFileService {

    private final AppProperties appProperties;
    private final TaskConfigLoader taskConfigLoader;
    private final ObjectMapper yamlMapper;

    public TaskConfigFileService(AppProperties appProperties, TaskConfigLoader taskConfigLoader) {
        this.appProperties = appProperties;
        this.taskConfigLoader = taskConfigLoader;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.yamlMapper.registerModule(new JavaTimeModule());
        this.yamlMapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
    }

    public TaskConfigDocument readCurrentConfig() {
        Path taskFilePath = resolveTaskFilePath();
        try {
            String rawYaml = Files.readString(taskFilePath, StandardCharsets.UTF_8);
            TaskFileConfig taskFileConfig = taskConfigLoader.load(taskFilePath);
            List<String> taskNames = taskFileConfig.getTasks().stream().map(TaskDefinition::getName).toList();
            return new TaskConfigDocument(rawYaml, taskNames);
        } catch (IOException exception) {
            throw new IllegalStateException("读取任务配置文件失败: " + taskFilePath, exception);
        }
    }

    public StructuredTaskConfigDocument readStructuredConfig() {
        TaskConfigDocument advanced = readCurrentConfig();
        TaskFileConfig taskFileConfig = taskConfigLoader.load(resolveTaskFilePath());
        List<EditableAccountView> accounts = taskFileConfig.getAccounts().stream()
            .map(account -> new EditableAccountView(
                account.getName(),
                account.getCookie(),
                !isBlank(account.getCookie()),
                countTasksForAccount(taskFileConfig, account.getName())))
            .toList();
        List<EditableTaskView> tasks = taskFileConfig.getTasks().stream()
            .map(task -> new EditableTaskView(
                task.getName(),
                task.getAccount(),
                task.getShareUrl(),
                task.getSavePath(),
                task.getPattern(),
                task.getReplace(),
                task.isEnabled(),
                task.isIgnoreExtension(),
                List.copyOf(task.getRunWeek()),
                task.getEndDate()))
            .toList();
        return new StructuredTaskConfigDocument(accounts, tasks, advanced);
    }

    public TaskConfigDocument save(String rawYaml) {
        writeValidatedYaml(rawYaml);
        return readCurrentConfig();
    }

    public StructuredTaskConfigDocument saveStructured(SaveStructuredTaskConfigRequest request) {
        validateStructuredRequest(request);
        TaskFileConfig fileConfig = toTaskFileConfig(request);
        try {
            String rawYaml = yamlMapper.writeValueAsString(fileConfig);
            writeValidatedYaml(rawYaml);
            return readStructuredConfig();
        } catch (IOException exception) {
            throw new IllegalStateException("生成任务配置失败", exception);
        }
    }

    private void writeValidatedYaml(String rawYaml) {
        Path taskFilePath = resolveTaskFilePath();
        Path parentPath = taskFilePath.getParent() == null ? Path.of(".") : taskFilePath.getParent();
        Path tempFile = null;
        try {
            Files.createDirectories(parentPath);
            tempFile = Files.createTempFile(parentPath, "tasks-", ".yml");
            Files.writeString(tempFile, rawYaml, StandardCharsets.UTF_8);
            taskConfigLoader.load(tempFile);
            Files.move(tempFile, taskFilePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            throw new IllegalStateException("保存任务配置文件失败: " + taskFilePath, exception);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // Ignore temp file cleanup failures.
                }
            }
        }
    }

    private TaskFileConfig toTaskFileConfig(SaveStructuredTaskConfigRequest request) {
        TaskFileConfig fileConfig = new TaskFileConfig();
        fileConfig.setAccounts(request.accounts().stream().map(this::toAccountConfig).toList());
        fileConfig.setTasks(request.tasks().stream().map(this::toTaskDefinition).toList());
        return fileConfig;
    }

    private AccountConfig toAccountConfig(EditableAccountRequest request) {
        AccountConfig accountConfig = new AccountConfig();
        accountConfig.setName(request.name());
        accountConfig.setCookie(request.cookie());
        return accountConfig;
    }

    private TaskDefinition toTaskDefinition(EditableTaskRequest request) {
        TaskDefinition taskDefinition = new TaskDefinition();
        taskDefinition.setName(request.name());
        taskDefinition.setAccount(request.account());
        taskDefinition.setShareUrl(request.shareUrl());
        taskDefinition.setSavePath(request.savePath());
        taskDefinition.setPattern(request.pattern());
        taskDefinition.setReplace(request.replace());
        taskDefinition.setEnabled(request.enabled());
        taskDefinition.setIgnoreExtension(request.ignoreExtension());
        taskDefinition.setRunWeek(request.runWeek() == null ? List.of() : List.copyOf(request.runWeek()));
        taskDefinition.setEndDate(request.endDate());
        return taskDefinition;
    }

    private void validateStructuredRequest(SaveStructuredTaskConfigRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("配置内容不能为空");
        }
        if (request.accounts() == null || request.accounts().isEmpty()) {
            throw new IllegalArgumentException("至少需要配置一个账号");
        }
        if (request.tasks() == null) {
            throw new IllegalArgumentException("任务列表不能为空");
        }

        Set<String> accountNames = new LinkedHashSet<>();
        for (EditableAccountRequest account : request.accounts()) {
            if (account == null || isBlank(account.name()) || isBlank(account.cookie())) {
                throw new IllegalArgumentException("账号配置不完整");
            }
            if (!accountNames.add(account.name())) {
                throw new IllegalArgumentException("账号名称不能重复: " + account.name());
            }
        }

        Set<String> taskNames = new LinkedHashSet<>();
        for (EditableTaskRequest task : request.tasks()) {
            if (task == null || isBlank(task.name()) || isBlank(task.shareUrl()) || isBlank(task.savePath())) {
                throw new IllegalArgumentException("任务配置不完整");
            }
            if (!taskNames.add(task.name())) {
                throw new IllegalArgumentException("任务名称不能重复: " + task.name());
            }
            if (!accountNames.contains(task.account())) {
                throw new IllegalArgumentException("任务引用了不存在的账号: " + task.account());
            }
        }
    }

    private int countTasksForAccount(TaskFileConfig taskFileConfig, String accountName) {
        return (int) taskFileConfig.getTasks().stream()
            .filter(task -> Objects.equals(task.getAccount(), accountName))
            .count();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Path resolveTaskFilePath() {
        return Path.of(appProperties.getTaskFile());
    }
}
