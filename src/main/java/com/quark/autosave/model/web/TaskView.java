package com.quark.autosave.model.web;

import java.time.LocalDate;
import java.util.List;

public record TaskView(
    String name,
    String account,
    String savePath,
    boolean enabled,
    List<Integer> runWeek,
    LocalDate endDate
) {
}
