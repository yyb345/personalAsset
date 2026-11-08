package com.example.finance.followread;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SubtitleTrackSelectorTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ObjectNode manual = mapper.createObjectNode();
    private final ObjectNode automatic = mapper.createObjectNode();

    private void add(ObjectNode tracks, String key, String query) {
        tracks.withArray(key).addObject().put("ext", "vtt")
            .put("url", "https://www.youtube.com/api/timedtext?" + query);
    }
    private SubtitleTrackSelector.Track select() {
        return SubtitleTrackSelector.select(manual, automatic, "en");
    }

    @Test void manualEnglishWinsOverOriginalAndTranslated() {
        add(manual, "en-US", "lang=en-US");
        add(automatic, "en-orig", "lang=en");
        add(automatic, "en", "lang=ja&tlang=en");
        assertEquals("en-US", select().key());
    }
    @Test void originalEnglishWinsEvenWithJapaneseManualCaptions() {
        add(manual, "ja", "lang=ja");
        add(automatic, "en", "lang=ar&tlang=en");
        add(automatic, "en-orig", "lang=en");
        assertEquals("en-orig", select().key());
    }
    @Test void translationIsLastResortWithinTargetLanguage() {
        add(automatic, "en", "lang=ja&tlang=en");
        assertEquals(3, select().priority());
    }
    @Test void rejectsMislabelledSourceAndTranslation() {
        add(manual, "en", "lang=ja");
        add(automatic, "en-orig", "lang=en&tlang=ja");
        assertNull(select());
    }
    @Test void noEnglishDoesNotSelectJapanese() {
        add(manual, "ja", "lang=ja");
        add(automatic, "ja-orig", "lang=ja");
        assertNull(select());
    }
    @Test void emptyAndUnsupportedFormatsAreNotAvailable() {
        automatic.putArray("en-orig").addObject().put("ext", "json3")
            .put("url", "https://www.youtube.com/api/timedtext?lang=en");
        manual.putArray("en");
        assertNull(select());
    }
    @Test void inspectsEachUrlUnderSharedLanguageKey() {
        add(automatic, "en", "lang=en&tlang=ja");
        add(automatic, "en", "lang=en");
        assertEquals(2, select().priority());
    }
}
