package com.quark.autosave.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quark.autosave.config.AppProperties;
import com.quark.autosave.config.TaskConfigLoader;
import com.quark.autosave.model.web.EditableAccountRequest;
import com.quark.autosave.model.web.EditableTaskRequest;
import com.quark.autosave.model.web.SaveStructuredTaskConfigRequest;
import com.quark.autosave.model.web.StructuredTaskConfigDocument;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
    void shouldReturnStructuredAccountsTasksAndAdvancedYaml() throws Exception {
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
                pattern: ".*"
                replace: ""
                enabled: true
                ignore-extension: false
                run-week:
                  - 1
                  - 3
            """, StandardCharsets.UTF_8);
        AppProperties appProperties = new AppProperties();
        appProperties.setTaskFile(taskFile.toString());
        TaskConfigFileService service =
            new TaskConfigFileService(appProperties, new TaskConfigLoader(new MockEnvironment()));

        StructuredTaskConfigDocument document = service.readStructuredConfig();

        assertThat(document.accounts()).extracting("name").contains("primary");
        assertThat(document.tasks()).extracting("name").contains("demo-task");
        assertThat(document.advanced().rawYaml()).contains("accounts:");
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

    @Test
    void shouldSaveStructuredConfigAsYaml() throws Exception {
        Path taskFile = tempDir.resolve("tasks.yml");
        Files.writeString(taskFile, """
            accounts: []
            tasks: []
            """, StandardCharsets.UTF_8);
        AppProperties appProperties = new AppProperties();
        appProperties.setTaskFile(taskFile.toString());
        TaskConfigFileService service =
            new TaskConfigFileService(appProperties, new TaskConfigLoader(new MockEnvironment()));

        SaveStructuredTaskConfigRequest request = new SaveStructuredTaskConfigRequest(
            List.of(new EditableAccountRequest("primary", "cookie-value")),
            List.of(new EditableTaskRequest(
                "demo-task",
                "primary",
                "https://pan.quark.cn/s/demo",
                "/folder",
                ".*",
                "",
                true,
                false,
                List.of(1, 3, 5),
                null))
        );

        StructuredTaskConfigDocument saved = service.saveStructured(request);

        assertThat(saved.tasks()).extracting("savePath").contains("/folder");
        assertThat(Files.readString(taskFile, StandardCharsets.UTF_8)).contains("save-path: \"/folder\"");
    }

    @Test
    void shouldRejectStructuredConfigWithUnknownAccount() throws Exception {
        Path taskFile = tempDir.resolve("tasks.yml");
        Files.writeString(taskFile, """
            accounts: []
            tasks: []
            """, StandardCharsets.UTF_8);
        AppProperties appProperties = new AppProperties();
        appProperties.setTaskFile(taskFile.toString());
        TaskConfigFileService service =
            new TaskConfigFileService(appProperties, new TaskConfigLoader(new MockEnvironment()));

        SaveStructuredTaskConfigRequest request = new SaveStructuredTaskConfigRequest(
            List.of(new EditableAccountRequest("primary", "cookie-value")),
            List.of(new EditableTaskRequest(
                "demo-task",
                "missing",
                "https://pan.quark.cn/s/demo",
                "/folder",
                ".*",
                "",
                true,
                false,
                List.of(),
                null))
        );

        assertThatThrownBy(() -> service.saveStructured(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("账号");
    }
}
