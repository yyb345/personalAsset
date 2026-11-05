#!/usr/bin/env python3
"""Merge reviewed specialist definitions without losing them on a source recrawl."""
import copy
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SUPPLEMENT = ROOT / 'src/test/resources/glossary/advanced_terms.json'
OUTPUT = ROOT / 'src/main/resources/glossary/terms.json'
FIELDS = {'id', 'termEn', 'termZh', 'aliases', 'definitionEn', 'definitionZh', 'category', 'sources'}


def normalize(name):
    # @ and ^ distinguish pass@k (any success) from pass^k (all successes).
    # Likewise preserve + and # in names such as C++ and C#.
    return re.sub(r'[^a-z0-9\u3400-\u9fff@^+#]', '', name.casefold())


def names(term):
    return [term['termEn'], term['termZh'], *term['aliases']]


def validate(terms):
    seen = set()
    for term in terms:
        if set(term) != FIELDS:
            raise ValueError(f"Unexpected fields in {term.get('id')}")
        if term['id'] in seen:
            raise ValueError(f"Duplicate concept id: {term['id']}")
        seen.add(term['id'])
        for key in ('id', 'termEn', 'termZh', 'definitionEn', 'definitionZh', 'category'):
            if not isinstance(term[key], str) or not term[key].strip():
                raise ValueError(f"Missing {key} in {term['id']}")
        if not re.search(r'[\u3400-\u9fff]', term['definitionZh']):
            raise ValueError(f"Missing Chinese explanation in {term['id']}")
        if not term['aliases'] or any(not isinstance(a, str) or not a.strip() for a in term['aliases']):
            raise ValueError(f"Invalid aliases in {term['id']}")
        if not term['sources'] or any(not s.startswith('https://') for s in term['sources']):
            raise ValueError(f"Missing HTTPS source in {term['id']}")


def merge_terms(base, supplement=None):
    if supplement is None:
        supplement = json.loads(SUPPLEMENT.read_text())
    validate(base)
    validate(supplement)
    indexed = {t['id']: copy.deepcopy(t) for t in base}
    for item in supplement:
        # Stable ids explicitly merge SFT, LoRA, PEFT, ToT and HITL into existing concepts.
        same_id = indexed.get(item['id'])
        wanted = {normalize(n) for n in names(item)}
        for old in indexed.values():
            if old['id'] != item['id'] and wanted & {normalize(n) for n in names(old)}:
                raise ValueError(f"Alias collision: {item['id']} and {old['id']}; merge concepts explicitly")
        replacement = copy.deepcopy(item)
        replacement['aliases'] = list(dict.fromkeys(names(item)))
        if same_id:
            replacement['aliases'] = list(dict.fromkeys(names(item) + names(same_id)))
            replacement['sources'] = list(dict.fromkeys(item['sources'] + same_id['sources']))
        indexed[item['id']] = replacement
    result = sorted(indexed.values(), key=lambda t: (t['termEn'].casefold(), t['id']))
    validate(result)
    return result


if __name__ == '__main__':
    base = json.loads(OUTPUT.read_text())
    result = merge_terms(base)
    OUTPUT.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    print(f'{len(base)} -> {len(result)} concepts; {len(result)-len(base)} added; '
          f'{sum(len(t["aliases"]) for t in result)} names/aliases')
