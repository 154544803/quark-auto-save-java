package com.quark.autosave.model.web;

import java.util.List;

public record StructuredTaskConfigDocument(
    List<EditableAccountView> accounts,
    List<EditableTaskView> tasks,
    TaskConfigDocument advanced
) {
}
