package com.quark.autosave.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String runMode = "server";
    private boolean startupRun;
    private String taskFile = "config/tasks.yml";
    private final ScheduleProperties schedule = new ScheduleProperties();
    private final NotificationProperties notification = new NotificationProperties();
    private final WebConsoleProperties webConsole = new WebConsoleProperties();

    public String getRunMode() {
        return runMode;
    }

    public void setRunMode(String runMode) {
        this.runMode = runMode;
    }

    public boolean isStartupRun() {
        return startupRun;
    }

    public void setStartupRun(boolean startupRun) {
        this.startupRun = startupRun;
    }

    public String getTaskFile() {
        return taskFile;
    }

    public void setTaskFile(String taskFile) {
        this.taskFile = taskFile;
    }

    public ScheduleProperties getSchedule() {
        return schedule;
    }

    public NotificationProperties getNotification() {
        return notification;
    }

    public WebConsoleProperties getWebConsole() {
        return webConsole;
    }

    public static class ScheduleProperties {

        private boolean enabled = true;
        private String cron = "0 0 20 * * *";
        private String zone = "Asia/Shanghai";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public String getZone() {
            return zone;
        }

        public void setZone(String zone) {
            this.zone = zone;
        }
    }

    public static class NotificationProperties {

        private final MailProperties mail = new MailProperties();

        public MailProperties getMail() {
            return mail;
        }
    }

    public static class MailProperties {

        private boolean enabled;
        private String subjectPrefix = "网盘自动转存";
        private String to;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSubjectPrefix() {
            return subjectPrefix;
        }

        public void setSubjectPrefix(String subjectPrefix) {
            this.subjectPrefix = subjectPrefix;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }
    }

    public static class WebConsoleProperties {

        private boolean enabled = true;
        private String username = "admin";
        private String password = "admin123";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
