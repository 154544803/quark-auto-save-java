package com.quark.autosave.support;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class RenameRuleEngine {

    public String rename(String pattern, String replace, String originalName) {
        if (replace == null || replace.isBlank()) {
            return originalName;
        }
        if (pattern == null || pattern.isBlank()) {
            return originalName;
        }
        return Pattern.compile(pattern).matcher(originalName).replaceAll(replace);
    }
}
