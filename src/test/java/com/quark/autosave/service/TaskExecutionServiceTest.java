package com.quark.autosave.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quark.autosave.model.config.AccountConfig;
import com.quark.autosave.model.config.TaskDefinition;
import com.quark.autosave.model.config.TaskFileConfig;
import com.quark.autosave.model.runtime.TaskExecutionItem;
import com.quark.autosave.model.runtime.TaskExecutionSummary;
import com.quark.autosave.support.TaskScheduleDecider;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TaskExecutionServiceTest {

    @Test
    void shouldOnlyExecuteEnabledTasksWithinSchedule() {
        QuarkTransferService transferService = Mockito.mock(QuarkTransferService.class);
        MailNotificationService mailNotificationService = Mockito.mock(MailNotificationService.class);
        TaskExecutionService service = new TaskExecutionService(new TaskScheduleDecider(), transferService, mailNotificationService);
        TaskFileConfig config = buildConfig(
            buildTask("task-1", true, null, List.of()),
            buildTask("task-2", false, null, List.of()),
            buildTask("task-3", true, LocalDate.of(2026, 3, 12), List.of())
        );

        when(transferService.execute(any(), any())).thenReturn(TaskExecutionItem.success("task-1", "执行成功"));

        TaskExecutionSummary summary = service.executeAll(config, LocalDate.of(2026, 3, 13));

        verify(transferService, times(1)).execute(any(), any());
        assertEquals(1, summary.getSuccessCount());
        assertEquals(2, summary.getSkipCount());
    }

    @Test
    void shouldContinueWhenSingleTaskFails() {
        QuarkTransferService transferService = Mockito.mock(QuarkTransferService.class);
        MailNotificationService mailNotificationService = Mockito.mock(MailNotificationService.class);
        TaskExecutionService service = new TaskExecutionService(new TaskScheduleDecider(), transferService, mailNotificationService);
        TaskFileConfig config = buildConfig(
            buildTask("task-1", true, null, List.of()),
            buildTask("task-2", true, null, List.of())
        );

        when(transferService.execute(any(), any())).thenReturn(TaskExecutionItem.success("task-1", "执行成功"));
        doThrow(new IllegalStateException("接口异常")).when(transferService).execute(any(AccountConfig.class), any(TaskDefinition.class));

        TaskExecutionSummary summary = service.executeAll(config, LocalDate.of(2026, 3, 13));

        verify(transferService, times(2)).execute(any(), any());
        verify(mailNotificationService, times(1)).sendSummary(summary);
        assertEquals(0, summary.getSuccessCount());
        assertEquals(2, summary.getFailureCount());
    }

    @Test
    void shouldCountSuccessFailureAndSkipSeparately() {
        QuarkTransferService transferService = Mockito.mock(QuarkTransferService.class);
        MailNotificationService mailNotificationService = Mockito.mock(MailNotificationService.class);
        TaskExecutionService service = new TaskExecutionService(new TaskScheduleDecider(), transferService, mailNotificationService);
        TaskFileConfig config = buildConfig(
            buildTask("task-1", true, null, List.of()),
            buildTask("task-2", false, null, List.of()),
            buildTask("task-3", true, null, List.of())
        );

        when(transferService.execute(any(), any()))
            .thenReturn(TaskExecutionItem.success("task-1", "执行成功"))
            .thenReturn(TaskExecutionItem.failure("task-3", "执行失败"));

        TaskExecutionSummary summary = service.executeAll(config, LocalDate.of(2026, 3, 13));

        assertEquals(1, summary.getSuccessCount());
        assertEquals(1, summary.getFailureCount());
        assertEquals(1, summary.getSkipCount());
    }

    private TaskFileConfig buildConfig(TaskDefinition... tasks) {
        AccountConfig account = new AccountConfig();
        account.setName("primary");
        account.setCookie("cookie");

        TaskFileConfig config = new TaskFileConfig();
        config.setAccounts(List.of(account));
        config.setTasks(List.of(tasks));
        return config;
    }

    private TaskDefinition buildTask(String name, boolean enabled, LocalDate endDate, List<Integer> runWeek) {
        TaskDefinition task = new TaskDefinition();
        task.setName(name);
        task.setAccount("primary");
        task.setShareUrl("https://pan.quark.cn/s/demo");
        task.setSavePath("/demo");
        task.setEnabled(enabled);
        task.setEndDate(endDate);
        task.setRunWeek(runWeek);
        return task;
    }
}
