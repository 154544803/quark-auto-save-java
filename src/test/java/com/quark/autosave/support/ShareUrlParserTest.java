package com.quark.autosave.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.quark.autosave.model.quark.ShareParseResult;
import org.junit.jupiter.api.Test;

class ShareUrlParserTest {

    private final ShareUrlParser parser = new ShareUrlParser();

    @Test
    void shouldParsePwdIdAndPasscode() {
        ShareParseResult result = parser.parse("https://pan.quark.cn/s/d07a34a9c695?pwd=abcd");

        assertEquals("d07a34a9c695", result.getPwdId());
        assertEquals("abcd", result.getPasscode());
        assertEquals("0", result.getPdirFid());
    }

    @Test
    void shouldParseLastPathFidWhenUrlContainsShareFolder() {
        ShareParseResult result = parser.parse(
            "https://pan.quark.cn/s/d07a34a9c695#/list/share/7e25ddd87cf64443b637125478733295-demo/71df3902f42d4270a58c0eb12aa2b014-test"
        );

        assertEquals("71df3902f42d4270a58c0eb12aa2b014", result.getPdirFid());
    }
}
