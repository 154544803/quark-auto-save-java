# quark-auto-save Java 版说明

## 概览

这是基于 `Spring Boot` 的 `quark-auto-save` Java 版本，支持两种主要使用方式：

- 本地或服务器常驻运行，通过 Web 管理台维护账号、任务和手动执行
- 在 GitHub Actions 中使用 `once` 模式执行一次任务后退出

当前版本重点提供：

- Quark 自动转存核心能力
- 登录保护的 Web 管理台
- 结构化表单配置编辑
- 高级 YAML 编辑兜底入口
- 最近执行记录查看
- 邮件通知能力

## 环境要求

- `JDK 17`
- `Maven 3.6.3+`

## 快速开始

### 1. 准备任务配置文件

复制示例配置：

```bash
cp config/tasks.example.yml config/tasks.yml
```

### 2. 配置环境变量

最少需要配置 Quark Cookie：

```bash
set QUARK_COOKIE=你的夸克Cookie
```

如果要启用邮件通知，再补充：

```bash
set MAIL_HOST=smtp.example.com
set MAIL_PORT=465
set MAIL_USERNAME=your@example.com
set MAIL_PASSWORD=your-password
```

### 3. 运行测试

```bash
mvn test
```

### 4. 本地启动

```bash
mvn spring-boot:run
```

### 5. 打包后运行

```bash
mvn clean package
java -jar target/quark-auto-save-1.0.0-SNAPSHOT-exec.jar
```

## Web 管理台

默认 `server` 模式会启动 Web 管理台。

启动后访问：

- `http://localhost:8080/login`

### 默认账号密码

如果没有通过环境变量覆盖，默认凭据为：

- 用户名：`admin`
- 密码：`admin123`

建议通过环境变量覆盖：

```bash
set WEB_CONSOLE_USERNAME=admin
set WEB_CONSOLE_PASSWORD=change-me
```

### 管理台能力

- 查看账号列表和任务列表
- 新增、编辑、删除账号
- 新增、编辑、删除任务
- 手动执行全部任务或单个任务
- 查看最近执行记录
- 通过表单保存常用配置
- 在高级模式中直接编辑原始 `tasks.yml`

### 配置编辑说明

管理台默认采用“表单优先”方式编辑配置：

- 账号通过表单维护 `name` 和 `cookie`
- 任务通过表单维护常用字段，例如分享链接、保存路径、运行星期等
- 保存时由后端统一校验并写回 `config/tasks.yml`

如果遇到少量表单暂未覆盖的字段，可以展开“高级模式”，直接编辑原始 YAML。高级模式保存时同样会经过后端校验。

## 接口

### 任务接口

- `GET /api/tasks`
- `POST /api/tasks/run`
- `POST /api/tasks/run/{taskName}`

### 配置接口

- `GET /api/config/tasks`
- `PUT /api/config/tasks/structured`
- `PUT /api/config/tasks/advanced`

### 认证接口

- `POST /api/auth/login`
- `POST /api/auth/logout`

### 历史接口

- `GET /api/history`

## GitHub Actions

工作流文件：

- `.github/workflows/schedule-run.yml`

建议配置的 Secrets：

- `QUARK_COOKIE`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`

## `once` 模式说明

GitHub Actions 应继续使用 `once` 模式，而不是 Web 管理台模式。

示例：

```bash
java -jar target/quark-auto-save-1.0.0-SNAPSHOT-exec.jar --app.run-mode=once --app.task-file=config/tasks.yml --app.notification.mail.enabled=false
```

`once` 模式的行为保持不变：

- 启动
- 读取 `config/tasks.yml`
- 执行一次任务
- 输出结果后退出

它不依赖 Web 登录流程，也不会因为管理台界面升级而改变现有 GitHub Actions 执行链路。
