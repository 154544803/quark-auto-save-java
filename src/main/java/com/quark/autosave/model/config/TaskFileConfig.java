package com.quark.autosave.model.config;

import java.util.ArrayList;
import java.util.List;

public class TaskFileConfig {

    private List<AccountConfig> accounts = new ArrayList<>();
    private List<TaskDefinition> tasks = new ArrayList<>();

    public List<AccountConfig> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<AccountConfig> accounts) {
        this.accounts = accounts;
    }

    public List<TaskDefinition> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskDefinition> tasks) {
        this.tasks = tasks;
    }
}
