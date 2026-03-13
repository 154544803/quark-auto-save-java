package com.quark.autosave.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quark.autosave.model.config.TaskDefinition;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TaskScheduleDeciderTest {

    private final TaskScheduleDecider decider = new TaskScheduleDecider();

    @Test
    void shouldAllowTaskWithoutAnyTimeRestriction() {
        TaskDefinition task = new TaskDefinition();
        task.setEnabled(true);

        assertTrue(decider.shouldRun(task, LocalDate.of(2026, 3, 13)));
    }

    @Test
    void shouldRejectTaskWhenEndDateExpired() {
        TaskDefinition task = new TaskDefinition();
        task.setEnabled(true);
        task.setEndDate(LocalDate.of(2026, 3, 12));

        assertFalse(decider.shouldRun(task, LocalDate.of(2026, 3, 13)));
    }

    @Test
    void shouldRejectTaskWhenWeekdayDoesNotMatch() {
        TaskDefinition task = new TaskDefinition();
        task.setEnabled(true);
        task.setRunWeek(List.of(1, 3, 7));

        assertFalse(decider.shouldRun(task, LocalDate.of(2026, 3, 13)));
    }

    @Test
    void shouldAllowTaskWhenWeekdayMatches() {
        TaskDefinition task = new TaskDefinition();
        task.setEnabled(true);
        task.setRunWeek(List.of(5));

        assertTrue(decider.shouldRun(task, LocalDate.of(2026, 3, 13)));
    }
}
