package com.quark.autosave.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RenameRuleEngineTest {

    private final RenameRuleEngine engine = new RenameRuleEngine();

    @Test
    void shouldRenameFileWithRegexReplacement() {
        String renamed = engine.rename("^(\\d+)\\.mp4$", "S02E$1.mp4", "01.mp4");

        assertEquals("S02E01.mp4", renamed);
    }

    @Test
    void shouldKeepOriginalNameWhenReplaceIsBlank() {
        String renamed = engine.rename(".*", "", "01.mp4");

        assertEquals("01.mp4", renamed);
    }
}
