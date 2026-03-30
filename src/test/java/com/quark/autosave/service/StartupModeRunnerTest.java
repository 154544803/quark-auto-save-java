package com.quark.autosave.service;

import static org.mockito.Mockito.verify;

import com.quark.autosave.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

class StartupModeRunnerTest {

    @Test
    void shouldRunOnceModeWithoutDependingOnWebConsoleCredentials() {
        AppProperties appProperties = new AppProperties();
        appProperties.setRunMode("once");
        ApplicationRunnerService applicationRunnerService = Mockito.mock(ApplicationRunnerService.class);
        ConfigurableApplicationContext applicationContext = Mockito.mock(ConfigurableApplicationContext.class);
        StartupModeRunner startupModeRunner =
            new StartupModeRunner(appProperties, applicationRunnerService, applicationContext);

        try (MockedStatic<SpringApplication> springApplication = Mockito.mockStatic(SpringApplication.class)) {
            springApplication.when(() -> SpringApplication.exit(Mockito.same(applicationContext), Mockito.any()))
                .thenReturn(0);

            startupModeRunner.run(new DefaultApplicationArguments(new String[0]));

            verify(applicationRunnerService).runAllOnce();
            springApplication.verify(() -> SpringApplication.exit(Mockito.same(applicationContext), Mockito.any()));
        }
    }
}
