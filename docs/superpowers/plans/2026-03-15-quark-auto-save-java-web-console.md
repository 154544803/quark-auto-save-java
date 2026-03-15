# Quark Auto Save Java Web Console Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a login-protected web console for `quark-auto-save-java` that can view tasks, run tasks, and edit `config/tasks.yml` without changing the existing GitHub Actions `once` execution path.

**Architecture:** Add a server-rendered console inside the existing Spring Boot app using Thymeleaf templates plus small vanilla JavaScript. Reuse the current task execution services, and add focused services for session auth, YAML file IO, execution history, and execution locking so that `server` mode gains a console while `once` mode keeps its current startup-and-exit behavior.

**Tech Stack:** Java 17, Spring Boot Web, Thymeleaf, Jackson YAML, JUnit 5, MockMvc, HTML/CSS, vanilla JavaScript

---

## Chunk 1: Web Console Foundations

Use `@superpowers:test-driven-development` for every task in this chunk. Do not move to UI polish until route behavior, auth behavior, and `once`-mode regression tests are passing.

### Task 1: Add page rendering support and unauthenticated page routing

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/com/quark/autosave/controller/WebConsolePageController.java`
- Create: `src/main/resources/templates/login.html`
- Create: `src/main/resources/templates/dashboard.html`
- Create: `src/test/java/com/quark/autosave/controller/WebConsolePageControllerTest.java`

- [ ] **Step 1: Write the failing page controller tests**

```java
class WebConsolePageControllerTest {

    @Test
    void shouldRenderLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(view().name("login"));
    }

    @Test
    void shouldRedirectAnonymousDashboardRequestToLogin() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -q -Dtest=WebConsolePageControllerTest test`
Expected: FAIL with missing Thymeleaf view resolution or missing controller class

- [ ] **Step 3: Add the minimal rendering support**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

```java
@Controller
public class WebConsolePageController {

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/")
    public String dashboardPage(HttpSession session) {
        return session.getAttribute("webConsoleUser") == null ? "redirect:/login" : "dashboard";
    }
}
```

- [ ] **Step 4: Add minimal placeholder templates**

```html
<!-- login.html -->
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<body>
  <main><h1>Quark Auto Save</h1><form></form></main>
</body>
</html>
```

```html
<!-- dashboard.html -->
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<body>
  <main><h1>Web Console</h1></main>
</body>
</html>
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `mvn -q -Dtest=WebConsolePageControllerTest test`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add pom.xml src/main/java/com/quark/autosave/controller/WebConsolePageController.java src/main/resources/templates/login.html src/main/resources/templates/dashboard.html src/test/java/com/quark/autosave/controller/WebConsolePageControllerTest.java
git commit -m "feat(web): add console page routing shell"
```

### Task 2: Add session login, logout, and request protection without changing `once` mode

**Files:**
- Modify: `src/main/java/com/quark/autosave/config/AppProperties.java`
- Create: `src/main/java/com/quark/autosave/config/WebConsoleMvcConfig.java`
- Create: `src/main/java/com/quark/autosave/controller/WebConsoleAuthController.java`
- Create: `src/main/java/com/quark/autosave/service/WebConsoleAuthService.java`
- Create: `src/test/java/com/quark/autosave/controller/WebConsoleAuthControllerTest.java`
- Create: `src/test/java/com/quark/autosave/service/StartupModeRunnerTest.java`
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: Write the failing auth and startup regression tests**

```java
@Test
void shouldRejectAnonymousApiRequest() throws Exception {
    mockMvc.perform(post("/api/tasks/run"))
        .andExpect(status().isUnauthorized());
}

@Test
void shouldCreateSessionAfterSuccessfulLogin() throws Exception {
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"admin","password":"secret"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(true));
}
```

```java
@Test
void shouldStillRunOnceModeWithoutWebConsoleCredentials() {
    appProperties.setRunMode("once");

    startupModeRunner.run(new DefaultApplicationArguments(new String[0]));

    verify(applicationRunnerService).runAllOnce();
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `mvn -q -Dtest=WebConsoleAuthControllerTest,StartupModeRunnerTest test`
Expected: FAIL with missing auth endpoints, missing session guard, or missing once-mode regression coverage

- [ ] **Step 3: Add web console credentials to configuration**

```java
public static class WebConsoleProperties {
    private boolean enabled = true;
    private String username = "admin";
    private String password = "admin123";
}
```

```yaml
app:
  web-console:
    enabled: true
    username: ${WEB_CONSOLE_USERNAME:admin}
    password: ${WEB_CONSOLE_PASSWORD:admin123}
```

- [ ] **Step 4: Implement session auth and interceptor protection**

```java
@PostMapping("/api/auth/login")
public Map<String, Object> login(@RequestBody LoginRequest request, HttpSession session) {
    authService.authenticate(request.username(), request.password());
    session.setAttribute("webConsoleUser", request.username());
    return Map.of("authenticated", true);
}
```

```java
registry.addInterceptor(webConsoleLoginInterceptor)
    .addPathPatterns("/", "/api/**")
    .excludePathPatterns("/login", "/api/auth/login", "/css/**", "/js/**");
```

- [ ] **Step 5: Implement logout and unauthorized handling**

```java
@PostMapping("/api/auth/logout")
public Map<String, Object> logout(HttpSession session) {
    session.invalidate();
    return Map.of("authenticated", false);
}
```

- [ ] **Step 6: Update `StartupModeRunner` only if needed to preserve current exit semantics**

```java
if ("once".equalsIgnoreCase(appProperties.getRunMode())) {
    applicationRunnerService.runAllOnce();
    SpringApplication.exit(applicationContext, () -> 0);
    return;
}
```

- [ ] **Step 7: Run the tests to verify they pass**

Run: `mvn -q -Dtest=WebConsoleAuthControllerTest,StartupModeRunnerTest test`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/quark/autosave/config/AppProperties.java src/main/java/com/quark/autosave/config/WebConsoleMvcConfig.java src/main/java/com/quark/autosave/controller/WebConsoleAuthController.java src/main/java/com/quark/autosave/service/WebConsoleAuthService.java src/test/java/com/quark/autosave/controller/WebConsoleAuthControllerTest.java src/test/java/com/quark/autosave/service/StartupModeRunnerTest.java src/main/resources/application.yml
git commit -m "feat(web): add session auth and once mode regression coverage"
```

## Chunk 2: YAML Configuration Management

Keep the file-writing logic isolated in one service. Do not let controllers call `Files.writeString(...)` directly.

### Task 3: Add a dedicated service to read, validate, and atomically save `config/tasks.yml`

**Files:**
- Create: `src/main/java/com/quark/autosave/service/TaskConfigFileService.java`
- Create: `src/main/java/com/quark/autosave/model/web/TaskConfigDocument.java`
- Create: `src/main/java/com/quark/autosave/model/web/SaveTaskConfigRequest.java`
- Create: `src/main/java/com/quark/autosave/controller/TaskConfigController.java`
- Create: `src/test/java/com/quark/autosave/service/TaskConfigFileServiceTest.java`
- Create: `src/test/java/com/quark/autosave/controller/TaskConfigControllerTest.java`

- [ ] **Step 1: Write the failing file-service tests**

```java
@Test
void shouldReturnRawYamlAndParsedSummary() {
    TaskConfigDocument document = service.readCurrentConfig();

    assertThat(document.rawYaml()).contains("accounts:");
    assertThat(document.taskNames()).contains("demo-task");
}

@Test
void shouldRejectInvalidYamlWithoutOverwritingSourceFile() {
    Path taskFile = tempDir.resolve("tasks.yml");
    Files.writeString(taskFile, "accounts: []\n");

    assertThatThrownBy(() -> service.save("""
        accounts:
          - name: broken
        tasks: [
        """))
        .isInstanceOf(IllegalArgumentException.class);

    assertThat(Files.readString(taskFile)).isEqualTo("accounts: []\n");
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `mvn -q -Dtest=TaskConfigFileServiceTest,TaskConfigControllerTest test`
Expected: FAIL with missing file service or missing config endpoints

- [ ] **Step 3: Implement YAML read and validation reuse**

```java
public TaskConfigDocument readCurrentConfig() {
    String rawYaml = Files.readString(taskFilePath, StandardCharsets.UTF_8);
    TaskFileConfig parsed = taskConfigLoader.load(taskFilePath);
    List<String> taskNames = parsed.getTasks().stream().map(TaskDefinition::getName).toList();
    return new TaskConfigDocument(rawYaml, taskNames);
}
```

```java
public TaskConfigDocument save(String rawYaml) {
    Path tempFile = Files.createTempFile(taskFilePath.getParent(), "tasks-", ".yml");
    Files.writeString(tempFile, rawYaml, StandardCharsets.UTF_8);
    taskConfigLoader.load(tempFile);
    Files.move(tempFile, taskFilePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    return readCurrentConfig();
}
```

- [ ] **Step 4: Implement the config APIs**

```java
@GetMapping("/api/config/tasks")
public TaskConfigDocument getCurrentConfig() {
    return taskConfigFileService.readCurrentConfig();
}

@PutMapping("/api/config/tasks")
public TaskConfigDocument saveConfig(@RequestBody SaveTaskConfigRequest request) {
    return taskConfigFileService.save(request.rawYaml());
}
```

- [ ] **Step 5: Add controller-level error mapping**

```java
@ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
public ResponseEntity<Map<String, Object>> handleConfigException(RuntimeException exception) {
    return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
}
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `mvn -q -Dtest=TaskConfigFileServiceTest,TaskConfigControllerTest test`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/quark/autosave/service/TaskConfigFileService.java src/main/java/com/quark/autosave/model/web/TaskConfigDocument.java src/main/java/com/quark/autosave/model/web/SaveTaskConfigRequest.java src/main/java/com/quark/autosave/controller/TaskConfigController.java src/test/java/com/quark/autosave/service/TaskConfigFileServiceTest.java src/test/java/com/quark/autosave/controller/TaskConfigControllerTest.java
git commit -m "feat(web): add editable task config API"
```

## Chunk 3: Task Dashboard APIs and Execution State

Do not bolt history state onto controllers. Put it behind dedicated services so page rendering and API tests stay simple.

### Task 4: Add structured task summaries for the dashboard

**Files:**
- Modify: `src/main/java/com/quark/autosave/service/ApplicationRunnerService.java`
- Create: `src/main/java/com/quark/autosave/model/web/TaskView.java`
- Modify: `src/main/java/com/quark/autosave/controller/TaskController.java`
- Modify: `src/test/java/com/quark/autosave/controller/TaskControllerTest.java`

- [ ] **Step 1: Write the failing structured task API test**

```java
@Test
void shouldReturnStructuredTasks() throws Exception {
    when(applicationRunnerService.listTasks()).thenReturn(List.of(
        new TaskView("task-1", "primary", "/动漫/任务1", true, List.of(1, 3), null)
    ));

    mockMvc.perform(get("/api/tasks"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("task-1"))
        .andExpect(jsonPath("$[0].account").value("primary"))
        .andExpect(jsonPath("$[0].enabled").value(true));
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -q -Dtest=TaskControllerTest test`
Expected: FAIL because `/api/tasks` currently returns a list of strings

- [ ] **Step 3: Extend the service and controller**

```java
public List<TaskView> listTasks() {
    return loadTaskFile().getTasks().stream()
        .map(task -> new TaskView(
            task.getName(),
            task.getAccount(),
            task.getSavePath(),
            task.isEnabled(),
            task.getRunWeek(),
            task.getEndDate()))
        .toList();
}
```

```java
@GetMapping
public List<TaskView> listTasks() {
    return applicationRunnerService.listTasks();
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -q -Dtest=TaskControllerTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/quark/autosave/service/ApplicationRunnerService.java src/main/java/com/quark/autosave/model/web/TaskView.java src/main/java/com/quark/autosave/controller/TaskController.java src/test/java/com/quark/autosave/controller/TaskControllerTest.java
git commit -m "feat(web): expose structured task summaries"
```

### Task 5: Add execution lock and recent history for manual runs

**Files:**
- Create: `src/main/java/com/quark/autosave/service/ExecutionGuardService.java`
- Create: `src/main/java/com/quark/autosave/service/ExecutionHistoryService.java`
- Create: `src/main/java/com/quark/autosave/model/web/ExecutionHistoryEntry.java`
- Create: `src/main/java/com/quark/autosave/controller/ExecutionHistoryController.java`
- Modify: `src/main/java/com/quark/autosave/controller/TaskController.java`
- Create: `src/test/java/com/quark/autosave/service/ExecutionGuardServiceTest.java`
- Create: `src/test/java/com/quark/autosave/service/ExecutionHistoryServiceTest.java`
- Modify: `src/test/java/com/quark/autosave/controller/TaskControllerTest.java`

- [ ] **Step 1: Write the failing guard and history tests**

```java
@Test
void shouldRejectSecondManualRunWhileFirstIsActive() {
    assertThat(service.tryAcquire()).isTrue();
    assertThat(service.tryAcquire()).isFalse();
}
```

```java
@Test
void shouldStoreNewestHistoryEntryFirst() {
    historyService.record("ALL", summaryOne);
    historyService.record("task-1", summaryTwo);

    assertThat(historyService.listRecent()).extracting(ExecutionHistoryEntry::trigger)
        .containsExactly("task-1", "ALL");
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `mvn -q -Dtest=ExecutionGuardServiceTest,ExecutionHistoryServiceTest,TaskControllerTest test`
Expected: FAIL with missing history and concurrency behavior

- [ ] **Step 3: Implement the guard and history services**

```java
public boolean tryAcquire() {
    return running.compareAndSet(false, true);
}

public void release() {
    running.set(false);
}
```

```java
public void record(String trigger, TaskExecutionSummary summary) {
    entries.addFirst(new ExecutionHistoryEntry(LocalDateTime.now(), trigger, summary));
    while (entries.size() > 10) {
        entries.removeLast();
    }
}
```

- [ ] **Step 4: Wrap task execution with the guard and history**

```java
if (!executionGuardService.tryAcquire()) {
    throw new IllegalStateException("任务正在执行中，请稍后再试");
}
try {
    TaskExecutionSummary summary = applicationRunnerService.runAllOnce();
    executionHistoryService.record("ALL", summary);
    return summary;
} finally {
    executionGuardService.release();
}
```

- [ ] **Step 5: Add the recent-history API**

```java
@GetMapping("/api/history")
public List<ExecutionHistoryEntry> listRecentHistory() {
    return executionHistoryService.listRecent();
}
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `mvn -q -Dtest=ExecutionGuardServiceTest,ExecutionHistoryServiceTest,TaskControllerTest test`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/quark/autosave/service/ExecutionGuardService.java src/main/java/com/quark/autosave/service/ExecutionHistoryService.java src/main/java/com/quark/autosave/model/web/ExecutionHistoryEntry.java src/main/java/com/quark/autosave/controller/ExecutionHistoryController.java src/main/java/com/quark/autosave/controller/TaskController.java src/test/java/com/quark/autosave/service/ExecutionGuardServiceTest.java src/test/java/com/quark/autosave/service/ExecutionHistoryServiceTest.java src/test/java/com/quark/autosave/controller/TaskControllerTest.java
git commit -m "feat(web): track manual run history and prevent overlapping runs"
```

## Chunk 4: Dashboard UI, Documentation, and Verification

Treat this chunk as integration work. Keep the markup intentional and simple; do not rebuild the Python UI pixel-for-pixel.

### Task 6: Build the dashboard HTML, CSS, and client-side behavior

**Files:**
- Modify: `src/main/resources/templates/login.html`
- Modify: `src/main/resources/templates/dashboard.html`
- Create: `src/main/resources/static/css/web-console.css`
- Create: `src/main/resources/static/js/web-console.js`
- Create: `src/test/java/com/quark/autosave/controller/WebConsolePageRenderingTest.java`

- [ ] **Step 1: Write the failing rendering test**

```java
@Test
void shouldRenderDashboardSectionsForAuthenticatedUser() throws Exception {
    mockMvc.perform(get("/").sessionAttr("webConsoleUser", "admin"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("任务列表")))
        .andExpect(content().string(containsString("最近执行记录")))
        .andExpect(content().string(containsString("tasks.yml")));
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -q -Dtest=WebConsolePageRenderingTest test`
Expected: FAIL because the placeholder templates do not contain the dashboard sections

- [ ] **Step 3: Implement the login page**

```html
<main class="login-shell">
  <section class="login-card">
    <h1>Quark Auto Save</h1>
    <p>登录后管理任务与配置</p>
    <form id="login-form">
      <input name="username" type="text" autocomplete="username" required>
      <input name="password" type="password" autocomplete="current-password" required>
      <button type="submit">登录</button>
    </form>
    <p id="login-error" hidden></p>
  </section>
</main>
```

- [ ] **Step 4: Implement the dashboard page structure**

```html
<main class="dashboard-shell">
  <section class="panel" id="task-panel"></section>
  <section class="panel" id="history-panel"></section>
  <section class="panel">
    <header><h2>tasks.yml</h2><button id="save-config">保存配置</button></header>
    <textarea id="config-editor"></textarea>
  </section>
</main>
```

- [ ] **Step 5: Implement the client-side API wiring**

```javascript
async function loadDashboard() {
  const [tasks, config, history] = await Promise.all([
    fetchJson('/api/tasks'),
    fetchJson('/api/config/tasks'),
    fetchJson('/api/history')
  ]);
  renderTasks(tasks);
  renderHistory(history);
  document.querySelector('#config-editor').value = config.rawYaml;
}
```

```javascript
document.querySelector('#run-all').addEventListener('click', () => runTasks('/api/tasks/run'));
document.querySelector('#save-config').addEventListener('click', saveConfig);
```

- [ ] **Step 6: Add one focused stylesheet instead of scattered inline styles**

```css
.dashboard-shell { display: grid; gap: 16px; grid-template-columns: 1.1fr 1fr; }
.panel { background: #ffffff; border-radius: 18px; padding: 20px; box-shadow: 0 12px 36px rgba(15, 23, 42, 0.08); }
.task-row[disabled] { opacity: 0.6; pointer-events: none; }
```

- [ ] **Step 7: Run the rendering test to verify it passes**

Run: `mvn -q -Dtest=WebConsolePageRenderingTest test`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add src/main/resources/templates/login.html src/main/resources/templates/dashboard.html src/main/resources/static/css/web-console.css src/main/resources/static/js/web-console.js src/test/java/com/quark/autosave/controller/WebConsolePageRenderingTest.java
git commit -m "feat(web): add console pages and client behavior"
```

### Task 7: Update docs and verify both runtime paths

**Files:**
- Modify: `README.md`
- Modify: `config/tasks.example.yml`
- Verify only: `src/main/java/com/quark/autosave/service/StartupModeRunner.java`
- Verify only: `.github/workflows/schedule-run.yml`

- [ ] **Step 1: Document the management console setup**

Add to `README.md`:

```md
## Web 管理台
启动：
`mvn -s settings.xml spring-boot:run`

环境变量：
`WEB_CONSOLE_USERNAME`
`WEB_CONSOLE_PASSWORD`
```

- [ ] **Step 2: Document the `once` path explicitly**

Add to `README.md`:

```md
GitHub Actions 继续使用：
`java -jar target/quark-auto-save-1.0.0-SNAPSHOT-exec.jar --app.run-mode=once --app.task-file=config/tasks.yml --app.notification.mail.enabled=false`
```

- [ ] **Step 3: Add any missing comments to the example config**

Update `config/tasks.example.yml` only if a comment or sample field is needed for the console workflow. Do not add web-console credentials here.

- [ ] **Step 4: Run focused automated verification**

Run: `mvn test`
Expected: PASS

- [ ] **Step 5: Run packaging verification**

Run: `mvn -q -DskipTests clean package`
Expected: PASS and `target/quark-auto-save-1.0.0-SNAPSHOT-exec.jar` exists

- [ ] **Step 6: Run `once`-mode regression verification**

Run: `java -jar target/quark-auto-save-1.0.0-SNAPSHOT-exec.jar --app.run-mode=once --app.task-file=config/tasks.example.yml --app.notification.mail.enabled=false`
Expected: app executes once and exits without requiring any web-console login config

- [ ] **Step 7: Run `server`-mode smoke verification**

Run: `mvn -q spring-boot:run`
Expected: app starts on port `8080`, `/login` renders, and unauthenticated `/api/tasks` returns `401`

- [ ] **Step 8: Commit**

```bash
git add README.md config/tasks.example.yml
git commit -m "docs: add web console usage guidance"
```

## Execution Notes

- Keep `once` mode behavior stable throughout the implementation. If any chunk requires touching startup logic, rerun the `StartupModeRunnerTest` before moving on.
- Prefer adding new focused classes instead of growing `TaskController` or `ApplicationRunnerService` into mixed UI-and-business files.
- If a test command fails because of environment-specific external dependencies, stop and narrow the test to the smallest affected class rather than weakening coverage globally.
