package com.quark.autosave.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.quark.autosave.config.AppProperties;
import com.quark.autosave.model.runtime.TaskExecutionItem;
import com.quark.autosave.model.runtime.TaskExecutionSummary;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

class MailNotificationServiceTest {

    @Test
    void shouldBuildReadableSummaryContent() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        DefaultMailNotificationService service = new DefaultMailNotificationService(provider(mailSender), buildProperties(false));
        TaskExecutionSummary summary = new TaskExecutionSummary();
        summary.addItem(TaskExecutionItem.success("task-1", "执行成功"));
        summary.addItem(TaskExecutionItem.failure("task-2", "接口失败"));

        String content = service.buildContent(summary);

        assertTrue(content.contains("成功任务数: 1"));
        assertTrue(content.contains("失败任务数: 1"));
        assertTrue(content.contains("task-2"));
    }

    @Test
    void shouldSkipSendingWhenMailDisabled() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        DefaultMailNotificationService service = new DefaultMailNotificationService(provider(mailSender), buildProperties(false));

        service.sendSummary(new TaskExecutionSummary());

        verify(mailSender, never()).send(Mockito.any(org.springframework.mail.SimpleMailMessage.class));
    }

    private AppProperties buildProperties(boolean enabled) {
        AppProperties properties = new AppProperties();
        properties.getNotification().getMail().setEnabled(enabled);
        properties.getNotification().getMail().setSubjectPrefix("[test]");
        return properties;
    }

    private ObjectProvider<JavaMailSender> provider(JavaMailSender mailSender) {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = Mockito.mock(ObjectProvider.class);
        Mockito.when(provider.getIfAvailable()).thenReturn(mailSender);
        return provider;
    }
}
