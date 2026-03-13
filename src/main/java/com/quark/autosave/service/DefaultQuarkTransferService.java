package com.quark.autosave.service;

import com.quark.autosave.client.QuarkClient;
import com.quark.autosave.model.config.AccountConfig;
import com.quark.autosave.model.config.TaskDefinition;
import com.quark.autosave.model.quark.QuarkFileItem;
import com.quark.autosave.model.quark.ShareParseResult;
import com.quark.autosave.model.runtime.TaskExecutionItem;
import com.quark.autosave.support.RenameRuleEngine;
import com.quark.autosave.support.ShareUrlParser;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class DefaultQuarkTransferService implements QuarkTransferService {

    private final QuarkClient quarkClient;
    private final ShareUrlParser shareUrlParser;
    private final RenameRuleEngine renameRuleEngine;

    public DefaultQuarkTransferService(QuarkClient quarkClient,
                                       ShareUrlParser shareUrlParser,
                                       RenameRuleEngine renameRuleEngine) {
        this.quarkClient = quarkClient;
        this.shareUrlParser = shareUrlParser;
        this.renameRuleEngine = renameRuleEngine;
    }

    @Override
    public TaskExecutionItem execute(AccountConfig accountConfig, TaskDefinition taskDefinition) {
        ShareParseResult shareParseResult = shareUrlParser.parse(taskDefinition.getShareUrl());
        String stoken = quarkClient.getShareToken(accountConfig, shareParseResult);
        List<QuarkFileItem> shareFiles = collectShareFilesRecursively(
            accountConfig,
            shareParseResult,
            stoken,
            new HashSet<>()
        );
        String directoryFid = quarkClient.ensureDirectory(accountConfig, taskDefinition.getSavePath());
        List<String> existingNames = quarkClient.listTargetFileNames(accountConfig, directoryFid);

        List<QuarkFileItem> filesToSave = new ArrayList<>();
        for (QuarkFileItem shareFile : shareFiles) {
            if (!matches(taskDefinition.getPattern(), shareFile.getFileName())) {
                continue;
            }
            String renamedFileName = renameRuleEngine.rename(
                taskDefinition.getPattern(),
                taskDefinition.getReplace(),
                shareFile.getFileName()
            );
            shareFile.setFileNameAfterRename(renamedFileName);
            if (exists(existingNames, renamedFileName, taskDefinition.isIgnoreExtension())) {
                continue;
            }
            filesToSave.add(shareFile);
        }

        if (filesToSave.isEmpty()) {
            return TaskExecutionItem.skipped(taskDefinition.getName(), "没有可转存的新文件");
        }

        List<String> savedFileIds = quarkClient.saveFiles(accountConfig, shareParseResult, stoken, directoryFid, filesToSave);
        for (int index = 0; index < filesToSave.size() && index < savedFileIds.size(); index++) {
            QuarkFileItem fileItem = filesToSave.get(index);
            if (!fileItem.getFileName().equals(fileItem.getFileNameAfterRename())) {
                quarkClient.renameFile(accountConfig, savedFileIds.get(index), fileItem.getFileNameAfterRename());
            }
        }
        return TaskExecutionItem.success(taskDefinition.getName(), "成功转存 " + filesToSave.size() + " 个文件");
    }

    private List<QuarkFileItem> collectShareFilesRecursively(AccountConfig accountConfig,
                                                             ShareParseResult shareParseResult,
                                                             String stoken,
                                                             Set<String> visitedDirectoryIds) {
        String currentDirectoryId = shareParseResult.getPdirFid();
        if (!visitedDirectoryIds.add(currentDirectoryId)) {
            return List.of();
        }

        List<QuarkFileItem> currentLevelFiles = quarkClient.listShareFiles(accountConfig, shareParseResult, stoken);
        List<QuarkFileItem> collectedFiles = new ArrayList<>();
        for (QuarkFileItem currentLevelFile : currentLevelFiles) {
            if (!currentLevelFile.isDir()) {
                collectedFiles.add(currentLevelFile);
                continue;
            }

            // 递归进入分享子目录，避免深层文件因为只扫描当前层而漏转存。
            ShareParseResult subDirectoryResult = copyShareParseResult(shareParseResult, currentLevelFile.getFid());
            collectedFiles.addAll(collectShareFilesRecursively(
                accountConfig,
                subDirectoryResult,
                stoken,
                visitedDirectoryIds
            ));
        }
        return collectedFiles;
    }

    private ShareParseResult copyShareParseResult(ShareParseResult source, String pdirFid) {
        ShareParseResult target = new ShareParseResult();
        target.setPwdId(source.getPwdId());
        target.setPasscode(source.getPasscode());
        target.setPdirFid(pdirFid);
        return target;
    }

    private boolean matches(String pattern, String fileName) {
        if (pattern == null || pattern.isBlank()) {
            return true;
        }
        return Pattern.compile(pattern).matcher(fileName).find();
    }

    private boolean exists(List<String> existingNames, String targetName, boolean ignoreExtension) {
        if (!ignoreExtension) {
            return existingNames.contains(targetName);
        }
        String normalizedTargetName = stripExtension(targetName);
        return existingNames.stream().map(this::stripExtension).anyMatch(normalizedTargetName::equals);
    }

    private String stripExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex < 0 ? fileName : fileName.substring(0, lastDotIndex);
    }
}
