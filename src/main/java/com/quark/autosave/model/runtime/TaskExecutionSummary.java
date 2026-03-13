package com.quark.autosave.model.runtime;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TaskExecutionSummary {

    private LocalDateTime startTime = LocalDateTime.now();
    private LocalDateTime endTime;
    private List<TaskExecutionItem> items = new ArrayList<>();

    public void addItem(TaskExecutionItem item) {
        items.add(item);
    }

    public int getSuccessCount() {
        return (int) items.stream().filter(item -> "SUCCESS".equals(item.getStatus())).count();
    }

    public int getFailureCount() {
        return (int) items.stream().filter(item -> "FAILURE".equals(item.getStatus())).count();
    }

    public int getSkipCount() {
        return (int) items.stream().filter(item -> "SKIPPED".equals(item.getStatus())).count();
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public List<TaskExecutionItem> getItems() {
        return items;
    }

    public void setItems(List<TaskExecutionItem> items) {
        this.items = items;
    }
}
