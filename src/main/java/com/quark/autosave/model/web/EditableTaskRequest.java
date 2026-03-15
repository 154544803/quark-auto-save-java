package com.quark.autosave.model.web;

import java.time.LocalDate;
import java.util.List;

public record EditableTaskRequest(
    String name,
    String account,
    String shareUrl,
    String savePath,
    String pattern,
    String replace,
    boolean enabled,
    boolean ignoreExtension,
    List<Integer> runWeek,
    LocalDate endDate
) {
}
