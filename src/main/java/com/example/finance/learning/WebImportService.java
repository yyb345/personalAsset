package com.example.finance.learning;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.*;
import java.util.Locale;
import java.util.Set;

@Service
public class WebImportService {
    static final int MAX_BYTES = 2_000_000;
    private static final java.util.concurrent.Semaphore SLOTS = new java.util.concurrent.Semaphore(4);
    private static final java.util.concurrent.ScheduledThreadPoolExecutor DEADLINES;
    static {
        DEADLINES = new java.util.concurrent.ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = new Thread(runnable, "web-import-deadline"); thread.setDaemon(true); return thread;
        });
        DEADLINES.setRemoveOnCancelPolicy(true);
    }
    public record Article(String title, String text, String sourceUrl) {}
    public static class ImportFailure extends RuntimeException {
        final int status;
        ImportFailure(int status, String message) { super(message); this.status = status; }
    }

    public Article extract(String input) {
        if (!SLOTS.tryAcquire()) throw new ImportFailure(429, "当前导入任务较多，请稍后重试。");
        try { return extractInternal(input); } finally { SLOTS.release(); }
    }

    private Article extractInternal(String input) {
        URI uri = validateUrl(input);
        try {
            for (int hop = 0; hop <= 3; hop++) {
                InetAddress[] addresses = InetAddress.getAllByName(uri.getHost());
                validateAddresses(addresses);
                String host = uri.getHost();
                // Pin the checked DNS results for this connection, including TLS hostname validation.
                try (var client = HttpClients.custom()
                        .setDnsResolver(requested -> {
                            if (!requested.equalsIgnoreCase(host)) throw new UnknownHostException();
                            return addresses;
                        })
                        .disableRedirectHandling().disableAutomaticRetries().disableCookieManagement()
                        .setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(5000)
                                .setSocketTimeout(7000).setConnectionRequestTimeout(3000).build()).build()) {
                    HttpGet request = new HttpGet(uri);
                    request.setHeader("User-Agent", "XLearning-ArticleImporter/1.0");
                    request.setHeader("Accept", "text/html,application/xhtml+xml,text/plain;q=0.8");
                    var deadline = DEADLINES.schedule(request::abort, 12, java.util.concurrent.TimeUnit.SECONDS);
                    try (var response = client.execute(request)) {
                        int status = response.getStatusLine().getStatusCode();
                        if (Set.of(301,302,303,307,308).contains(status)) {
                            var location = response.getFirstHeader("Location");
                            if (location == null || hop == 3) throw new ImportFailure(422, "网页跳转过多或缺少跳转地址。");
                            uri = validateUrl(uri.resolve(location.getValue()).toString());
                            continue;
                        }
                        if (status != 200) throw new ImportFailure(422, "网站返回 HTTP " + status + "，请尝试粘贴正文。");
                        var entity = response.getEntity();
                        if (entity == null) throw new ImportFailure(422, "网页没有正文。");
                        String type = entity.getContentType() == null ? "" : entity.getContentType().getValue().toLowerCase(Locale.ROOT);
                        if (!(type.startsWith("text/html") || type.startsWith("application/xhtml+xml") || type.startsWith("text/plain")))
                            throw new ImportFailure(422, "仅支持 HTML 或纯文本网页，请改用文章链接。");
                        if (entity.getContentLength() > MAX_BYTES) throw new ImportFailure(422, "网页过大，请粘贴需要学习的段落。");
                        try (var stream = entity.getContent()) {
                            byte[] bytes = stream.readNBytes(MAX_BYTES + 1);
                            if (bytes.length > MAX_BYTES) throw new ImportFailure(422, "网页过大，请粘贴需要学习的段落。");
                            if (type.startsWith("text/plain")) {
                                String text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8).trim();
                                return article(uri.getHost(), text, uri.toString());
                            }
                            Document doc = Jsoup.parse(new java.io.ByteArrayInputStream(bytes), null, uri.toString());
                            return parseDocument(doc, uri.toString());
                        }
                    } finally { deadline.cancel(false); }
                }
            }
        } catch (ImportFailure ex) { throw ex; }
        catch (SocketTimeoutException ex) { throw new ImportFailure(504, "网页读取超时，请稍后重试或粘贴正文。"); }
        catch (IOException | IllegalArgumentException ex) { throw new ImportFailure(502, "无法读取该网页，请检查链接或粘贴正文。"); }
        throw new ImportFailure(422, "无法完成网页跳转。");
    }

    static URI validateUrl(String input) {
        try {
            if (input == null || input.length() > 2048) throw new IllegalArgumentException();
            URI uri = new URI(input.trim());
            String scheme = uri.getScheme();
            if (!("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) || uri.getHost() == null
                    || uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 80 && uri.getPort() != 443))
                throw new IllegalArgumentException();
            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), null);
        } catch (Exception ex) { throw new ImportFailure(400, "请输入有效的 HTTP/HTTPS 网页链接（仅支持标准网页端口）。"); }
    }

    static void validateAddresses(InetAddress[] addresses) {
        if (addresses.length == 0) throw new ImportFailure(400, "无法解析网址。");
        for (InetAddress address : addresses) {
            byte[] bytes = address.getAddress();
            boolean blocked = address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress();
            if (bytes.length == 4) {
                int a = bytes[0] & 255, b = bytes[1] & 255;
                blocked |= a == 0 || a == 10 || a == 127 || a >= 224 || (a == 100 && b >= 64 && b <= 127)
                        || (a == 169 && b == 254) || (a == 172 && b >= 16 && b <= 31)
                        || (a == 192 && (b == 0 || b == 168)) || (a == 198 && (b == 18 || b == 19 || b == 51))
                        || (a == 203 && b == 0);
            } else {
                // Allow only global IPv6 unicast; exclude documentation and transition ranges.
                blocked |= (bytes[0] & 0xe0) != 0x20 || (bytes[0] == 0x20 && bytes[1] == 0x01
                        && ((bytes[2] & 255) <= 1 || ((bytes[2] & 255) == 0x0d && (bytes[3] & 255) == 0xb8)))
                        || (bytes[0] == 0x20 && bytes[1] == 0x02);
            }
            if (blocked) throw new ImportFailure(400, "仅支持公开互联网网页，不能访问本机或内网地址。");
        }
    }

    static Article parseDocument(Document doc, String url) {
        String title = doc.title();
        Element ogTitle = doc.selectFirst("meta[property=og:title]");
        if (ogTitle != null && !ogTitle.attr("content").isBlank()) title = ogTitle.attr("content");
        doc.select("script,style,noscript,iframe,svg,nav,header,footer,aside,form,button,[hidden],[aria-hidden=true],.cookie-banner,.advertisement").remove();
        Element root = null;
        int best = -1;
        for (Element candidate : doc.select("article,main,[role=main],.entry-content,.post-content,.article-body")) {
            int score = candidate.select("p").text().length();
            if (score > best) { root = candidate; best = score; }
        }
        if (root == null) root = doc.body();
        final Element selectedRoot = root;
        StringBuilder text = new StringBuilder();
        for (Element block : root.select("h1,h2,h3,p,li,blockquote,pre")) {
            if (block.parents().stream().anyMatch(parent -> parent != selectedRoot && parent != block && Set.of("p","li","blockquote","pre").contains(parent.tagName()))) continue;
            String line = block.text().trim();
            if (!line.isEmpty()) text.append(line).append("\n\n");
        }
        return article(title.isBlank() ? root.select("h1").text() : title,
                text.isEmpty() ? root.text() : text.toString().trim(), url);
    }

    static Article article(String title, String text, String url) {
        if (text.length() < 40) throw new ImportFailure(422, "未找到足够的正文，页面可能需要登录或动态加载，请粘贴正文。");
        if (text.length() > 50000) throw new ImportFailure(422, "正文超过 50,000 字符，请粘贴需要学习的段落。");
        return new Article(title.isBlank() ? "Imported article" : title.substring(0, Math.min(160,title.length())), text, url);
    }
}
