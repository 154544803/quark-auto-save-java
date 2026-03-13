package com.quark.autosave.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.quark.autosave.config.AppProperties;
import com.quark.autosave.model.runtime.TaskExecutionItem;
import com.quark.autosave.model.runtime.TaskExecutionSummary;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class MailNotificationServiceReceiverTest {

    @Test
    void shouldSendMailToConfiguredReceiver() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        DefaultMailNotificationService service = new DefaultMailNotificationService(provider(mailSender), buildProperties(true));
        TaskExecutionSummary summary = new TaskExecutionSummary();
        summary.addItem(TaskExecutionItem.success("task-1", "执行成功"));

        service.sendSummary(summary);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertTrue("receiver@example.com".equals(message.getTo()[0]));
        assertTrue(message.getSubject() != null && message.getSubject().contains("执行结果"));
        assertTrue(message.getText().contains("task-1"));
    }

    private AppProperties buildProperties(boolean enabled) {
        AppProperties properties = new AppProperties();
        properties.getNotification().getMail().setEnabled(enabled);
        properties.getNotification().getMail().setSubjectPrefix("[test]");
        setReceiver(properties, "receiver@example.com");
        return properties;
    }

    private void setReceiver(AppProperties properties, String receiver) {
        try {
            Field receiverField = properties.getNotification().getMail().getClass().getDeclaredField("to");
            receiverField.setAccessible(true);
            receiverField.set(properties.getNotification().getMail(), receiver);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("邮件通知缺少收件人配置字段 to", exception);
        }
    }

    private ObjectProvider<JavaMailSender> provider(JavaMailSender mailSender) {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = Mockito.mock(ObjectProvider.class);
        Mockito.when(provider.getIfAvailable()).thenReturn(mailSender);
        return provider;
    }
}
