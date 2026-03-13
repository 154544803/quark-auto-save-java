package com.quark.autosave.model.config;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TaskDefinition {

    private String name;
    private String account;
    private String shareUrl;
    private String savePath;
    private String pattern;
    private String replace;
    private boolean enabled = true;
    private LocalDate endDate;
    private List<Integer> runWeek = new ArrayList<>();
    private boolean ignoreExtension;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getShareUrl() {
        return shareUrl;
    }

    public void setShareUrl(String shareUrl) {
        this.shareUrl = shareUrl;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getReplace() {
        return replace;
    }

    public void setReplace(String replace) {
        this.replace = replace;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public List<Integer> getRunWeek() {
        return runWeek;
    }

    public void setRunWeek(List<Integer> runWeek) {
        this.runWeek = runWeek;
    }

    public boolean isIgnoreExtension() {
        return ignoreExtension;
    }

    public void setIgnoreExtension(boolean ignoreExtension) {
        this.ignoreExtension = ignoreExtension;
    }
}
