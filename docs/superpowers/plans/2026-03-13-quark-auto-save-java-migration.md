# Quark Auto Save Java Migration Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将当前 Python 版 quark-auto-save 迁移为可在本地运行、支持 GitHub Actions 定时执行的 Spring Boot Java 项目。

**Architecture:** 使用 Spring Boot 单体应用承载配置加载、定时调度、夸克转存核心流程、邮件通知和最小 REST 接口。通过 `application.yml` 与外部 `tasks.yml` 分离系统配置和业务任务配置，并提供 `server` 与 `once` 两种运行模式。

**Tech Stack:** Java 17、Spring Boot 3、Maven 3.6.3、Spring Web、Spring Scheduling、Spring Mail、JUnit 5、Mockito

---

## Chunk 1: 项目骨架与构建配置

### Task 1: 清理旧入口并建立 Maven 工程骨架

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/quark/autosave/QuarkAutoSaveApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/test/java/com/quark/autosave/QuarkAutoSaveApplicationTests.java`
- Modify: `.gitignore`

- [ ] **Step 1: 写启动测试**

```java
@SpringBootTest
class QuarkAutoSaveApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `mvn -q -Dtest=QuarkAutoSaveApplicationTests test`
Expected: FAIL，提示缺少 Maven 工程或 Spring Boot 入口

- [ ] **Step 3: 编写最小 Spring Boot 工程**

实现 Maven 构建、主启动类与基础配置文件。

- [ ] **Step 4: 再次运行测试验证通过**

Run: `mvn -q -Dtest=QuarkAutoSaveApplicationTests test`
Expected: PASS

### Task 2: 建立外部配置加载能力

**Files:**
- Create: `src/main/java/com/quark/autosave/config/AppProperties.java`
- Create: `src/main/java/com/quark/autosave/config/TaskFileProperties.java`
- Create: `src/main/java/com/quark/autosave/config/TaskConfigLoader.java`
- Create: `src/main/java/com/quark/autosave/model/config/AccountConfig.java`
- Create: `src/main/java/com/quark/autosave/model/config/TaskDefinition.java`
- Create: `src/main/java/com/quark/autosave/model/config/TaskFileConfig.java`
- Create: `src/test/java/com/quark/autosave/config/TaskConfigLoaderTest.java`
- Create: `src/test/resources/config/tasks-valid.yml`

- [ ] **Step 1: 写配置加载失败测试**

覆盖：
- 正常加载账号和任务
- 文件不存在时报错
- 缺少账号引用时报错

- [ ] **Step 2: 运行测试验证失败**

Run: `mvn -q -Dtest=TaskConfigLoaderTest test`
Expected: FAIL，提示缺少配置加载实现

- [ ] **Step 3: 实现配置属性与 YAML 加载器**

使用 Spring Boot + Jackson YAML 解析外部任务文件，并添加中文注释解释校验逻辑。

- [ ] **Step 4: 再次运行测试验证通过**

Run: `mvn -q -Dtest=TaskConfigLoaderTest test`
Expected: PASS

## Chunk 2: 核心规则与任务编排

### Task 3: 实现任务时间窗口判断

**Files:**
- Create: `src/main/java/com/quark/autosave/support/TaskScheduleDecider.java`
- Create: `src/test/java/com/quark/autosave/support/TaskScheduleDeciderTest.java`

- [ ] **Step 1: 写失败测试**

覆盖：
- 无限制任务可执行
- 超过截止日期不可执行
- 周限制不匹配不可执行
- 周限制匹配可执行

- [ ] **Step 2: 运行测试验证失败**

Run: `mvn -q -Dtest=TaskScheduleDeciderTest test`
Expected: FAIL

- [ ] **Step 3: 实现最小判断逻辑**

只实现本期所需的日期和星期判断。

- [ ] **Step 4: 运行测试验证通过**

Run: `mvn -q -Dtest=TaskScheduleDeciderTest test`
Expected: PASS

### Task 4: 实现分享链接解析与重命名规则

**Files:**
- Create: `src/main/java/com/quark/autosave/support/ShareUrlParser.java`
- Create: `src/main/java/com/quark/autosave/support/RenameRuleEngine.java`
- Create: `src/test/java/com/quark/autosave/support/ShareUrlParserTest.java`
- Create: `src/test/java/com/quark/autosave/support/RenameRuleEngineTest.java`

- [ ] **Step 1: 写失败测试**

覆盖：
- 解析分享 ID
- 解析提取码
- 解析路径 fid
- 正则重命名成功
- 空替换保持原文件名

- [ ] **Step 2: 运行测试验证失败**

Run: `mvn -q -Dtest=ShareUrlParserTest,RenameRuleEngineTest test`
Expected: FAIL

- [ ] **Step 3: 实现解析器与重命名引擎**

优先支持本期任务模型，不复刻全部魔法变量。

- [ ] **Step 4: 运行测试验证通过**

Run: `mvn -q -Dtest=ShareUrlParserTest,RenameRuleEngineTest test`
Expected: PASS

### Task 5: 实现任务执行编排服务

**Files:**
- Create: `src/main/java/com/quark/autosave/model/runtime/TaskExecutionItem.java`
- Create: `src/main/java/com/quark/autosave/model/runtime/TaskExecutionSummary.java`
- Create: `src/main/java/com/quark/autosave/service/TaskExecutionService.java`
- Create: `src/test/java/com/quark/autosave/service/TaskExecutionServiceTest.java`

- [ ] **Step 1: 写失败测试**

覆盖：
- 只执行启用且满足时间条件的任务
- 单任务失败不影响其他任务
- 汇总成功、失败、跳过数量

- [ ] **Step 2: 运行测试验证失败**

Run: `mvn -q -Dtest=TaskExecutionServiceTest test`
Expected: FAIL

- [ ] **Step 3: 实现任务编排服务**

先通过接口抽象调用底层夸克执行器，保证编排层可独立测试。

- [ ] **Step 4: 运行测试验证通过**

Run: `mvn -q -Dtest=TaskExecutionServiceTest test`
Expected: PASS

## Chunk 3: 夸克客户端、调度与通知

### Task 6: 实现夸克客户端抽象与 HTTP 骨架

**Files:**
- Create: `src/main/java/com/quark/autosave/client/QuarkClient.java`
- Create: `src/main/java/com/quark/autosave/client/DefaultQuarkClient.java`
- Create: `src/main/java/com/quark/autosave/model/quark/ShareParseResult.java`
- Create: `src/main/java/com/quark/autosave/model/quark/QuarkFileItem.java`
- Create: `src/main/java/com/quark/autosave/service/QuarkTransferService.java`
- Create: `src/test/java/com/quark/autosave/service/QuarkTransferServiceTest.java`

- [ ] **Step 1: 写失败测试**

覆盖：
- 使用任务和账号构造一次转存请求
- 根据匹配规则筛选待转存文件
- 生成重命名结果

- [ ] **Step 2: 运行测试验证失败**

Run: `mvn -q -Dtest=QuarkTransferServiceTest test`
Expected: FAIL

- [ ] **Step 3: 实现最小可用夸克转存服务**

通过接口分离 HTTP 调用，关键流程添加中文注释。

- [ ] **Step 4: 运行测试验证通过**

Run: `mvn -q -Dtest=QuarkTransferServiceTest test`
Expected: PASS

### Task 7: 实现调度器、运行模式与 REST 接口

**Files:**
- Create: `src/main/java/com/quark/autosave/scheduler/TaskSchedulerRunner.java`
- Create: `src/main/java/com/quark/autosave/controller/TaskController.java`
- Create: `src/main/java/com/quark/autosave/service/ApplicationRunnerService.java`
- Create: `src/test/java/com/quark/autosave/controller/TaskControllerTest.java`

- [ ] **Step 1: 写失败测试**

覆盖：
- 查询任务列表接口
- 手动触发全部任务接口
- 手动触发单任务接口

- [ ] **Step 2: 运行测试验证失败**

Run: `mvn -q -Dtest=TaskControllerTest test`
Expected: FAIL

- [ ] **Step 3: 实现接口与运行模式逻辑**

支持 `server` 和 `once` 两种模式。

- [ ] **Step 4: 运行测试验证通过**

Run: `mvn -q -Dtest=TaskControllerTest test`
Expected: PASS

### Task 8: 实现邮件通知

**Files:**
- Create: `src/main/java/com/quark/autosave/service/MailNotificationService.java`
- Create: `src/test/java/com/quark/autosave/service/MailNotificationServiceTest.java`

- [ ] **Step 1: 写失败测试**

覆盖：
- 汇总内容格式化
- 未启用邮件时跳过发送

- [ ] **Step 2: 运行测试验证失败**

Run: `mvn -q -Dtest=MailNotificationServiceTest test`
Expected: FAIL

- [ ] **Step 3: 实现最小邮件通知服务**

使用 Spring Mail 发送纯文本摘要。

- [ ] **Step 4: 运行测试验证通过**

Run: `mvn -q -Dtest=MailNotificationServiceTest test`
Expected: PASS

## Chunk 4: 交付文件与运行验证

### Task 9: 更新仓库交付物

**Files:**
- Create: `config/tasks.example.yml`
- Modify: `README.md`
- Modify: `.gitignore`
- Create: `.github/workflows/schedule-run.yml`

- [ ] **Step 1: 写工作流与配置示例清单**

明确本地运行方式、环境变量、GitHub Secrets 和定时执行方式。

- [ ] **Step 2: 实现 README、示例配置和工作流**

工作流需支持 `schedule` 与 `workflow_dispatch`。

- [ ] **Step 3: 手工检查关键文档内容**

确认 README、示例配置和工作流中的路径、命令和变量名一致。

### Task 10: 完整验证

**Files:**
- Verify only

- [ ] **Step 1: 运行单元测试**

Run: `mvn test`
Expected: 全部测试通过

- [ ] **Step 2: 运行打包验证**

Run: `mvn clean package -DskipTests`
Expected: 构建成功，生成可执行 jar

- [ ] **Step 3: 运行应用启动验证**

Run: `mvn spring-boot:run`
Expected: 应用正常启动并暴露接口

- [ ] **Step 4: 运行单次执行模式验证**

Run: `java -jar target/*.jar --app.run-mode=once --app.task-file=config/tasks.example.yml`
Expected: 程序执行一次后正常退出
