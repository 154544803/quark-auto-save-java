package com.quark.autosave.client;

import com.quark.autosave.model.config.AccountConfig;
import com.quark.autosave.model.quark.QuarkFileItem;
import com.quark.autosave.model.quark.ShareParseResult;
import java.util.List;

public interface QuarkClient {

    String getShareToken(AccountConfig accountConfig, ShareParseResult shareParseResult);

    List<QuarkFileItem> listShareFiles(AccountConfig accountConfig, ShareParseResult shareParseResult, String stoken);

    String ensureDirectory(AccountConfig accountConfig, String savePath);

    List<String> listTargetFileNames(AccountConfig accountConfig, String directoryFid);

    List<String> saveFiles(AccountConfig accountConfig, ShareParseResult shareParseResult, String stoken,
                           String directoryFid, List<QuarkFileItem> fileItems);

    void renameFile(AccountConfig accountConfig, String fileId, String targetFileName);
}
