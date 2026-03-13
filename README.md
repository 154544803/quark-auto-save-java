# quark-auto-save Java 版说明

## 概述

这是当前仓库新增的 `Spring Boot` Java 版核心实现，目标是优先满足以下场景：

- 本地基于 `JDK 17`、`Maven 3.6.3` 直接运行
- 通过 GitHub Actions 定时执行一次任务
- 通过最小 REST 接口手动触发任务
- 通过邮件接收执行摘要

## 快速开始

### 1. 准备任务文件

复制示例文件：

```bash
cp config/tasks.example.yml config/tasks.yml
```

### 2. 配置环境变量

```bash
set QUARK_COOKIE=你的夸克Cookie
set MAIL_HOST=smtp.example.com
set MAIL_PORT=465
set MAIL_USERNAME=your@example.com
set MAIL_PASSWORD=your-password
```

### 3. 运行测试

```bash
mvn -s settings.xml test
```

### 4. 本地启动

```bash
mvn -s settings.xml spring-boot:run
```

### 5. 打包后运行

```bash
mvn -s settings.xml clean package
java -jar target/quark-auto-save-1.0.0-SNAPSHOT-exec.jar
```

### 6. 单次执行模式

```bash
java -jar target/quark-auto-save-1.0.0-SNAPSHOT-exec.jar --app.run-mode=once --app.task-file=config/tasks.yml --app.notification.mail.enabled=true
```

## 接口

- `GET /api/tasks`
- `POST /api/tasks/run`
- `POST /api/tasks/run/{taskName}`

## GitHub Actions

工作流文件：

- `.github/workflows/schedule-run.yml`

建议配置的 Secrets：

- `QUARK_COOKIE`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
