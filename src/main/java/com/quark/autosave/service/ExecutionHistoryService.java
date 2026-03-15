package com.quark.autosave.service;

import com.quark.autosave.model.runtime.TaskExecutionSummary;
import com.quark.autosave.model.web.ExecutionHistoryEntry;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ExecutionHistoryService {

    private static final int MAX_ENTRIES = 10;

    private final LinkedList<ExecutionHistoryEntry> entries = new LinkedList<>();

    public synchronized void record(String trigger, TaskExecutionSummary summary) {
        entries.addFirst(new ExecutionHistoryEntry(LocalDateTime.now(), trigger, summary));
        while (entries.size() > MAX_ENTRIES) {
            entries.removeLast();
        }
    }

    public synchronized List<ExecutionHistoryEntry> listRecent() {
        return new ArrayList<>(entries);
    }
}
