package com.quark.autosave.model.web;

import java.util.List;

public record TaskConfigDocument(String rawYaml, List<String> taskNames) {
}
