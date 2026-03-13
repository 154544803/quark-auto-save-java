package com.quark.autosave.service;

import com.quark.autosave.model.config.AccountConfig;
import com.quark.autosave.model.config.TaskDefinition;
import com.quark.autosave.model.runtime.TaskExecutionItem;

public interface QuarkTransferService {

    TaskExecutionItem execute(AccountConfig accountConfig, TaskDefinition taskDefinition);
}
