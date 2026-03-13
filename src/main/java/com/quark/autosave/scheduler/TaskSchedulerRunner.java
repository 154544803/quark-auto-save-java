package com.quark.autosave.scheduler;

import com.quark.autosave.config.AppProperties;
import com.quark.autosave.service.ApplicationRunnerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TaskSchedulerRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskSchedulerRunner.class);

    private final AppProperties appProperties;
    private final ApplicationRunnerService applicationRunnerService;

    public TaskSchedulerRunner(AppProperties appProperties, ApplicationRunnerService applicationRunnerService) {
        this.appProperties = appProperties;
        this.applicationRunnerService = applicationRunnerService;
    }

    // 显式指定时区，确保本地和 GitHub Actions 环境都按北京时间调度。
    @Scheduled(cron = "${app.schedule.cron}", zone = "${app.schedule.zone}")
    public void runOnSchedule() {
        if (!appProperties.getSchedule().isEnabled() || "once".equalsIgnoreCase(appProperties.getRunMode())) {
            return;
        }
        LOGGER.info("开始执行定时任务");
        applicationRunnerService.runAllOnce();
    }
}
