(function () {
    const pageType = document.body.dataset.page;

    if (pageType === "login") {
        bindLoginForm();
    }

    if (pageType === "dashboard") {
        bindDashboard();
    }

    function bindLoginForm() {
        const form = document.getElementById("login-form");
        const errorElement = document.getElementById("login-error");

        form.addEventListener("submit", async function (event) {
            event.preventDefault();
            hideMessage(errorElement);
            const submitButton = form.querySelector("button[type='submit']");
            setPending(submitButton, true);

            try {
                await fetchJson("/api/auth/login", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({
                        username: document.getElementById("username").value,
                        password: document.getElementById("password").value
                    })
                });
                window.location.href = "/";
            } catch (error) {
                showMessage(errorElement, error.message || "登录失败", "error");
            } finally {
                setPending(submitButton, false);
            }
        });
    }

    function bindDashboard() {
        const runAllButton = document.getElementById("run-all");
        const saveConfigButton = document.getElementById("save-config");
        const logoutButton = document.getElementById("logout-button");

        runAllButton.addEventListener("click", function () {
            runTasks("/api/tasks/run", runAllButton);
        });
        saveConfigButton.addEventListener("click", saveConfig);
        logoutButton.addEventListener("click", logout);

        loadDashboard();
    }

    async function loadDashboard() {
        const [tasks, config, history] = await Promise.all([
            fetchJson("/api/tasks"),
            fetchJson("/api/config/tasks"),
            fetchJson("/api/history")
        ]);
        renderTasks(tasks);
        renderHistory(history);
        document.getElementById("config-editor").value = config.rawYaml || "";
    }

    function renderTasks(tasks) {
        const container = document.getElementById("task-list");
        const count = document.getElementById("task-count");
        count.textContent = tasks.length + " 个任务";

        if (!tasks.length) {
            container.innerHTML = '<div class="empty-state">当前没有可展示的任务。</div>';
            return;
        }

        container.innerHTML = tasks.map(function (task) {
            const runWeek = task.runWeek && task.runWeek.length ? task.runWeek.join(", ") : "未限制";
            const endDate = task.endDate || "未设置";
            const statusClass = task.enabled ? "task-status" : "task-status off";
            const statusText = task.enabled ? "启用中" : "已停用";
            return [
                '<article class="task-row">',
                '  <div class="task-topline">',
                '    <div>',
                '      <div class="task-name">' + escapeHtml(task.name) + "</div>",
                '      <div class="' + statusClass + '">' + statusText + "</div>",
                "    </div>",
                '    <button class="task-action" data-task-name="' + escapeHtml(task.name) + '" type="button">执行此任务</button>',
                "  </div>",
                '  <div class="task-grid">',
                '    <div class="task-meta"><strong>账号</strong><span>' + escapeHtml(task.account || "-") + "</span></div>",
                '    <div class="task-meta"><strong>保存路径</strong><span>' + escapeHtml(task.savePath || "-") + "</span></div>",
                '    <div class="task-meta"><strong>运行星期</strong><span>' + escapeHtml(runWeek) + "</span></div>",
                '    <div class="task-meta"><strong>结束日期</strong><span>' + escapeHtml(endDate) + "</span></div>",
                "  </div>",
                "</article>"
            ].join("");
        }).join("");

        container.querySelectorAll("[data-task-name]").forEach(function (button) {
            button.addEventListener("click", function () {
                runTasks("/api/tasks/run/" + encodeURIComponent(button.dataset.taskName), button);
            });
        });
    }

    function renderHistory(history) {
        const container = document.getElementById("history-list");
        if (!history.length) {
            container.innerHTML = '<div class="empty-state">还没有手动执行记录。</div>';
            return;
        }

        container.innerHTML = history.map(function (entry) {
            const summary = entry.summary || {};
            return [
                '<article class="history-item">',
                '  <div class="history-topline">',
                '    <div>',
                '      <h3>' + escapeHtml(entry.trigger) + "</h3>",
                '      <p class="history-meta">' + escapeHtml(formatDateTime(entry.recordedAt)) + "</p>",
                "    </div>",
                '    <span class="history-badge">手动触发</span>',
                "  </div>",
                '  <div class="history-summary">',
                '    <span><strong>成功</strong> ' + (summary.successCount || 0) + "</span>",
                '    <span><strong>失败</strong> ' + (summary.failureCount || 0) + "</span>",
                '    <span><strong>跳过</strong> ' + (summary.skipCount || 0) + "</span>",
                "  </div>",
                "</article>"
            ].join("");
        }).join("");
    }

    async function runTasks(url, triggerButton) {
        setPending(triggerButton, true);
        try {
            await fetchJson(url, { method: "POST" });
            await loadDashboard();
        } catch (error) {
            alert(error.message || "任务执行失败");
        } finally {
            setPending(triggerButton, false);
        }
    }

    async function saveConfig() {
        const button = document.getElementById("save-config");
        const messageElement = document.getElementById("config-message");
        hideMessage(messageElement);
        setPending(button, true);

        try {
            const response = await fetchJson("/api/config/tasks", {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    rawYaml: document.getElementById("config-editor").value
                })
            });
            document.getElementById("config-editor").value = response.rawYaml || "";
            showMessage(messageElement, "配置保存成功", "info");
            const tasks = await fetchJson("/api/tasks");
            renderTasks(tasks);
        } catch (error) {
            showMessage(messageElement, error.message || "配置保存失败", "error");
        } finally {
            setPending(button, false);
        }
    }

    async function logout() {
        await fetchJson("/api/auth/logout", { method: "POST" });
        window.location.href = "/login";
    }

    async function fetchJson(url, options) {
        const response = await fetch(url, Object.assign({ credentials: "same-origin" }, options || {}));
        if (response.status === 401) {
            window.location.href = "/login";
            throw new Error("未登录");
        }

        const text = await response.text();
        const payload = text ? JSON.parse(text) : {};
        if (!response.ok) {
            throw new Error(payload.message || "请求失败");
        }
        return payload;
    }

    function setPending(button, pending) {
        if (!button) {
            return;
        }
        button.disabled = pending;
    }

    function showMessage(element, text, type) {
        element.hidden = false;
        element.textContent = text;
        element.className = "message " + type;
    }

    function hideMessage(element) {
        element.hidden = true;
        element.textContent = "";
        element.className = "message";
    }

    function formatDateTime(value) {
        if (!value) {
            return "未知时间";
        }
        return value.replace("T", " ");
    }

    function escapeHtml(value) {
        return String(value)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#39;");
    }
})();
