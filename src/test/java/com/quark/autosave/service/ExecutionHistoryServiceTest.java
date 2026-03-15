package com.quark.autosave.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.quark.autosave.model.runtime.TaskExecutionItem;
import com.quark.autosave.model.runtime.TaskExecutionSummary;
import org.junit.jupiter.api.Test;

class ExecutionHistoryServiceTest {

    @Test
    void shouldStoreNewestHistoryEntryFirst() {
        ExecutionHistoryService historyService = new ExecutionHistoryService();
        TaskExecutionSummary first = buildSummary("task-1");
        TaskExecutionSummary second = buildSummary("task-2");

        historyService.record("ALL", first);
        historyService.record("task-2", second);

        assertThat(historyService.listRecent())
            .extracting(entry -> entry.trigger())
            .containsExactly("task-2", "ALL");
    }

    private TaskExecutionSummary buildSummary(String taskName) {
        TaskExecutionSummary summary = new TaskExecutionSummary();
        summary.addItem(TaskExecutionItem.success(taskName, "success"));
        return summary;
    }
}
