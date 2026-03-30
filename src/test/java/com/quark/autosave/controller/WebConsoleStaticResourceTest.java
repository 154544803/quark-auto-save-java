package com.quark.autosave.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class WebConsoleStaticResourceTest {

    @Test
    void shouldContainReadableFrontendMessages() throws Exception {
        String script = Files.readString(
            Path.of("src/main/resources/static/js/web-console.js"),
            StandardCharsets.UTF_8
        );

        assertThat(script)
            .contains("登录失败")
            .contains("配置保存成功。")
            .contains("任务名称不能为空。")
            .contains("当前有未保存修改，确定关闭吗？")
            .contains("还没有账号，先新增一个 Quark 账号，再开始创建任务。")
            .contains("还没有任务，新增任务后就能在这里查看和执行。")
            .contains("还没有手动执行记录。");
    }
    @Test
    void shouldHideDrawerPanelWhenHiddenAttributeIsPresent() throws Exception {
        String style = Files.readString(
            Path.of("src/main/resources/static/css/web-console.css"),
            StandardCharsets.UTF_8
        );

        assertThat(style)
            .contains(".drawer-panel[hidden]")
            .contains("display: none");
    }
}
