package com.quark.autosave.model.runtime;

public class TaskExecutionItem {

    private String taskName;
    private String status;
    private String message;

    public static TaskExecutionItem success(String taskName, String message) {
        return of(taskName, "SUCCESS", message);
    }

    public static TaskExecutionItem failure(String taskName, String message) {
        return of(taskName, "FAILURE", message);
    }

    public static TaskExecutionItem skipped(String taskName, String message) {
        return of(taskName, "SKIPPED", message);
    }

    private static TaskExecutionItem of(String taskName, String status, String message) {
        TaskExecutionItem item = new TaskExecutionItem();
        item.setTaskName(taskName);
        item.setStatus(status);
        item.setMessage(message);
        return item;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
