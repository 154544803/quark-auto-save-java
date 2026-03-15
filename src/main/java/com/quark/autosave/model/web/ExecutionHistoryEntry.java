package com.quark.autosave.model.web;

import com.quark.autosave.model.runtime.TaskExecutionSummary;
import java.time.LocalDateTime;

public record ExecutionHistoryEntry(
    LocalDateTime recordedAt,
    String trigger,
    TaskExecutionSummary summary
) {
}
