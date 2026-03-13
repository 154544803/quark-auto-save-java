package com.quark.autosave.support;

import com.quark.autosave.model.quark.ShareParseResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ShareUrlParser {

    private static final Pattern PWD_ID_PATTERN = Pattern.compile("/s/(\\w+)");
    private static final Pattern PASSCODE_PATTERN = Pattern.compile("pwd=(\\w+)");
    private static final Pattern FID_PATTERN = Pattern.compile("/(\\w{32})-?([^/]+)?");

    public ShareParseResult parse(String url) {
        ShareParseResult result = new ShareParseResult();
        result.setPwdId(matchGroup(PWD_ID_PATTERN, url));
        result.setPasscode(defaultString(matchGroup(PASSCODE_PATTERN, url)));

        Matcher matcher = FID_PATTERN.matcher(url);
        String lastFid = "0";
        while (matcher.find()) {
            lastFid = matcher.group(1);
        }
        result.setPdirFid(lastFid);
        return result;
    }

    private String matchGroup(Pattern pattern, String url) {
        Matcher matcher = pattern.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
