package com.quark.autosave.support;

import com.quark.autosave.model.config.TaskDefinition;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TaskScheduleDecider {

    public boolean shouldRun(TaskDefinition task, LocalDate currentDate) {
        if (!task.isEnabled()) {
            return false;
        }
        if (task.getEndDate() != null && currentDate.isAfter(task.getEndDate())) {
            return false;
        }
        List<Integer> runWeek = task.getRunWeek();
        if (runWeek == null || runWeek.isEmpty()) {
            return true;
        }
        // 与原项目保持一致，星期一到星期日映射为 1 到 7。
        int currentWeekday = currentDate.getDayOfWeek().getValue();
        return runWeek.contains(currentWeekday);
    }
}
