#!/usr/bin/env python3
"""Collect attributed, bilingual Google AI glossary entries (BeautifulSoup4 required)."""
import argparse, hashlib, json, re, urllib.request
from pathlib import Path
from bs4 import BeautifulSoup

ROOT = Path(__file__).resolve().parents[2]
SOURCES = [('education', 'https://developers.google.com/machine-learning/glossary'), ('cloud', 'https://cloud.google.com/vertex-ai/docs/glossary')]

def clean(value):
    return re.sub(r'\s+', ' ', re.sub(r'[\ue000-\uf8ff]', '', value)).strip()

def parse(html, kind):
    body = BeautifulSoup(html, 'html.parser').select_one('.devsite-article-body')
    result = {}
    if kind == 'cloud':
        for item in body.select('li.termDisplay'):
            heading, definition = item.find('h5'), item.select_one('.termDefinition')
            if heading and definition:
                result[heading['id']] = (clean(heading.get_text(' ', strip=True)), clean(definition.get_text(' ', strip=True)), [], [])
        return result
    for heading in body.find_all('h2'):
        key = heading.get('id', '')
        if len(key) < 2 or key == 'key-takeaways-panel-title':
            continue
        paragraphs, links, tags = [], [], []
        for node in heading.next_elements:
            if getattr(node, 'name', None) == 'h2':
                break
            if getattr(node, 'name', None) in ('p', 'li'):
                # Ignore layout-only paragraphs containing headings.
                if node.find(['h2', 'h3', 'li']):
                    continue
                value = clean(node.get_text(' ', strip=True))
                if value:
                    paragraphs.append(value)
                    links.extend(a.get('href', '') for a in node.find_all('a'))
            if getattr(node, 'name', None) == 'div' and 'glossary-icon' in node.get('class', []):
                tags.append(node.get('title', ''))
        if paragraphs:
            result[key] = (clean(heading.get_text(' ', strip=True)), ' '.join(paragraphs[:5]), links, tags)
    return result

def short(text, zh=False):
    limit = 450 if zh else 850
    if len(text) <= limit: return text
    piece = text[:limit]
    cuts = [piece.rfind(c) for c in ('。', '！', '？')] if zh else [piece.rfind('. '), piece.rfind('; ')]
    end = max(cuts)
    return piece[:end+1] if end > limit//3 else piece.rstrip() + '…'

def main():
    parser = argparse.ArgumentParser(); parser.add_argument('--cache-dir', type=Path, default=Path('/tmp/ai-glossary-cache'))
    args = parser.parse_args(); args.cache_dir.mkdir(parents=True, exist_ok=True)
    records, aliases, seen = [], {}, set()
    for kind, url in SOURCES:
        languages = {}
        for lang in ('en', 'zh-cn'):
            path = args.cache_dir / f'{kind}-{lang}.html'
            if not path.exists():
                path.write_bytes(urllib.request.urlopen(url+'?hl='+lang, timeout=60).read())
            languages[lang] = parse(path.read_text(), kind)
        en, zh = languages['en'], languages['zh-cn']
        for key, (name, definition, links, tags) in en.items():
            if key not in zh: continue
            zname, zdefinition, _, _ = zh[key]
            # Merge acronym / "see" redirects with their canonical concept.
            if re.match(r'^(Abbreviation|Acronym|See |Another name|Synonym)', definition):
                target = next((link.split('#')[-1] for link in links if '#' in link), None)
                if target: aliases.setdefault(target, []).extend([name, zname])
                continue
            if len(definition) < 30 or len(zdefinition) < 10: continue
            norm = re.sub(r'[^a-z0-9]', '', name.lower())
            if norm in seen: continue
            seen.add(norm)
            display = name
            term_aliases = [name, zname]
            term_aliases += [re.sub(r'\s*[（(][^）)]*[）)]', '', zname).strip()]
            if name.lower() == 'token':
                zname = '词元'; term_aliases += ['词元', '令牌']
            if name.lower() == 'attention':
                zname = '注意力机制'; term_aliases += ['注意力', '注意力机制']
            # Include parenthetical abbreviations but keep one record per concept.
            match = re.match(r'^(.+?)\s*\(([A-Za-z][A-Za-z0-9-]{1,15})\)$', name)
            if match: display = match[1]; term_aliases.append(match[2])
            records.append({'id': 'ai-'+kind+'-'+key, 'termEn': display, 'termZh': zname, 'aliases': term_aliases, 'definitionEn': short(definition), 'definitionZh': short(zdefinition, True), 'category': (tags[0] if tags else ('AI infrastructure' if kind=='cloud' else 'Machine learning')), 'sources': [url+'#'+key, url+'?hl=zh-cn#'+key], '_key': key})
    for record in records:
        record['aliases'] = list(dict.fromkeys(record['aliases'] + aliases.get(record.pop('_key'), [])))
    # Consolidate independently described cloud/education variants with identical English core labels.
    unique = {}
    for record in records:
        normalized = re.sub(r'[^a-z0-9]', '', record['termEn'].lower())
        if normalized in unique:
            prior = unique[normalized]
            prior['aliases'] = list(dict.fromkeys(prior['aliases']+record['aliases']))
            prior['sources'] += record['sources']
        else: unique[normalized] = record
    records = sorted(unique.values(), key=lambda r:r['termEn'].lower())
    # Explicit surface forms found in the real subtitle regression fixture.
    # Keep inflections as aliases of a concept, never as extra dictionary entries.
    plural_aliases = {'token': ['tokens'], 'large language model': ['large language models', 'LLMs']}
    for record in records:
        record['aliases'] = list(dict.fromkeys(record['aliases'] + plural_aliases.get(record['termEn'].lower(), [])))
    assert len(records) >= 500, f'Only {len(records)} concepts; source markup may have changed'
    assert len({r['id'] for r in records}) == len(records)
    output = ROOT/'src/main/resources/glossary/terms.json'
    output.write_text(json.dumps(records, ensure_ascii=False, indent=2)+'\n')
    print(f'Wrote {len(records)} unique bilingual concepts to {output}')

if __name__ == '__main__': main()
