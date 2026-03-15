# Quark Auto Save Java Form Editor and UI Refresh Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the raw-YAML-first dashboard editor with a form-first account/task editor, preserve advanced YAML as a fallback, and upgrade the dashboard UI without changing the existing `once`-mode execution path.

**Architecture:** Keep the current Spring Boot + Thymeleaf + vanilla JavaScript structure, but add a structured configuration read/write layer on top of the existing `TaskFileConfig` model. The dashboard will render summary cards and open focused editing drawers for accounts and tasks, while the backend remains the single source of truth that validates input and writes `config/tasks.yml`.

**Tech Stack:** Java 17, Spring Boot Web, Thymeleaf, Jackson YAML, JUnit 5, MockMvc, HTML, CSS, vanilla JavaScript

---

## Chunk 1: Structured Configuration API

Use `@superpowers:test-driven-development` for every task in this chunk. Do not touch dashboard UI code until the structured configuration contract exists and can round-trip back to `tasks.yml`.

### Task 1: Add structured configuration response models for accounts, tasks, and advanced YAML

**Files:**
- Create: `src/main/java/com/quark/autosave/model/web/EditableAccountView.java`
- Create: `src/main/java/com/quark/autosave/model/web/EditableTaskView.java`
- Create: `src/main/java/com/quark/autosave/model/web/StructuredTaskConfigDocument.java`
- Modify: `src/test/java/com/quark/autosave/service/TaskConfigFileServiceTest.java`

- [ ] **Step 1: Write the failing service-level read test**

```java
@Test
void shouldReturnStructuredAccountsTasksAndAdvancedYaml() {
    StructuredTaskConfigDocument document = service.readStructuredConfig();

    assertThat(document.accounts()).extracting(EditableAccountView::name).contains("primary");
    assertThat(document.tasks()).extracting(EditableTaskView::name).contains("demo-task");
    assertThat(document.advanced().rawYaml()).contains("accounts:");
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -q -Dtest=TaskConfigFileServiceTest test`
Expected: FAIL with missing method or missing structured response model types

- [ ] **Step 3: Create minimal web models**

```java
public record EditableAccountView(String name, String cookie, boolean cookieConfigured, int taskCount) {}
```

```java
public record EditableTaskView(
    String name,
    String account,
    String shareUrl,
    String savePath,
    String pattern,
    String replace,
    boolean enabled,
    boolean ignoreExtension,
    List<Integer> runWeek,
    LocalDate endDate
) {}
```

```java
public record StructuredTaskConfigDocument(
    List<EditableAccountView> accounts,
    List<EditableTaskView> tasks,
    TaskConfigDocument advanced
) {}
```

- [ ] **Step 4: Add the minimal read conversion in `TaskConfigFileService`**

```java
public StructuredTaskConfigDocument readStructuredConfig() {
    TaskConfigDocument advanced = readCurrentConfig();
    TaskFileConfig fileConfig = taskConfigLoader.load(resolveTaskFilePath());
    // map accounts and tasks into editable views
    return new StructuredTaskConfigDocument(accounts, tasks, advanced);
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `mvn -q -Dtest=TaskConfigFileServiceTest test`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/quark/autosave/model/web/EditableAccountView.java src/main/java/com/quark/autosave/model/web/EditableTaskView.java src/main/java/com/quark/autosave/model/web/StructuredTaskConfigDocument.java src/test/java/com/quark/autosave/service/TaskConfigFileServiceTest.java src/main/java/com/quark/autosave/service/TaskConfigFileService.java
git commit -m "feat(config): add structured config read model"
```

### Task 2: Add structured save request models and persist form-edited config back to YAML

**Files:**
- Create: `src/main/java/com/quark/autosave/model/web/EditableAccountRequest.java`
- Create: `src/main/java/com/quark/autosave/model/web/EditableTaskRequest.java`
- Create: `src/main/java/com/quark/autosave/model/web/SaveStructuredTaskConfigRequest.java`
- Modify: `src/main/java/com/quark/autosave/service/TaskConfigFileService.java`
- Modify: `src/test/java/com/quark/autosave/service/TaskConfigFileServiceTest.java`

- [ ] **Step 1: Write the failing structured save tests**

```java
@Test
void shouldSaveStructuredConfigAsYaml() {
    SaveStructuredTaskConfigRequest request = new SaveStructuredTaskConfigRequest(
        List.of(new EditableAccountRequest("primary", "cookie-value")),
        List.of(new EditableTaskRequest("demo-task", "primary", "https://pan.quark.cn/s/demo", "/folder", ".*", "", true, false, List.of(1, 3, 5), null))
    );

    StructuredTaskConfigDocument saved = service.saveStructured(request);

    assertThat(saved.tasks()).extracting(EditableTaskView::savePath).contains("/folder");
    assertThat(Files.readString(taskFilePath)).contains("save-path: /folder");
}
```

```java
@Test
void shouldRejectStructuredConfigWithUnknownAccount() {
    SaveStructuredTaskConfigRequest request = new SaveStructuredTaskConfigRequest(
        List.of(new EditableAccountRequest("primary", "cookie-value")),
        List.of(new EditableTaskRequest("demo-task", "missing", "https://pan.quark.cn/s/demo", "/folder", ".*", "", true, false, List.of(), null))
    );

    assertThatThrownBy(() -> service.saveStructured(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("账号");
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `mvn -q -Dtest=TaskConfigFileServiceTest test`
Expected: FAIL with missing request classes or missing structured save method

- [ ] **Step 3: Add request records and request validation helpers**

```java
public record SaveStructuredTaskConfigRequest(
    List<EditableAccountRequest> accounts,
    List<EditableTaskRequest> tasks
) {}
```

- [ ] **Step 4: Implement `saveStructured(...)` in `TaskConfigFileService`**

```java
public StructuredTaskConfigDocument saveStructured(SaveStructuredTaskConfigRequest request) {
    validateStructuredRequest(request);
    TaskFileConfig fileConfig = toTaskFileConfig(request);
    String rawYaml = yamlMapper.writeValueAsString(fileConfig);
    save(rawYaml);
    return readStructuredConfig();
}
```

- [ ] **Step 5: Reuse the existing atomic file-write path**

```java
private void writeValidatedYaml(String rawYaml) {
    // keep existing temp-file + load + atomic move behavior
}
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `mvn -q -Dtest=TaskConfigFileServiceTest test`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/quark/autosave/model/web/EditableAccountRequest.java src/main/java/com/quark/autosave/model/web/EditableTaskRequest.java src/main/java/com/quark/autosave/model/web/SaveStructuredTaskConfigRequest.java src/main/java/com/quark/autosave/service/TaskConfigFileService.java src/test/java/com/quark/autosave/service/TaskConfigFileServiceTest.java
git commit -m "feat(config): add structured config save flow"
```

### Task 3: Expose structured config endpoints while keeping advanced YAML fallback

**Files:**
- Modify: `src/main/java/com/quark/autosave/controller/TaskConfigController.java`
- Modify: `src/test/java/com/quark/autosave/controller/TaskConfigControllerTest.java`

- [ ] **Step 1: Write the failing controller tests**

```java
@Test
void shouldReturnStructuredConfigPayload() throws Exception {
    mockMvc.perform(get("/api/config/tasks"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accounts[0].name").value("primary"))
        .andExpect(jsonPath("$.tasks[0].name").value("demo-task"))
        .andExpect(jsonPath("$.advanced.rawYaml").exists());
}
```

```java
@Test
void shouldSaveStructuredConfigPayload() throws Exception {
    mockMvc.perform(put("/api/config/tasks/structured")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"accounts":[{"name":"primary","cookie":"cookie"}],
                 "tasks":[{"name":"demo-task","account":"primary","shareUrl":"https://pan.quark.cn/s/demo","savePath":"/folder","pattern":".*","replace":"","enabled":true,"ignoreExtension":false,"runWeek":[1,3],"endDate":null}]}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tasks[0].savePath").value("/folder"));
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `mvn -q "-Dtest=TaskConfigControllerTest" test`
Expected: FAIL with unexpected payload shape or missing structured save endpoint

- [ ] **Step 3: Update the controller contract**

```java
@GetMapping
public StructuredTaskConfigDocument getCurrentConfig() {
    return taskConfigFileService.readStructuredConfig();
}

@PutMapping("/structured")
public StructuredTaskConfigDocument saveStructured(@RequestBody SaveStructuredTaskConfigRequest request) {
    return taskConfigFileService.saveStructured(request);
}
```

- [ ] **Step 4: Keep the advanced YAML endpoint explicit**

```java
@PutMapping("/advanced")
public TaskConfigDocument saveAdvanced(@RequestBody SaveTaskConfigRequest request) {
    return taskConfigFileService.save(request.rawYaml());
}
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `mvn -q "-Dtest=TaskConfigControllerTest" test`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/quark/autosave/controller/TaskConfigController.java src/test/java/com/quark/autosave/controller/TaskConfigControllerTest.java
git commit -m "feat(config): expose structured config endpoints"
```

## Chunk 2: Form-First Dashboard Layout

Use `@superpowers:test-driven-development` here as well. Lock the HTML structure with rendering tests before writing the new dashboard markup and JS behavior.

### Task 4: Replace the raw editor-first dashboard layout with account and task sections

**Files:**
- Modify: `src/main/resources/templates/dashboard.html`
- Modify: `src/test/java/com/quark/autosave/controller/WebConsolePageRenderingTest.java`

- [ ] **Step 1: Write the failing page rendering test for the new shell**

```java
@Test
void shouldRenderFormFirstDashboardSectionsForAuthenticatedUser() throws Exception {
    mockMvc.perform(get("/").sessionAttr("webConsoleUser", "admin"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("account-section")))
        .andExpect(content().string(containsString("task-section")))
        .andExpect(content().string(containsString("advanced-config-toggle")))
        .andExpect(content().string(containsString("drawer-panel")));
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -q -Dtest=WebConsolePageRenderingTest test`
Expected: FAIL with missing section IDs and drawer container markup

- [ ] **Step 3: Rewrite `dashboard.html` with the new information architecture**

```html
<section class="hero-card" id="dashboard-hero"></section>
<section class="panel" id="account-section"></section>
<section class="panel" id="task-section"></section>
<section class="panel" id="history-section"></section>
<section class="panel panel-wide" id="config-actions-section">
  <button id="save-all-config" type="button">保存全部配置</button>
  <button id="validate-config" type="button">校验配置</button>
  <button id="advanced-config-toggle" type="button">高级模式</button>
</section>
<aside id="drawer-panel" class="drawer-panel" hidden></aside>
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -q -Dtest=WebConsolePageRenderingTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/templates/dashboard.html src/test/java/com/quark/autosave/controller/WebConsolePageRenderingTest.java
git commit -m "feat(ui): add form-first dashboard shell"
```

### Task 5: Rebuild the dashboard script around structured form state and editing drawers

**Files:**
- Modify: `src/main/resources/static/js/web-console.js`
- Modify: `src/main/resources/templates/dashboard.html`

- [ ] **Step 1: Add a minimal rendering test hook for the new controls**

```java
@Test
void shouldExposeDashboardActionsForStructuredEditing() throws Exception {
    mockMvc.perform(get("/").sessionAttr("webConsoleUser", "admin"))
        .andExpect(content().string(containsString("add-account-button")))
        .andExpect(content().string(containsString("add-task-button")))
        .andExpect(content().string(containsString("save-all-config")));
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -q -Dtest=WebConsolePageRenderingTest test`
Expected: FAIL with missing action IDs

- [ ] **Step 3: Add action anchors to the template**

```html
<button id="add-account-button" type="button">新增账号</button>
<button id="add-task-button" type="button">新增任务</button>
```

- [ ] **Step 4: Rewrite the dashboard script around a single `dashboardState` object**

```javascript
const dashboardState = {
  accounts: [],
  tasks: [],
  history: [],
  advancedRawYaml: "",
  dirty: false,
  drawerMode: null,
  editingIndex: null
};
```

```javascript
async function loadDashboard() {
  const [config, history] = await Promise.all([
    fetchJson("/api/config/tasks"),
    fetchJson("/api/history")
  ]);
  dashboardState.accounts = config.accounts || [];
  dashboardState.tasks = config.tasks || [];
  dashboardState.advancedRawYaml = config.advanced?.rawYaml || "";
  renderDashboard();
}
```

- [ ] **Step 5: Add focused renderers and editor actions**

```javascript
function openAccountDrawer(index) { /* prefill account form */ }
function openTaskDrawer(index) { /* prefill task form */ }
function saveAllStructuredConfig() { /* PUT /api/config/tasks/structured */ }
function saveAdvancedConfig() { /* PUT /api/config/tasks/advanced */ }
```

- [ ] **Step 6: Run the rendering test to verify it passes**

Run: `mvn -q -Dtest=WebConsolePageRenderingTest test`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/static/js/web-console.js src/main/resources/templates/dashboard.html src/test/java/com/quark/autosave/controller/WebConsolePageRenderingTest.java
git commit -m "feat(ui): wire structured dashboard interactions"
```

## Chunk 3: Form UX, Validation, and Visual Refresh

Do not widen the backend contract in this chunk. Stay focused on usability, clarity, and presentation.

### Task 6: Add drawer forms with clear field descriptions and lightweight client-side validation

**Files:**
- Modify: `src/main/resources/templates/dashboard.html`
- Modify: `src/main/resources/static/js/web-console.js`

- [ ] **Step 1: Extend the rendering test for key field labels and helper copy**

```java
@Test
void shouldRenderHumanReadableTaskFieldHelp() throws Exception {
    mockMvc.perform(get("/").sessionAttr("webConsoleUser", "admin"))
        .andExpect(content().string(containsString("Quark Cookie")))
        .andExpect(content().string(containsString("文件匹配规则")))
        .andExpect(content().string(containsString("支持正则")))
        .andExpect(content().string(containsString("周一")));
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -q -Dtest=WebConsolePageRenderingTest test`
Expected: FAIL with missing form labels and helper text

- [ ] **Step 3: Add account and task drawer form markup**

```html
<template id="account-drawer-template">...</template>
<template id="task-drawer-template">...</template>
```

Include:
- account name
- cookie
- task name
- account select
- share URL
- save path
- pattern
- replace
- enabled toggle
- ignore extension toggle
- weekday multiselect
- end date

- [ ] **Step 4: Implement lightweight client-side validation**

```javascript
function validateAccountDraft(account) { /* required + unique */ }
function validateTaskDraft(task) { /* required + unique + account exists */ }
```

- [ ] **Step 5: Add unsaved-change guard behavior**

```javascript
function confirmDiscardIfDirty() {
  return !dashboardState.dirty || window.confirm("当前有未保存修改，确定关闭吗？");
}
```

- [ ] **Step 6: Run the rendering test to verify it passes**

Run: `mvn -q -Dtest=WebConsolePageRenderingTest test`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/templates/dashboard.html src/main/resources/static/js/web-console.js src/test/java/com/quark/autosave/controller/WebConsolePageRenderingTest.java
git commit -m "feat(ui): add descriptive drawer forms and validation"
```

### Task 7: Refresh the visual language so the dashboard feels like a polished management console

**Files:**
- Modify: `src/main/resources/static/css/web-console.css`
- Modify: `src/main/resources/templates/dashboard.html`
- Modify: `src/main/resources/templates/login.html`

- [ ] **Step 1: Add a rendering test that locks in the new visual containers**

```java
@Test
void shouldRenderDashboardVisualScaffolding() throws Exception {
    mockMvc.perform(get("/").sessionAttr("webConsoleUser", "admin"))
        .andExpect(content().string(containsString("stats-strip")))
        .andExpect(content().string(containsString("card-collection")))
        .andExpect(content().string(containsString("drawer-surface")));
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -q -Dtest=WebConsolePageRenderingTest test`
Expected: FAIL with missing visual scaffold classes

- [ ] **Step 3: Redesign the CSS tokens and layout primitives**

```css
:root {
  --bg-paper: #f4efe8;
  --bg-mist: #dbe7e2;
  --ink-strong: #18313a;
  --teal-600: #0f766e;
  --sand-100: #fbf8f2;
  --radius-xl: 28px;
}
```

Include:
- tighter hero spacing
- stat chips
- richer card hierarchy
- right-side drawer styling
- mobile-safe stacked layout
- clear empty states

- [ ] **Step 4: Bring login page style in line with the refreshed dashboard**

```html
<main class="login-shell">
  <section class="login-card login-card-refined">...</section>
</main>
```

- [ ] **Step 5: Run the rendering test to verify it passes**

Run: `mvn -q -Dtest=WebConsolePageRenderingTest test`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/static/css/web-console.css src/main/resources/templates/dashboard.html src/main/resources/templates/login.html src/test/java/com/quark/autosave/controller/WebConsolePageRenderingTest.java
git commit -m "feat(ui): refresh dashboard visual design"
```

## Chunk 4: Regression, Docs, and Verification

This chunk makes sure the upgraded UI still works with existing task execution behavior and still leaves `once` mode untouched.

### Task 8: Update docs and tighten regression coverage for the new configuration flow

**Files:**
- Modify: `README.md`
- Modify: `src/test/java/com/quark/autosave/controller/TaskConfigControllerTest.java`
- Modify: `src/test/java/com/quark/autosave/controller/TaskControllerTest.java`
- Modify: `src/test/java/com/quark/autosave/service/StartupModeRunnerTest.java`

- [ ] **Step 1: Add failing regression tests where needed**

```java
@Test
void shouldKeepAdvancedYamlEndpointAvailable() throws Exception {
    mockMvc.perform(put("/api/config/tasks/advanced")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"rawYaml\":\"accounts: []\\ntasks: []\\n\"}"))
        .andExpect(status().isOk());
}
```

```java
@Test
void shouldStillRedirectAnonymousDashboardRequestsToLogin() throws Exception {
    mockMvc.perform(get("/"))
        .andExpect(status().is3xxRedirection());
}
```

- [ ] **Step 2: Run the targeted regression tests to verify they fail where appropriate**

Run: `mvn -q "-Dtest=TaskConfigControllerTest,TaskControllerTest,StartupModeRunnerTest,WebConsolePageRenderingTest" test`
Expected: FAIL until contracts and rendering are fully aligned

- [ ] **Step 3: Update README usage guidance**

Document:
- form-first config editing
- advanced YAML fallback
- default web console credentials and environment overrides
- explicit reminder that GitHub Actions should continue to use `--app.run-mode=once`

- [ ] **Step 4: Run targeted regression tests to verify they pass**

Run: `mvn -q "-Dtest=TaskConfigControllerTest,TaskControllerTest,StartupModeRunnerTest,WebConsolePageRenderingTest" test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add README.md src/test/java/com/quark/autosave/controller/TaskConfigControllerTest.java src/test/java/com/quark/autosave/controller/TaskControllerTest.java src/test/java/com/quark/autosave/service/StartupModeRunnerTest.java src/test/java/com/quark/autosave/controller/WebConsolePageRenderingTest.java
git commit -m "docs(ui): document form editor workflow"
```

### Task 9: Run full verification and smoke checks before handoff

**Files:**
- No source changes expected

- [ ] **Step 1: Run the full automated test suite**

Run: `mvn test`
Expected: PASS with all tests green

- [ ] **Step 2: Build the executable jar**

Run: `mvn -q -DskipTests clean package`
Expected: PASS and generate `target/quark-auto-save-1.0.0-SNAPSHOT-exec.jar`

- [ ] **Step 3: Verify `once` mode still executes and exits**

Run:

```bash
java -jar target/quark-auto-save-1.0.0-SNAPSHOT-exec.jar --app.run-mode=once --app.task-file=config/tasks.example.yml --app.notification.mail.enabled=false
```

Expected: logs show single-run execution and the process exits cleanly

- [ ] **Step 4: Smoke test server mode**

Run:

```bash
java -jar target/quark-auto-save-1.0.0-SNAPSHOT-exec.jar
```

Then verify:
- `/login` returns `200`
- unauthenticated `/api/tasks` returns `401`
- dashboard shows account cards, task cards, and advanced mode toggle after login

- [ ] **Step 5: Final commit if any verification-driven doc or test updates were needed**

```bash
git add README.md src/test/java src/main/resources
git commit -m "test(ui): verify form editor and dashboard refresh"
```
