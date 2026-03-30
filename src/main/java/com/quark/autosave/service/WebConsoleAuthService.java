package com.quark.autosave.service;

import com.quark.autosave.config.AppProperties;
import org.springframework.stereotype.Service;

@Service
public class WebConsoleAuthService {

    private final AppProperties appProperties;

    public WebConsoleAuthService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public boolean isEnabled() {
        return appProperties.getWebConsole().isEnabled();
    }

    public boolean isAuthenticated(String username, String password) {
        return appProperties.getWebConsole().getUsername().equals(username)
            && appProperties.getWebConsole().getPassword().equals(password);
    }
}
