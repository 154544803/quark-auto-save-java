package com.quark.autosave.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.quark.autosave.model.config.TaskFileConfig;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

class TaskConfigLoaderTest {

    @Test
    void shouldLoadAccountsAndTasksFromYaml() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("QUARK_COOKIE", "cookie-value");
        TaskConfigLoader loader = new TaskConfigLoader(new YAMLFactory(), environment);

        Path taskFile = new ClassPathResource("config/tasks-valid.yml").getFile().toPath();
        TaskFileConfig config = loader.load(taskFile);

        assertNotNull(config);
        assertEquals(1, config.getAccounts().size());
        assertEquals("primary", config.getAccounts().get(0).getName());
        assertEquals("cookie-value", config.getAccounts().get(0).getCookie());
        assertEquals(1, config.getTasks().size());
        assertEquals("demo-task", config.getTasks().get(0).getName());
        assertEquals("primary", config.getTasks().get(0).getAccount());
    }

    @Test
    void shouldThrowWhenTaskFileDoesNotExist() {
        MockEnvironment environment = new MockEnvironment();
        TaskConfigLoader loader = new TaskConfigLoader(new YAMLFactory(), environment);

        assertThrows(IllegalStateException.class, () -> loader.load(Path.of("config/not-found.yml")));
    }

    @Test
    void shouldThrowWhenTaskReferencesUnknownAccount() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        TaskConfigLoader loader = new TaskConfigLoader(new YAMLFactory(), environment);
        Path taskFile = new ClassPathResource("config/tasks-missing-account.yml").getFile().toPath();

        assertThrows(IllegalArgumentException.class, () -> loader.load(taskFile));
    }
}
