package com.quark.autosave.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WebConsolePageRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRenderLoginFormForAnonymousUser() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("login-form")))
            .andExpect(content().string(containsString("Quark Auto Save")))
            .andExpect(content().string(containsString("登录")))
            .andExpect(content().string(containsString("用户登录后可管理账号和任务")))
            .andExpect(content().string(containsString("primary-button")));
    }

    @Test
    void shouldRenderFormFirstDashboardSectionsForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/").sessionAttr("webConsoleUser", "admin"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("account-section")))
            .andExpect(content().string(containsString("task-section")))
            .andExpect(content().string(containsString("history-section")))
            .andExpect(content().string(containsString("advanced-config-toggle")))
            .andExpect(content().string(containsString("drawer-panel")))
            .andExpect(content().string(containsString("add-account-button")))
            .andExpect(content().string(containsString("add-task-button")))
            .andExpect(content().string(containsString("save-all-config")))
            .andExpect(content().string(containsString("stats-strip")))
            .andExpect(content().string(containsString("card-collection")))
            .andExpect(content().string(containsString("drawer-surface")))
            .andExpect(content().string(containsString("dashboard-main")))
            .andExpect(content().string(containsString("dashboard-primary")))
            .andExpect(content().string(containsString("dashboard-secondary")))
            .andExpect(content().string(containsString("hero-actions")))
            .andExpect(content().string(containsString("Quark Cookie")))
            .andExpect(content().string(containsString("任务管理台")))
            .andExpect(content().string(containsString("账号管理")))
            .andExpect(content().string(containsString("任务管理")))
            .andExpect(content().string(containsString("最近执行记录")))
            .andExpect(content().string(containsString("高级模式")))
            .andExpect(content().string(containsString("文件匹配规则")))
            .andExpect(content().string(containsString("支持正则")))
            .andExpect(content().string(containsString("周一")));
    }

    @Test
    void shouldRenderVisualScaffoldingClassesForPolishedLayout() throws Exception {
        mockMvc.perform(get("/").sessionAttr("webConsoleUser", "admin"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("hero-metrics")))
            .andExpect(content().string(containsString("panel-header-copy")))
            .andExpect(content().string(containsString("panel-side")))
            .andExpect(content().string(containsString("drawer-actions-end")));
    }
}
