package com.quark.autosave.service;

import com.quark.autosave.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class StartupModeRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartupModeRunner.class);

    private final AppProperties appProperties;
    private final ApplicationRunnerService applicationRunnerService;
    private final ConfigurableApplicationContext applicationContext;

    public StartupModeRunner(AppProperties appProperties,
                             ApplicationRunnerService applicationRunnerService,
                             ConfigurableApplicationContext applicationContext) {
        this.appProperties = appProperties;
        this.applicationRunnerService = applicationRunnerService;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        if ("once".equalsIgnoreCase(appProperties.getRunMode())) {
            LOGGER.info("检测到 once 模式，开始执行单次任务");
            applicationRunnerService.runAllOnce();
            SpringApplication.exit(applicationContext, () -> 0);
            return;
        }
        if (appProperties.isStartupRun()) {
            LOGGER.info("检测到启动即执行配置，开始执行一次任务");
            applicationRunnerService.runAllOnce();
        }
    }
}
