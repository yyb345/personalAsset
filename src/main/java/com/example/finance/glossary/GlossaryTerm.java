package com.example.finance.glossary;

import java.util.List;

public record GlossaryTerm(String id, String termEn, String termZh, List<String> aliases,
                           String definitionEn, String definitionZh, String category, List<String> sources) {
    public List<String> names() {
        var names = new java.util.LinkedHashSet<String>();
        if (termEn != null && !termEn.isBlank()) names.add(termEn);
        if (termZh != null && !termZh.isBlank()) names.add(termZh);
        if (aliases != null) aliases.stream().filter(s -> s != null && !s.isBlank()).forEach(names::add);
        return List.copyOf(names);
    }
}
