package com.quark.autosave.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.quark.autosave.model.config.AccountConfig;
import com.quark.autosave.model.config.TaskDefinition;
import com.quark.autosave.model.config.TaskFileConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class TaskConfigLoader {

    private final ObjectMapper objectMapper;
    private final Environment environment;

    @Autowired
    public TaskConfigLoader(Environment environment) {
        this(new YAMLFactory(), environment);
    }

    TaskConfigLoader(YAMLFactory yamlFactory, Environment environment) {
        this.objectMapper = new ObjectMapper(yamlFactory);
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
        this.environment = environment;
    }

    public TaskFileConfig load(Path taskFile) {
        if (!Files.exists(taskFile)) {
            throw new IllegalStateException("任务配置文件不存在: " + taskFile);
        }
        try {
            // 先用 Spring 环境解析占位符，保证本地和 GitHub Actions 都能共用同一份模板。
            String content = Files.readString(taskFile, StandardCharsets.UTF_8);
            String resolvedContent = environment.resolvePlaceholders(content);
            TaskFileConfig taskFileConfig = objectMapper.readValue(resolvedContent, TaskFileConfig.class);
            validate(taskFileConfig);
            return taskFileConfig;
        } catch (IOException exception) {
            throw new IllegalStateException("读取任务配置文件失败: " + taskFile, exception);
        }
    }

    private void validate(TaskFileConfig taskFileConfig) {
        Set<String> accountNames = new HashSet<>();
        for (AccountConfig account : taskFileConfig.getAccounts()) {
            if (account == null || isBlank(account.getName()) || isBlank(account.getCookie())) {
                throw new IllegalArgumentException("账号配置不完整");
            }
            accountNames.add(account.getName());
        }
        for (TaskDefinition task : taskFileConfig.getTasks()) {
            // 任务在启动阶段尽早校验，避免定时任务运行到一半才暴露配置问题。
            if (task == null || isBlank(task.getName()) || isBlank(task.getShareUrl()) || isBlank(task.getSavePath())) {
                throw new IllegalArgumentException("任务配置不完整");
            }
            if (!accountNames.contains(task.getAccount())) {
                throw new IllegalArgumentException("任务引用了不存在的账号: " + task.getAccount());
            }
        }
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.trim().isEmpty();
    }
}
