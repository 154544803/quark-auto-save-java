package com.quark.autosave.service;

import com.quark.autosave.config.AppProperties;
import com.quark.autosave.model.runtime.TaskExecutionItem;
import com.quark.autosave.model.runtime.TaskExecutionSummary;
import java.util.StringJoiner;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class DefaultMailNotificationService implements MailNotificationService {

    private final ObjectProvider<JavaMailSender> javaMailSenderProvider;
    private final AppProperties appProperties;

    public DefaultMailNotificationService(ObjectProvider<JavaMailSender> javaMailSenderProvider,
                                          AppProperties appProperties) {
        this.javaMailSenderProvider = javaMailSenderProvider;
        this.appProperties = appProperties;
    }

    @Override
    public void sendSummary(TaskExecutionSummary summary) {
        if (!appProperties.getNotification().getMail().isEnabled()) {
            return;
        }
        JavaMailSender javaMailSender = javaMailSenderProvider.getIfAvailable();
        if (javaMailSender == null) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject(appProperties.getNotification().getMail().getSubjectPrefix() + " 执行结果");
        message.setText(buildContent(summary));
        javaMailSender.send(message);
    }

    String buildContent(TaskExecutionSummary summary) {
        StringJoiner joiner = new StringJoiner(System.lineSeparator());
        joiner.add("夸克自动转存执行摘要");
        joiner.add("成功任务数: " + summary.getSuccessCount());
        joiner.add("失败任务数: " + summary.getFailureCount());
        joiner.add("跳过任务数: " + summary.getSkipCount());
        joiner.add("");
        // 统一在邮件中展开每个任务详情，便于 GitHub Actions 场景直接查看执行结果。
        for (TaskExecutionItem item : summary.getItems()) {
            joiner.add(item.getStatus() + " | " + item.getTaskName() + " | " + item.getMessage());
        }
        return joiner.toString();
    }
}
