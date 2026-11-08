package com.example.finance.followread;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Select a VTT track by its actual source/translation language, not its display name. */
final class SubtitleTrackSelector {
    record Track(String key, JsonNode format, int priority) { }

    static Track select(JsonNode manual, JsonNode automatic, String target) {
        List<Track> candidates = new ArrayList<>();
        collect(manual, target, true, candidates);
        collect(automatic, target, false, candidates);
        return candidates.stream().min(Comparator.comparingInt(Track::priority)
            .thenComparing(Track::key)).orElse(null);
    }

    private static void collect(JsonNode tracks, String target, boolean manual, List<Track> result) {
        tracks.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            if (!key.matches("[A-Za-z0-9_-]+") || !matches(key, target)) return;
            for (JsonNode format : entry.getValue()) {
                if (!"vtt".equals(format.path("ext").asText())) continue;
                Map<String, String> query;
                try {
                    URI uri = URI.create(format.path("url").asText());
                    if (!Set.of("https", "http").contains(uri.getScheme()) || uri.getHost() == null) continue;
                    query = query(uri);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                String translated = query.get("tlang");
                String source = query.get("lang");
                if (translated != null && !translated.isBlank()) {
                    if (matches(translated, target)) result.add(new Track(key, format, 3));
                } else if (source == null || matches(source, target)) {
                    // Some manual subtitle hosts have no lang query parameter.
                    int rank = manual ? 0 : key.endsWith("-orig") ? 1 : 2;
                    result.add(new Track(key, format, rank));
                }
            }
        });
    }

    private static boolean matches(String actual, String target) {
        if (actual == null || target == null) return false;
        String a = actual.toLowerCase(Locale.ROOT).replace('_', '-').replaceFirst("-orig$", "");
        String t = target.toLowerCase(Locale.ROOT).replace('_', '-').replaceFirst("-orig$", "");
        return a.equals(t) || a.startsWith(t + "-");
    }

    private static Map<String, String> query(URI uri) {
        Map<String, String> result = new HashMap<>();
        if (uri.getRawQuery() != null) {
            for (String part : uri.getRawQuery().split("&")) {
                String[] pair = part.split("=", 2);
                result.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                    pair.length == 2 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "");
            }
        }
        return result;
    }
}
