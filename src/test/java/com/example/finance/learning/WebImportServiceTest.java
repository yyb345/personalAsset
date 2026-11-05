package com.example.finance.learning;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import java.net.InetAddress;
import static org.junit.jupiter.api.Assertions.*;

class WebImportServiceTest {
    @Test void validatesUrlsAndBlocksInternalAddresses() throws Exception {
        for (String url : new String[]{"file:///etc/passwd", "http://user:pass@example.com", "http://example.com:8081", "not a url", "https://"})
            assertThrows(WebImportService.ImportFailure.class, () -> WebImportService.validateUrl(url));
        assertEquals("https://example.com/article?a=1", WebImportService.validateUrl("https://example.com/article?a=1#fragment").toString());
        for (String ip : new String[]{"127.0.0.1","10.0.0.1","172.16.0.1","192.168.1.1","169.254.169.254","100.64.0.1","0.0.0.0","224.0.0.1","::1","fc00::1","fe80::1","2001:db8::1","2002:7f00:1::"})
            assertThrows(WebImportService.ImportFailure.class, () -> WebImportService.validateAddresses(new InetAddress[]{InetAddress.getByName(ip)}), ip);
        WebImportService.validateAddresses(new InetAddress[]{InetAddress.getByName("93.184.216.34")});
        assertThrows(WebImportService.ImportFailure.class, () -> WebImportService.validateAddresses(new InetAddress[]{InetAddress.getByName("93.184.216.34"), InetAddress.getByName("127.0.0.1")}));
    }
    @Test void extractsArticleWithoutScriptsOrNavigationAndPreservesParagraphs() {
        var doc = Jsoup.parse("<title>Page title</title><meta property='og:title' content='Learning &amp; discovery'><nav>BAD MENU</nav><article><h1>New ideas</h1><p>Technology helps people learn more about the world.</p><blockquote><p>Good questions lead to useful discoveries.</p></blockquote><script>BAD SCRIPT</script></article><footer>BAD FOOTER</footer>");
        var result = WebImportService.parseDocument(doc, "https://example.com/article");
        assertEquals("Learning & discovery", result.title());
        assertTrue(result.text().contains("\n\n"));
        assertFalse(result.text().contains("BAD"));
        assertEquals(1, result.text().split("Good questions", -1).length - 1);
        assertEquals("https://example.com/article", result.sourceUrl());
    }
    @Test void handlesFallbackAndRejectsEmptyOrOversizeContent() {
        var result = WebImportService.parseDocument(Jsoup.parse("<p>A public article with enough meaningful text for a learning lesson.</p>"), "https://example.com");
        assertFalse(result.text().isBlank());
        assertThrows(WebImportService.ImportFailure.class, () -> WebImportService.parseDocument(Jsoup.parse("<script>empty</script>"), "https://example.com"));
        assertThrows(WebImportService.ImportFailure.class, () -> WebImportService.article("Large", "a".repeat(50001), "https://example.com"));
    }
}
