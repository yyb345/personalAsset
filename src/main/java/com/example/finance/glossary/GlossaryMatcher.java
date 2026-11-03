package com.example.finance.glossary;

import java.util.*;
import java.util.regex.Pattern;

/** Exact surface spans after percolation; Java offsets are UTF-16, as in JavaScript. */
public final class GlossaryMatcher {
    public record Span(int start, int end, String termId, String termEn, String termZh,
                       String definitionEn, String definitionZh) {}
    private record Entry(GlossaryTerm term, Pattern pattern) {}
    private final List<Entry> entries;

    public GlossaryMatcher(List<GlossaryTerm> terms) {
        entries = terms.stream().map(t -> new Entry(t, Pattern.compile(t.names().stream()
            .sorted(Comparator.comparingInt(String::length).reversed())
            .map(name -> (isLatinWord(name.charAt(0)) ? "(?<![a-zA-Z0-9_])" : "")
                + Pattern.quote(name)
                + (isLatinWord(name.charAt(name.length()-1)) ? "(?![a-zA-Z0-9_])" : ""))
            .collect(java.util.stream.Collectors.joining("|")), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)))
            .toList();
    }
    private static boolean isLatinWord(char c) {
        return c < 128 && (Character.isLetterOrDigit(c) || c == '_');
    }
    public List<Span> match(String text, Set<String> candidates) {
        List<Span> found = new ArrayList<>();
        for (Entry entry : entries) {
            var t = entry.term();
            if (candidates != null && !candidates.contains(t.id())) continue;
            var matcher = entry.pattern().matcher(text);
            while (matcher.find()) found.add(new Span(matcher.start(), matcher.end(), t.id(),
                t.termEn(), t.termZh(), t.definitionEn(), t.definitionZh()));
        }
        // Longest matching concept wins overlaps; stable tie-break by id.
        found.sort(Comparator.<Span>comparingInt(s -> s.end()-s.start()).reversed()
            .thenComparingInt(Span::start).thenComparing(Span::termId));
        var selected = new TreeMap<Integer, Span>();
        for (Span span : found) {
            var before = selected.floorEntry(span.start());
            var after = selected.ceilingEntry(span.start());
            if ((before == null || before.getValue().end() <= span.start())
                && (after == null || after.getKey() >= span.end())) selected.put(span.start(), span);
        }
        return List.copyOf(selected.values());
    }
}
