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
        const state = {
            accounts: [],
            tasks: [],
            history: [],
            advancedRawYaml: "",
            drawerType: null,
            editingIndex: null,
            drawerDirty: false
        };

        const drawerPanel = document.getElementById("drawer-panel");
        const drawerSurface = document.getElementById("drawer-surface");
        const configMessage = document.getElementById("config-message");

        document.getElementById("run-all").addEventListener("click", function (event) {
            runTasks("/api/tasks/run", event.currentTarget, state);
        });
        document.getElementById("logout-button").addEventListener("click", logout);
        document.getElementById("add-account-button").addEventListener("click", function () {
            openAccountDrawer(state, null);
        });
        document.getElementById("add-task-button").addEventListener("click", function () {
            openTaskDrawer(state, null);
        });
        document.getElementById("save-all-config").addEventListener("click", function () {
            saveStructuredConfig(state);
        });
        document.getElementById("validate-config").addEventListener("click", function () {
            validateStructuredDraft(state, true);
        });
        document.getElementById("advanced-config-toggle").addEventListener("click", toggleAdvancedMode);
        document.getElementById("save-advanced-config").addEventListener("click", function () {
            saveAdvancedConfig(state);
        });
        drawerPanel.addEventListener("click", function (event) {
            if (event.target === drawerPanel) {
                closeDrawer(state);
            }
        });

        loadDashboard();

        async function loadDashboard() {
            hideMessage(configMessage);
            const [config, history] = await Promise.all([
                fetchJson("/api/config/tasks"),
                fetchJson("/api/history")
            ]);
            state.accounts = config.accounts || [];
            state.tasks = config.tasks || [];
            state.history = history || [];
            state.advancedRawYaml = config.advanced && config.advanced.rawYaml ? config.advanced.rawYaml : "";
            document.getElementById("config-editor").value = state.advancedRawYaml;
            renderStats(state);
            renderAccounts(state);
            renderTasks(state);
            renderHistory(state);
        }

        function renderStats(currentState) {
            const enabledCount = currentState.tasks.filter(function (task) {
                return task.enabled;
            }).length;
            document.getElementById("account-count").textContent = String(currentState.accounts.length);
            document.getElementById("task-total-count").textContent = String(currentState.tasks.length);
            document.getElementById("task-enabled-count").textContent = String(enabledCount);
            document.getElementById("last-run-at").textContent = currentState.history.length
                ? formatDateTime(currentState.history[0].recordedAt)
                : "暂无";
        }

        function renderAccounts(currentState) {
            const container = document.getElementById("account-list");
            if (!currentState.accounts.length) {
                container.innerHTML = '<div class="empty-state">还没有账号，先新增一个 Quark 账号再创建任务。</div>';
                return;
            }

            container.innerHTML = currentState.accounts.map(function (account, index) {
                return [
                    '<article class="config-card">',
                    '  <div class="config-card-header">',
                    '    <div>',
                    '      <p class="card-kicker">Account</p>',
                    '      <h3>' + escapeHtml(account.name) + '</h3>',
                    '    </div>',
                    '    <span class="status-pill' + (account.cookieConfigured ? '' : ' off') + '">',
                    account.cookieConfigured ? 'Cookie 已配置' : 'Cookie 缺失',
                    '    </span>',
                    '  </div>',
                    '  <dl class="meta-list">',
                    '    <div><dt>关联任务</dt><dd>' + (account.taskCount || 0) + '</dd></div>',
                    '    <div><dt>Cookie</dt><dd>' + maskCookie(account.cookie) + '</dd></div>',
                    '  </dl>',
                    '  <div class="card-actions">',
                    '    <button class="secondary-button" type="button" data-edit-account="' + index + '">编辑</button>',
                    '  </div>',
                    '</article>'
                ].join("");
            }).join("");

            container.querySelectorAll("[data-edit-account]").forEach(function (button) {
                button.addEventListener("click", function () {
                    openAccountDrawer(state, Number(button.dataset.editAccount));
                });
            });
        }

        function renderTasks(currentState) {
            const container = document.getElementById("task-list");
            if (!currentState.tasks.length) {
                container.innerHTML = '<div class="empty-state">还没有任务，新增任务后就能从这里查看和执行。</div>';
                return;
            }

            container.innerHTML = currentState.tasks.map(function (task, index) {
                return [
                    '<article class="config-card">',
                    '  <div class="config-card-header">',
                    '    <div>',
                    '      <p class="card-kicker">Task</p>',
                    '      <h3>' + escapeHtml(task.name) + '</h3>',
                    '    </div>',
                    '    <span class="status-pill' + (task.enabled ? '' : ' off') + '">',
                    task.enabled ? '启用中' : '已停用',
                    '    </span>',
                    '  </div>',
                    '  <dl class="meta-list">',
                    '    <div><dt>所属账号</dt><dd>' + escapeHtml(task.account || "-") + '</dd></div>',
                    '    <div><dt>保存路径</dt><dd>' + escapeHtml(task.savePath || "-") + '</dd></div>',
                    '    <div><dt>运行星期</dt><dd>' + escapeHtml(formatRunWeek(task.runWeek)) + '</dd></div>',
                    '    <div><dt>结束日期</dt><dd>' + escapeHtml(task.endDate || "未设置") + '</dd></div>',
                    '  </dl>',
                    '  <div class="card-actions">',
                    '    <button class="ghost-button" type="button" data-run-task="' + escapeAttribute(task.name) + '">执行任务</button>',
                    '    <button class="secondary-button" type="button" data-edit-task="' + index + '">编辑</button>',
                    '  </div>',
                    '</article>'
                ].join("");
            }).join("");

            container.querySelectorAll("[data-edit-task]").forEach(function (button) {
                button.addEventListener("click", function () {
                    openTaskDrawer(state, Number(button.dataset.editTask));
                });
            });
            container.querySelectorAll("[data-run-task]").forEach(function (button) {
                button.addEventListener("click", function () {
                    runTasks("/api/tasks/run/" + encodeURIComponent(button.dataset.runTask), button, state);
                });
            });
        }

        function renderHistory(currentState) {
            const container = document.getElementById("history-list");
            if (!currentState.history.length) {
                container.innerHTML = '<div class="empty-state">还没有手动执行记录。</div>';
                return;
            }

            container.innerHTML = currentState.history.map(function (entry) {
                const summary = entry.summary || {};
                return [
                    '<article class="history-item">',
                    '  <div class="config-card-header">',
                    '    <div>',
                    '      <p class="card-kicker">Manual Run</p>',
                    '      <h3>' + escapeHtml(entry.trigger || "未知任务") + '</h3>',
                    '      <p class="history-meta">' + escapeHtml(formatDateTime(entry.recordedAt)) + '</p>',
                    '    </div>',
                    '    <span class="status-pill">已记录</span>',
                    '  </div>',
                    '  <div class="history-summary">',
                    '    <span><strong>成功</strong>' + (summary.successCount || 0) + '</span>',
                    '    <span><strong>失败</strong>' + (summary.failureCount || 0) + '</span>',
                    '    <span><strong>跳过</strong>' + (summary.skipCount || 0) + '</span>',
                    '  </div>',
                    '</article>'
                ].join("");
            }).join("");
        }

        function openAccountDrawer(currentState, index) {
            const template = document.getElementById("account-drawer-template");
            drawerSurface.innerHTML = template.innerHTML;
            drawerPanel.hidden = false;
            currentState.drawerType = "account";
            currentState.editingIndex = index;
            currentState.drawerDirty = false;

            const form = document.getElementById("account-form");
            const draft = index === null
                ? { name: "", cookie: "" }
                : Object.assign({}, currentState.accounts[index]);
            document.getElementById("account-drawer-title").textContent = index === null ? "新增账号" : "编辑账号";
            document.getElementById("account-name").value = draft.name || "";
            document.getElementById("account-cookie").value = draft.cookie || "";
            bindDrawerCommonEvents(currentState, form, document.getElementById("account-form-message"));

            form.addEventListener("submit", function (event) {
                event.preventDefault();
                const nextDraft = {
                    name: document.getElementById("account-name").value.trim(),
                    cookie: document.getElementById("account-cookie").value.trim()
                };
                const validationError = validateAccountDraft(currentState, nextDraft, index);
                if (validationError) {
                    showMessage(document.getElementById("account-form-message"), validationError, "error");
                    return;
                }
                if (index === null) {
                    currentState.accounts.push(nextDraft);
                } else {
                    currentState.accounts[index] = Object.assign({}, currentState.accounts[index], nextDraft);
                }
                syncAccountTaskCounts(currentState);
                renderStats(currentState);
                renderAccounts(currentState);
                closeDrawer(currentState, true);
            });

            document.getElementById("delete-account-button").addEventListener("click", function () {
                if (index === null) {
                    closeDrawer(currentState);
                    return;
                }
                const linkedTaskCount = currentState.tasks.filter(function (task) {
                    return task.account === currentState.accounts[index].name;
                }).length;
                if (linkedTaskCount > 0) {
                    showMessage(document.getElementById("account-form-message"), "请先处理关联任务，再删除账号。", "error");
                    return;
                }
                if (window.confirm("确定删除这个账号吗？")) {
                    currentState.accounts.splice(index, 1);
                    renderStats(currentState);
                    renderAccounts(currentState);
                    closeDrawer(currentState, true);
                }
            });
        }

        function openTaskDrawer(currentState, index) {
            const template = document.getElementById("task-drawer-template");
            drawerSurface.innerHTML = template.innerHTML;
            drawerPanel.hidden = false;
            currentState.drawerType = "task";
            currentState.editingIndex = index;
            currentState.drawerDirty = false;

            const form = document.getElementById("task-form");
            const draft = index === null
                ? {
                    name: "",
                    account: currentState.accounts[0] ? currentState.accounts[0].name : "",
                    shareUrl: "",
                    savePath: "",
                    pattern: "",
                    replace: "",
                    enabled: true,
                    ignoreExtension: false,
                    runWeek: [],
                    endDate: null
                }
                : Object.assign({}, currentState.tasks[index], {
                    runWeek: Array.isArray(currentState.tasks[index].runWeek) ? currentState.tasks[index].runWeek.slice() : []
                });

            document.getElementById("task-drawer-title").textContent = index === null ? "新增任务" : "编辑任务";
            const accountSelect = document.getElementById("task-account");
            accountSelect.innerHTML = currentState.accounts.map(function (account) {
                return '<option value="' + escapeAttribute(account.name) + '">' + escapeHtml(account.name) + '</option>';
            }).join("");
            accountSelect.value = draft.account || "";

            document.getElementById("task-name").value = draft.name || "";
            document.getElementById("task-share-url").value = draft.shareUrl || "";
            document.getElementById("task-save-path").value = draft.savePath || "";
            document.getElementById("task-pattern").value = draft.pattern || "";
            document.getElementById("task-replace").value = draft.replace || "";
            document.getElementById("task-enabled").checked = draft.enabled !== false;
            document.getElementById("task-ignore-extension").checked = Boolean(draft.ignoreExtension);
            document.getElementById("task-end-date").value = draft.endDate || "";
            setRunWeekSelection(draft.runWeek || []);
            bindDrawerCommonEvents(currentState, form, document.getElementById("task-form-message"));

            form.addEventListener("submit", function (event) {
                event.preventDefault();
                const nextDraft = {
                    name: document.getElementById("task-name").value.trim(),
                    account: document.getElementById("task-account").value,
                    shareUrl: document.getElementById("task-share-url").value.trim(),
                    savePath: document.getElementById("task-save-path").value.trim(),
                    pattern: document.getElementById("task-pattern").value.trim(),
                    replace: document.getElementById("task-replace").value.trim(),
                    enabled: document.getElementById("task-enabled").checked,
                    ignoreExtension: document.getElementById("task-ignore-extension").checked,
                    runWeek: readRunWeekSelection(),
                    endDate: document.getElementById("task-end-date").value || null
                };
                const validationError = validateTaskDraft(currentState, nextDraft, index);
                if (validationError) {
                    showMessage(document.getElementById("task-form-message"), validationError, "error");
                    return;
                }
                if (index === null) {
                    currentState.tasks.push(nextDraft);
                } else {
                    currentState.tasks[index] = nextDraft;
                }
                syncAccountTaskCounts(currentState);
                renderStats(currentState);
                renderAccounts(currentState);
                renderTasks(currentState);
                closeDrawer(currentState, true);
            });

            document.getElementById("delete-task-button").addEventListener("click", function () {
                if (index === null) {
                    closeDrawer(currentState);
                    return;
                }
                if (window.confirm("确定删除这个任务吗？")) {
                    currentState.tasks.splice(index, 1);
                    syncAccountTaskCounts(currentState);
                    renderStats(currentState);
                    renderAccounts(currentState);
                    renderTasks(currentState);
                    closeDrawer(currentState, true);
                }
            });
        }

        function bindDrawerCommonEvents(currentState, form, messageElement) {
            form.querySelectorAll("input, textarea, select").forEach(function (element) {
                element.addEventListener("input", function () {
                    currentState.drawerDirty = true;
                    hideMessage(messageElement);
                });
                element.addEventListener("change", function () {
                    currentState.drawerDirty = true;
                    hideMessage(messageElement);
                });
            });

            document.querySelectorAll("#close-drawer-button, #cancel-account-button, #cancel-task-button").forEach(function (button) {
                button.addEventListener("click", function () {
                    closeDrawer(currentState);
                });
            });
        }

        function closeDrawer(currentState, forceClose) {
            if (!forceClose && currentState.drawerDirty && !window.confirm("当前有未保存修改，确定关闭吗？")) {
                return;
            }
            drawerPanel.hidden = true;
            drawerSurface.innerHTML = "";
            currentState.drawerType = null;
            currentState.editingIndex = null;
            currentState.drawerDirty = false;
        }

        async function saveStructuredConfig(currentState) {
            const validationError = validateStructuredDraft(currentState, false);
            if (validationError) {
                return;
            }
            const saveButton = document.getElementById("save-all-config");
            setPending(saveButton, true);
            try {
                const response = await fetchJson("/api/config/tasks/structured", {
                    method: "PUT",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({
                        accounts: currentState.accounts.map(function (account) {
                            return {
                                name: account.name,
                                cookie: account.cookie
                            };
                        }),
                        tasks: currentState.tasks
                    })
                });
                currentState.accounts = response.accounts || [];
                currentState.tasks = response.tasks || [];
                currentState.advancedRawYaml = response.advanced && response.advanced.rawYaml ? response.advanced.rawYaml : "";
                document.getElementById("config-editor").value = currentState.advancedRawYaml;
                renderStats(currentState);
                renderAccounts(currentState);
                renderTasks(currentState);
                showMessage(configMessage, "配置保存成功。", "info");
            } catch (error) {
                showMessage(configMessage, error.message || "配置保存失败", "error");
            } finally {
                setPending(saveButton, false);
            }
        }

        function validateStructuredDraft(currentState, showSuccess) {
            hideMessage(configMessage);
            for (let index = 0; index < currentState.accounts.length; index++) {
                const accountError = validateAccountDraft(currentState, currentState.accounts[index], index);
                if (accountError) {
                    showMessage(configMessage, accountError, "error");
                    return accountError;
                }
            }
            for (let index = 0; index < currentState.tasks.length; index++) {
                const taskError = validateTaskDraft(currentState, currentState.tasks[index], index);
                if (taskError) {
                    showMessage(configMessage, taskError, "error");
                    return taskError;
                }
            }
            if (showSuccess) {
                showMessage(configMessage, "表单校验通过，可以保存。", "info");
            }
            return "";
        }

        async function saveAdvancedConfig(currentState) {
            const button = document.getElementById("save-advanced-config");
            setPending(button, true);
            hideMessage(configMessage);
            try {
                const response = await fetchJson("/api/config/tasks/advanced", {
                    method: "PUT",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({
                        rawYaml: document.getElementById("config-editor").value
                    })
                });
                currentState.advancedRawYaml = response.rawYaml || "";
                await loadDashboard();
                showMessage(configMessage, "高级配置保存成功。", "info");
            } catch (error) {
                showMessage(configMessage, error.message || "高级配置保存失败", "error");
            } finally {
                setPending(button, false);
            }
        }

        function toggleAdvancedMode() {
            const panel = document.getElementById("advanced-config-panel");
            panel.hidden = !panel.hidden;
        }

        async function runTasks(url, triggerButton, currentState) {
            setPending(triggerButton, true);
            hideMessage(configMessage);
            try {
                await fetchJson(url, { method: "POST" });
                const history = await fetchJson("/api/history");
                currentState.history = history || [];
                renderStats(currentState);
                renderHistory(currentState);
                showMessage(configMessage, "任务执行完成。", "info");
            } catch (error) {
                showMessage(configMessage, error.message || "任务执行失败", "error");
            } finally {
                setPending(triggerButton, false);
            }
        }

        function syncAccountTaskCounts(currentState) {
            currentState.accounts = currentState.accounts.map(function (account) {
                const taskCount = currentState.tasks.filter(function (task) {
                    return task.account === account.name;
                }).length;
                return Object.assign({}, account, {
                    cookieConfigured: Boolean(account.cookie),
                    taskCount: taskCount
                });
            });
        }

        function validateAccountDraft(currentState, account, currentIndex) {
            if (!account.name) {
                return "账号名称不能为空。";
            }
            if (!account.cookie) {
                return "Quark Cookie 不能为空。";
            }
            const duplicateIndex = currentState.accounts.findIndex(function (item, index) {
                return item.name === account.name && index !== currentIndex;
            });
            if (duplicateIndex >= 0) {
                return "账号名称不能重复。";
            }
            return "";
        }

        function validateTaskDraft(currentState, task, currentIndex) {
            if (!task.name) {
                return "任务名称不能为空。";
            }
            if (!task.account) {
                return "请先选择一个账号。";
            }
            if (!task.shareUrl) {
                return "分享链接不能为空。";
            }
            if (!task.savePath) {
                return "保存路径不能为空。";
            }
            const duplicateIndex = currentState.tasks.findIndex(function (item, index) {
                return item.name === task.name && index !== currentIndex;
            });
            if (duplicateIndex >= 0) {
                return "任务名称不能重复。";
            }
            const accountExists = currentState.accounts.some(function (account) {
                return account.name === task.account;
            });
            if (!accountExists) {
                return "任务绑定的账号不存在。";
            }
            return "";
        }

        async function logout() {
            await fetchJson("/api/auth/logout", { method: "POST" });
            window.location.href = "/login";
        }
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
            return "暂无";
        }
        return value.replace("T", " ").replace(/:\d{2}$/, "");
    }

    function formatRunWeek(runWeek) {
        if (!runWeek || !runWeek.length) {
            return "不限";
        }
        const mapping = {
            1: "周一",
            2: "周二",
            3: "周三",
            4: "周四",
            5: "周五",
            6: "周六",
            7: "周日"
        };
        return runWeek.map(function (day) {
            return mapping[day] || String(day);
        }).join("、");
    }

    function readRunWeekSelection() {
        return Array.from(document.querySelectorAll(".weekday-group input:checked")).map(function (input) {
            return Number(input.value);
        });
    }

    function setRunWeekSelection(runWeek) {
        const selected = new Set(runWeek || []);
        document.querySelectorAll(".weekday-group input").forEach(function (input) {
            input.checked = selected.has(Number(input.value));
        });
    }

    function maskCookie(cookie) {
        if (!cookie) {
            return "未填写";
        }
        if (cookie.length <= 12) {
            return "已填写";
        }
        return cookie.slice(0, 6) + " ... " + cookie.slice(-6);
    }

    function escapeHtml(value) {
        return String(value)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#39;");
    }

    function escapeAttribute(value) {
        return escapeHtml(value);
    }
})();
