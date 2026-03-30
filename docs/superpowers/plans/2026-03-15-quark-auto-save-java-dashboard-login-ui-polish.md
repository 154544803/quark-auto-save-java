# Quark Auto Save Java Dashboard and Login UI Polish Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不改变现有业务逻辑、接口调用和数据流的前提下，渐进优化 `login` 与 `dashboard` 页面视觉层级、布局结构、表单体验和中文文案可读性。

**Architecture:** 保持当前 Spring Boot + Thymeleaf + 单文件 CSS + 原生 JavaScript 的实现方式，只调整模板结构、样式系统与前端文案。`dashboard` 重组为“顶部概览 + 左侧主工作区 + 右侧辅助区”，`login` 与其统一视觉语言，所有现有元素 ID 与交互流程保持兼容。

**Tech Stack:** Java 17, Spring Boot Web, Thymeleaf, JUnit 5, MockMvc, HTML, CSS, vanilla JavaScript

---

## Chunk 1: Rendering Guards and Readable Copy

Use `@superpowers:test-driven-development` for this chunk. Lock in the expected page structure and readable Chinese copy before modifying templates or CSS.

### Task 1: Add rendering tests for the new dashboard structure and readable text

**Files:**
- Modify: `src/test/java/com/quark/autosave/controller/WebConsolePageRenderingTest.java`
- Reference: `src/main/resources/templates/dashboard.html`
- Reference: `src/main/resources/templates/login.html`

- [ ] **Step 1: Write the failing rendering assertions for readable Chinese copy**

```java
@Test
void shouldRenderReadableDashboardCopyForAuthenticatedUser() throws Exception {
    mockMvc.perform(get("/").sessionAttr("webConsoleUser", "admin"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("任务管理台")))
        .andExpect(content().string(containsString("账号管理")))
        .andExpect(content().string(containsString("任务管理")))
        .andExpect(content().string(containsString("最近执行记录")))
        .andExpect(content().string(containsString("高级模式")))
        .andExpect(content().string(containsString("文件匹配规则")))
        .andExpect(content().string(containsString("支持正则")))
        .andExpect(content().string(containsString("周一")));
}
```

- [ ] **Step 2: Write the failing rendering assertions for the new layout containers**

```java
@Test
void shouldRenderDashboardMainAndSidebarContainers() throws Exception {
    mockMvc.perform(get("/").sessionAttr("webConsoleUser", "admin"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("dashboard-main")))
        .andExpect(content().string(containsString("dashboard-primary")))
        .andExpect(content().string(containsString("dashboard-secondary")))
        .andExpect(content().string(containsString("hero-actions")));
}
```

- [ ] **Step 3: Run the rendering test to verify it fails**

Run: `mvn -q -Dtest=WebConsolePageRenderingTest test`
Expected: FAIL because the current templates still contain garbled Chinese copy and do not expose the new layout container class names

- [ ] **Step 4: Commit the red test state if you are working in an isolated branch with explicit red-state commits enabled; otherwise leave the change unstaged and continue**

```bash
git diff -- src/test/java/com/quark/autosave/controller/WebConsolePageRenderingTest.java
```

### Task 2: Add login-page rendering expectations that match the unified visual direction

**Files:**
- Modify: `src/test/java/com/quark/autosave/controller/WebConsolePageRenderingTest.java`
- Reference: `src/main/resources/templates/login.html`

- [ ] **Step 1: Write the failing login rendering assertions**

```java
@Test
void shouldRenderReadableLoginCopy() throws Exception {
    mockMvc.perform(get("/login"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("登录")))
        .andExpect(content().string(containsString("用户登录后可管理账号和任务")))
        .andExpect(content().string(containsString("login-card")))
        .andExpect(content().string(containsString("primary-button")));
}
```

- [ ] **Step 2: Run the rendering test to verify it fails**

Run: `mvn -q -Dtest=WebConsolePageRenderingTest test`
Expected: FAIL because the current login template copy is garbled and the descriptive text does not match the approved design

- [ ] **Step 3: Commit the red test state if your execution workflow requires it; otherwise keep moving**

```bash
git diff -- src/test/java/com/quark/autosave/controller/WebConsolePageRenderingTest.java
```

## Chunk 2: Template Structure and Text Refresh

Use `@superpowers:test-driven-development` for this chunk. Do not touch CSS until the HTML structure and readable copy satisfy the rendering tests.

### Task 3: Rebuild `dashboard.html` around the approved “overview + main + sidebar” layout

**Files:**
- Modify: `src/main/resources/templates/dashboard.html`
- Test: `src/test/java/com/quark/autosave/controller/WebConsolePageRenderingTest.java`

- [ ] **Step 1: Update the top-level dashboard structure with stable IDs and new layout wrappers**

```html
<main class="dashboard-shell">
    <section class="hero-card">...</section>
    <section class="dashboard-main">
        <div class="dashboard-primary">
            <section id="account-section" class="panel">...</section>
            <section id="task-section" class="panel">...</section>
        </div>
        <aside class="dashboard-secondary">
            <section id="history-section" class="panel panel-side">...</section>
            <section id="config-actions-section" class="panel panel-side">...</section>
        </aside>
    </section>
</main>
```

- [ ] **Step 2: Replace all garbled dashboard text with UTF-8 Chinese copy that matches the spec**

```html
<h1>任务管理台</h1>
<p class="subtitle">保持当前功能和数据流不变，以更清晰的结构管理账号、任务与配置。</p>
```

- [ ] **Step 3: Keep every existing JavaScript hook stable**

Check that these IDs remain unchanged:
- `run-all`
- `logout-button`
- `account-list`
- `task-list`
- `history-list`
- `validate-config`
- `advanced-config-toggle`
- `save-all-config`
- `advanced-config-panel`
- `drawer-panel`
- `drawer-surface`

- [ ] **Step 4: Run the rendering test to verify it passes**

Run: `mvn -q -Dtest=WebConsolePageRenderingTest test`
Expected: PASS for the dashboard layout and readable copy assertions added in Chunk 1

- [ ] **Step 5: Commit the template structure update**

```bash
git add src/main/resources/templates/dashboard.html src/test/java/com/quark/autosave/controller/WebConsolePageRenderingTest.java
git commit -m "feat(ui): reorganize dashboard layout and readable copy"
```

### Task 4: Refresh `login.html` copy and structure while preserving the existing form flow

**Files:**
- Modify: `src/main/resources/templates/login.html`
- Test: `src/test/java/com/quark/autosave/controller/WebConsolePageRenderingTest.java`

- [ ] **Step 1: Replace garbled login text with readable Chinese copy**

```html
<h1>登录</h1>
<p class="subtitle">用户登录后可管理账号和任务，并按需使用高级 YAML 配置入口。</p>
```

- [ ] **Step 2: Add any lightweight non-breaking structural wrappers needed for visual polish**

```html
<section class="login-card">
    <div class="login-card-body">...</div>
</section>
```

Constraint: do not change these existing hooks:
- `login-form`
- `username`
- `password`
- `login-error`

- [ ] **Step 3: Run the rendering test to verify it passes**

Run: `mvn -q -Dtest=WebConsolePageRenderingTest test`
Expected: PASS for the login readable-copy assertions added in Chunk 1

- [ ] **Step 4: Commit the login template update**

```bash
git add src/main/resources/templates/login.html src/test/java/com/quark/autosave/controller/WebConsolePageRenderingTest.java
git commit -m "feat(ui): refresh login page copy and structure"
```

## Chunk 3: Visual System Refactor in CSS

Use `@superpowers:test-driven-development` for this chunk by locking the expected class usage in the template tests first, then changing styles.

### Task 5: Extend rendering guards for the new visual scaffolding

**Files:**
- Modify: `src/test/java/com/quark/autosave/controller/WebConsolePageRenderingTest.java`
- Reference: `src/main/resources/static/css/web-console.css`

- [ ] **Step 1: Add a failing test for the new structural class names used by the CSS**

```java
@Test
void shouldRenderVisualScaffoldingClasses() throws Exception {
    mockMvc.perform(get("/").sessionAttr("webConsoleUser", "admin"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("stats-strip")))
        .andExpect(content().string(containsString("panel-side")))
        .andExpect(content().string(containsString("drawer-actions-end")))
        .andExpect(content().string(containsString("weekday-group")));
}
```

- [ ] **Step 2: Run the rendering test to verify it fails if any class names are still missing**

Run: `mvn -q -Dtest=WebConsolePageRenderingTest test`
Expected: FAIL until the updated dashboard and drawer templates expose the final visual scaffolding classes consistently

### Task 6: Refactor the CSS tokens and layout primitives to match the approved style

**Files:**
- Modify: `src/main/resources/static/css/web-console.css`
- Reference: `src/main/resources/templates/dashboard.html`
- Reference: `src/main/resources/templates/login.html`
- Test: `src/test/java/com/quark/autosave/controller/WebConsolePageRenderingTest.java`

- [ ] **Step 1: Redefine the design tokens around lower-saturation green and warm neutrals**

```css
:root {
    --bg-paper: #f6f1ea;
    --bg-mist: #dfe9e3;
    --panel: rgba(255, 251, 246, 0.88);
    --panel-strong: rgba(255, 255, 255, 0.94);
    --teal-700: #315e57;
    --teal-600: #4b7a71;
    --ink-strong: #23363c;
}
```

- [ ] **Step 2: Add the new dashboard two-column layout and sidebar behavior**

```css
.dashboard-main {
    display: grid;
    grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.82fr);
    gap: 22px;
}

.dashboard-secondary {
    display: grid;
    gap: 20px;
}
```

- [ ] **Step 3: Refine card, button, form, and drawer styles without changing their semantics**

Include:
- stronger hero hierarchy
- softer glass panels
- clearer button priority
- better field spacing
- steadier drawer footer alignment
- mobile-safe stacking under the existing responsive breakpoint

- [ ] **Step 4: Run the rendering test to verify all locked class names still pass**

Run: `mvn -q -Dtest=WebConsolePageRenderingTest test`
Expected: PASS because CSS changes do not break the tested HTML scaffolding

- [ ] **Step 5: Commit the CSS refactor**

```bash
git add src/main/resources/static/css/web-console.css src/test/java/com/quark/autosave/controller/WebConsolePageRenderingTest.java
git commit -m "feat(ui): refine console visual system"
```

## Chunk 4: JavaScript Copy Sync and Regression Verification

Use `@superpowers:test-driven-development` here as well. Keep JavaScript behavior stable and only adapt text, selectors, and any non-breaking layout assumptions.

### Task 7: Replace garbled JavaScript messages and keep dashboard interactions aligned with the new markup

**Files:**
- Modify: `src/main/resources/static/js/web-console.js`
- Reference: `src/main/resources/templates/dashboard.html`
- Reference: `src/main/resources/templates/login.html`

- [ ] **Step 1: Review every user-facing string in `web-console.js` and map it to readable Chinese**

Examples to replace:

```javascript
showMessage(errorElement, error.message || "登录失败", "error");
```

```javascript
return "任务名称不能为空。";
```

- [ ] **Step 2: Verify the script still queries the same DOM IDs after the template re-layout**

Focus on:
- `bindLoginForm()`
- `bindDashboard()`
- `openAccountDrawer(...)`
- `openTaskDrawer(...)`
- `toggleAdvancedMode()`

- [ ] **Step 3: Run the page and auth tests to verify the JavaScript-facing pages still render and auth still works**

Run: `mvn -q "-Dtest=WebConsolePageRenderingTest,WebConsolePageControllerTest,WebConsoleAuthControllerTest" test`
Expected: PASS because page templates still render, auth endpoints are unchanged, and test-visible copy is now readable

- [ ] **Step 4: Commit the JavaScript copy and compatibility update**

```bash
git add src/main/resources/static/js/web-console.js src/main/resources/templates/dashboard.html src/main/resources/templates/login.html
git commit -m "feat(ui): sync frontend messages with refreshed console layout"
```

### Task 8: Run focused verification before execution handoff

**Files:**
- No source changes expected

- [ ] **Step 1: Run the page-focused regression suite**

Run: `mvn -q "-Dtest=WebConsolePageRenderingTest,WebConsolePageControllerTest,WebConsoleAuthControllerTest" test`
Expected: PASS

- [ ] **Step 2: Build the project without tests to confirm the static resources and templates are packaged cleanly**

Run: `mvn -q -DskipTests compile`
Expected: PASS

- [ ] **Step 3: If any rendering assertions had to be updated during verification, make only the minimal follow-up edit and rerun the same targeted test suite**

Run: `mvn -q "-Dtest=WebConsolePageRenderingTest,WebConsolePageControllerTest,WebConsoleAuthControllerTest" test`
Expected: PASS again after any tiny fix

- [ ] **Step 4: Commit any verification-driven test or copy adjustment**

```bash
git add src/test/java/com/quark/autosave/controller src/main/resources/templates src/main/resources/static
git commit -m "test(ui): verify dashboard and login polish"
```
