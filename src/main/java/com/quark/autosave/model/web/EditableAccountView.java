package com.quark.autosave.model.web;

public record EditableAccountView(String name, String cookie, boolean cookieConfigured, int taskCount) {
}
