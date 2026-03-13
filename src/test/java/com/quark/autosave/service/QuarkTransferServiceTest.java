package com.quark.autosave.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quark.autosave.client.QuarkClient;
import com.quark.autosave.model.config.AccountConfig;
import com.quark.autosave.model.config.TaskDefinition;
import com.quark.autosave.model.quark.QuarkFileItem;
import com.quark.autosave.model.runtime.TaskExecutionItem;
import com.quark.autosave.support.RenameRuleEngine;
import com.quark.autosave.support.ShareUrlParser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class QuarkTransferServiceTest {

    @Test
    void shouldSaveMatchedFilesAndRenameWhenNeeded() {
        QuarkClient client = Mockito.mock(QuarkClient.class);
        DefaultQuarkTransferService service = new DefaultQuarkTransferService(client, new ShareUrlParser(), new RenameRuleEngine());
        AccountConfig account = buildAccount();
        TaskDefinition task = buildTask("^(\\d+)\\.mp4$", "S01E$1.mp4");

        when(client.getShareToken(any(), any())).thenReturn("stoken");
        when(client.listShareFiles(any(), any(), eq("stoken"))).thenReturn(List.of(
            buildFile("fid-1", "token-1", "01.mp4", false),
            buildFile("fid-2", "token-2", "说明.txt", false)
        ));
        when(client.ensureDirectory(any(), eq("/demo"))).thenReturn("dir-fid");
        when(client.listTargetFileNames(any(), eq("dir-fid"))).thenReturn(List.of());
        when(client.saveFiles(any(), any(), eq("stoken"), eq("dir-fid"), any())).thenReturn(List.of("saved-fid-1"));

        TaskExecutionItem item = service.execute(account, task);

        assertEquals("SUCCESS", item.getStatus());
        verify(client, times(1)).renameFile(account, "saved-fid-1", "S01E01.mp4");
    }

    @Test
    void shouldSkipWhenNoNewMatchedFileExists() {
        QuarkClient client = Mockito.mock(QuarkClient.class);
        DefaultQuarkTransferService service = new DefaultQuarkTransferService(client, new ShareUrlParser(), new RenameRuleEngine());
        AccountConfig account = buildAccount();
        TaskDefinition task = buildTask(".*\\.mp4$", "");

        when(client.getShareToken(any(), any())).thenReturn("stoken");
        when(client.listShareFiles(any(), any(), eq("stoken"))).thenReturn(List.of(
            buildFile("fid-1", "token-1", "01.mp4", false)
        ));
        when(client.ensureDirectory(any(), eq("/demo"))).thenReturn("dir-fid");
        when(client.listTargetFileNames(any(), eq("dir-fid"))).thenReturn(List.of("01.mp4"));

        TaskExecutionItem item = service.execute(account, task);

        assertEquals("SKIPPED", item.getStatus());
    }

    @Test
    void shouldTraverseNestedShareDirectoriesAndSaveMatchedFiles() {
        QuarkClient client = Mockito.mock(QuarkClient.class);
        DefaultQuarkTransferService service = new DefaultQuarkTransferService(client, new ShareUrlParser(), new RenameRuleEngine());
        AccountConfig account = buildAccount();
        TaskDefinition task = buildTask(".*\\.(mp4|mkv)$", "");

        when(client.getShareToken(any(), any())).thenReturn("stoken");
        when(client.listShareFiles(any(), argThat(result -> result != null && "0".equals(result.getPdirFid())), eq("stoken")))
            .thenReturn(List.of(buildFile("dir-1", "token-dir-1", "周日仙逆", true)));
        when(client.listShareFiles(any(), argThat(result -> result != null && "dir-1".equals(result.getPdirFid())), eq("stoken")))
            .thenReturn(List.of(buildFile("file-1", "token-file-1", "01.mp4", false)));
        when(client.ensureDirectory(any(), eq("/demo"))).thenReturn("dir-fid");
        when(client.listTargetFileNames(any(), eq("dir-fid"))).thenReturn(List.of());
        when(client.saveFiles(any(), any(), eq("stoken"), eq("dir-fid"), any())).thenReturn(List.of("saved-fid-1"));

        TaskExecutionItem item = service.execute(account, task);

        assertEquals("SUCCESS", item.getStatus());
        verify(client, times(1)).saveFiles(
            any(),
            any(),
            eq("stoken"),
            eq("dir-fid"),
            argThat(files -> files.size() == 1 && "01.mp4".equals(files.get(0).getFileName()))
        );
    }

    private AccountConfig buildAccount() {
        AccountConfig account = new AccountConfig();
        account.setName("primary");
        account.setCookie("cookie");
        return account;
    }

    private TaskDefinition buildTask(String pattern, String replace) {
        TaskDefinition task = new TaskDefinition();
        task.setName("demo-task");
        task.setAccount("primary");
        task.setShareUrl("https://pan.quark.cn/s/demo");
        task.setSavePath("/demo");
        task.setPattern(pattern);
        task.setReplace(replace);
        task.setEnabled(true);
        return task;
    }

    private QuarkFileItem buildFile(String fid, String token, String fileName, boolean dir) {
        QuarkFileItem fileItem = new QuarkFileItem();
        fileItem.setFid(fid);
        fileItem.setShareFidToken(token);
        fileItem.setFileName(fileName);
        fileItem.setDir(dir);
        return fileItem;
    }
}
