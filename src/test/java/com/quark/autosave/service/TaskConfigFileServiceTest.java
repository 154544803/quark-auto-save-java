package com.quark.autosave.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quark.autosave.config.AppProperties;
import com.quark.autosave.config.TaskConfigLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

class TaskConfigFileServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReturnRawYamlAndParsedSummary() throws Exception {
        Path taskFile = tempDir.resolve("tasks.yml");
        Files.writeString(taskFile, """
            accounts:
              - name: primary
                cookie: demo-cookie

            tasks:
              - name: demo-task
                account: primary
                share-url: https://pan.quark.cn/s/demo
                save-path: /demo
                enabled: true
            """, StandardCharsets.UTF_8);
        AppProperties appProperties = new AppProperties();
        appProperties.setTaskFile(taskFile.toString());
        TaskConfigFileService service =
            new TaskConfigFileService(appProperties, new TaskConfigLoader(new MockEnvironment()));

        assertThat(service.readCurrentConfig().rawYaml()).contains("accounts:");
        assertThat(service.readCurrentConfig().taskNames()).contains("demo-task");
    }

    @Test
    void shouldRejectInvalidYamlWithoutOverwritingSourceFile() throws Exception {
        Path taskFile = tempDir.resolve("tasks.yml");
        Files.writeString(taskFile, "accounts: []\n", StandardCharsets.UTF_8);
        AppProperties appProperties = new AppProperties();
        appProperties.setTaskFile(taskFile.toString());
        TaskConfigFileService service =
            new TaskConfigFileService(appProperties, new TaskConfigLoader(new MockEnvironment()));

        assertThatThrownBy(() -> service.save("""
            accounts:
              - name: broken
            tasks: [
            """))
            .isInstanceOf(IllegalStateException.class);

        assertThat(Files.readString(taskFile, StandardCharsets.UTF_8)).isEqualTo("accounts: []\n");
    }
}
