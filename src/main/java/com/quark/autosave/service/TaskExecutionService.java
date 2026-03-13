package com.quark.autosave.service;

import com.quark.autosave.model.config.AccountConfig;
import com.quark.autosave.model.config.TaskDefinition;
import com.quark.autosave.model.config.TaskFileConfig;
import com.quark.autosave.model.runtime.TaskExecutionItem;
import com.quark.autosave.model.runtime.TaskExecutionSummary;
import com.quark.autosave.support.TaskScheduleDecider;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class TaskExecutionService {

    private final TaskScheduleDecider taskScheduleDecider;
    private final QuarkTransferService quarkTransferService;
    private final MailNotificationService mailNotificationService;

    public TaskExecutionService(TaskScheduleDecider taskScheduleDecider,
                                QuarkTransferService quarkTransferService,
                                MailNotificationService mailNotificationService) {
        this.taskScheduleDecider = taskScheduleDecider;
        this.quarkTransferService = quarkTransferService;
        this.mailNotificationService = mailNotificationService;
    }

    public TaskExecutionSummary executeAll(TaskFileConfig taskFileConfig, LocalDate currentDate) {
        TaskExecutionSummary summary = new TaskExecutionSummary();
        Map<String, AccountConfig> accountMap = taskFileConfig.getAccounts().stream()
            .collect(Collectors.toMap(AccountConfig::getName, Function.identity()));

        for (TaskDefinition task : taskFileConfig.getTasks()) {
            if (!taskScheduleDecider.shouldRun(task, currentDate)) {
                summary.addItem(TaskExecutionItem.skipped(task.getName(), "任务未到执行时间或已禁用"));
                continue;
            }
            AccountConfig accountConfig = accountMap.get(task.getAccount());
            if (accountConfig == null) {
                summary.addItem(TaskExecutionItem.failure(task.getName(), "未找到任务绑定账号"));
                continue;
            }
            try {
                summary.addItem(quarkTransferService.execute(accountConfig, task));
            } catch (Exception exception) {
                summary.addItem(TaskExecutionItem.failure(task.getName(), exception.getMessage()));
            }
        }
        summary.setEndTime(LocalDateTime.now());
        mailNotificationService.sendSummary(summary);
        return summary;
    }
}
