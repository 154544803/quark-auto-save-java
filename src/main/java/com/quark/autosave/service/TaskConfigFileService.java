package com.quark.autosave.service;

import com.quark.autosave.config.AppProperties;
import com.quark.autosave.config.TaskConfigLoader;
import com.quark.autosave.model.config.TaskDefinition;
import com.quark.autosave.model.config.TaskFileConfig;
import com.quark.autosave.model.web.TaskConfigDocument;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TaskConfigFileService {

    private final AppProperties appProperties;
    private final TaskConfigLoader taskConfigLoader;

    public TaskConfigFileService(AppProperties appProperties, TaskConfigLoader taskConfigLoader) {
        this.appProperties = appProperties;
        this.taskConfigLoader = taskConfigLoader;
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

    public TaskConfigDocument save(String rawYaml) {
        Path taskFilePath = resolveTaskFilePath();
        Path parentPath = taskFilePath.getParent() == null ? Path.of(".") : taskFilePath.getParent();
        Path tempFile = null;
        try {
            Files.createDirectories(parentPath);
            tempFile = Files.createTempFile(parentPath, "tasks-", ".yml");
            Files.writeString(tempFile, rawYaml, StandardCharsets.UTF_8);
            taskConfigLoader.load(tempFile);
            Files.move(tempFile, taskFilePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return readCurrentConfig();
        } catch (IOException exception) {
            throw new IllegalStateException("保存任务配置文件失败: " + taskFilePath, exception);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // 清理临时文件失败不影响主流程结果。
                }
            }
        }
    }

    private Path resolveTaskFilePath() {
        return Path.of(appProperties.getTaskFile());
    }
}
