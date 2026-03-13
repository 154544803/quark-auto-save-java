package com.quark.autosave.service;

import com.quark.autosave.model.runtime.TaskExecutionSummary;

public interface MailNotificationService {

    void sendSummary(TaskExecutionSummary summary);
}
