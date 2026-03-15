package com.quark.autosave.model.web;

import java.util.List;

public record SaveStructuredTaskConfigRequest(
    List<EditableAccountRequest> accounts,
    List<EditableTaskRequest> tasks
) {
}
