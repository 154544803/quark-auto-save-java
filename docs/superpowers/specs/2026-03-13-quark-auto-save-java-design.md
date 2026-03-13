# quark-auto-save Java 迁移设计

## 背景

当前仓库主体为 Python 项目，包含夸克网盘自动转存、定时执行、通知、插件和 WebUI 等能力。本次迁移目标不是一次性等价重写全部功能，而是在保留核心业务能力的前提下，重构为一个可在本地稳定运行、可上传到 GitHub 并使用 GitHub Actions 定时执行的 Java 项目。

## 目标

- 使用 `Spring Boot` 构建稳定的 Java 项目
- 兼容 `JDK 17` 与 `Maven 3.6.3`
- 支持本地运行与 GitHub Actions 定时执行
- 支持夸克分享链接自动转存核心流程
- 支持任务级正则过滤、重命名、截止日期、周执行限制
- 支持最小 REST 手动触发接口
- 支持邮件通知执行结果
- 关键代码增加中文注释，文件统一使用 `UTF-8`

## 范围

### 本期包含

- Spring Boot 启动与配置加载
- `application.yml` 系统配置
- `config/tasks.yml` 外部任务配置
- 夸克账号与任务模型
- 定时任务执行
- 手动触发接口
- 夸克核心 API 调用与转存流程
- 正则过滤与重命名
- 邮件通知
- GitHub Actions 定时工作流
- README 与示例配置更新

### 本期不包含

- WebUI 页面
- 插件体系
- 多通知渠道
- Docker 优先适配
- 数据库存储
- 与原 Python 项目所有高级规则完全兼容

## 总体架构

项目采用单体 Spring Boot 架构，按“配置、调度、业务、通知、接口”分层：

- `config`：加载 `application.yml` 与外部 `tasks.yml`
- `controller`：提供最小 REST 执行接口
- `scheduler`：根据 cron 定时触发任务
- `service`：封装任务执行、夸克 API 访问、邮件通知
- `model`：定义账号、任务、执行结果等领域模型
- `support`：放置正则重命名、日期判断、链接解析等工具

该设计优先保证本地运行稳定和 GitHub Actions 易接入，而不是先追求复杂扩展能力。

## 配置设计

### application.yml

用于存放系统级配置：

- 服务端口
- 定时任务开关与 cron 表达式
- 运行模式
- 日志配置
- 外部任务文件位置
- 邮件服务器配置

### config/tasks.yml

用于存放业务任务配置：

- 夸克账号列表
- 任务名称
- 任务绑定账号
- 分享链接
- 保存路径
- 文件匹配正则
- 重命名规则
- 截止日期
- 周执行限制
- 是否启用

### 敏感信息策略

所有敏感信息均支持环境变量占位符：

- 夸克 Cookie
- 邮件用户名
- 邮件密码

本地与 GitHub Actions 统一使用环境变量注入，避免敏感信息进入仓库。

## 运行模式

程序支持两种运行模式：

- `server`：默认模式，启动 Web 服务、定时任务和 REST 接口，适合本地
- `once`：启动后执行一次全部任务并退出，适合 GitHub Actions

GitHub Actions 通过启动参数覆盖：

```bash
java -jar app.jar --app.run-mode=once
```

## 核心执行流程

1. 启动时加载系统配置与任务配置
2. 校验账号、任务和邮件配置
3. 调度器或接口触发任务执行
4. 逐个处理启用任务
5. 校验是否过期、是否满足周执行条件
6. 解析分享链接中的分享 ID、提取码、目录信息
7. 调用夸克接口查询分享目录
8. 根据正则筛选目标文件
9. 对命中文件计算转存目标名称
10. 调用夸克接口执行转存
11. 如有必要执行重命名
12. 汇总任务执行结果并发送邮件

## 领域建模

### 账号模型

- `name`：账号标识
- `cookie`：夸克 Cookie

### 任务模型

- `name`
- `account`
- `shareUrl`
- `savePath`
- `pattern`
- `replace`
- `enabled`
- `endDate`
- `runWeek`
- `ignoreExtension`

### 执行结果模型

- 总任务数
- 成功数
- 失败数
- 跳过数
- 单任务摘要
- 开始时间与结束时间

## 夸克接口策略

本期保留原项目的核心调用思路，但会在 Java 中重构为清晰的客户端组件：

- 通过 Cookie 构造请求头
- 解析分享链接获得 `pwd_id` 等参数
- 获取分享 `stoken`
- 查询分享目录内容
- 查询目标目录 `fid`
- 创建缺失目录
- 执行转存
- 查询异步任务结果
- 必要时执行重命名

HTTP 客户端优先使用 Spring 原生 `RestClient`，减少额外依赖并提升维护稳定性。

## REST 接口设计

本期只提供最小接口：

- `POST /api/tasks/run`
  - 触发全部启用任务执行
- `POST /api/tasks/run/{taskName}`
  - 触发指定任务执行
- `GET /api/tasks`
  - 查看当前已加载任务列表

接口返回统一的执行摘要，便于本地调试。

## 错误处理

- 单个任务失败不影响后续任务继续执行
- 配置错误在启动阶段尽早暴露
- 夸克接口异常记录详细日志
- 邮件发送失败不影响主任务结果，但会记录告警日志
- 对响应结构异常进行显式判空和结果校验

## 测试策略

迁移实现采用 TDD：

- 先写配置加载测试
- 再写任务时间窗口判断测试
- 再写分享链接解析测试
- 再写正则重命名测试
- 再写任务编排服务测试
- HTTP 客户端使用可替换的网关接口，降低测试耦合

由于夸克接口依赖外部服务，本期优先做单元测试和服务层测试，不强依赖真实在线集成测试。

## GitHub Actions 设计

新增独立工作流用于定时执行：

- 触发方式：`schedule` + `workflow_dispatch`
- 环境：`ubuntu-latest`
- JDK：`17`
- 构建：Maven 打包
- 执行：`java -jar ... --app.run-mode=once`
- 配置：运行前由 Secrets 生成 `config/tasks.yml`

Secrets 至少包括：

- `QUARK_COOKIE`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_TO`

## 迁移策略

迁移采用“核心替换”策略：

1. 保留仓库历史
2. 新建 Spring Boot 工程结构
3. 逐步替换 Python 入口能力
4. 更新 README、示例配置与工作流
5. 让仓库默认以 Java 项目形态运行

原 Python 文件是否后续完全删除，可在迁移完成并验证通过后再决定。
