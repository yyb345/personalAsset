#!/usr/bin/env python3
"""Analyze local GSC query/page CSV exports. No network access or third-party dependencies."""
import argparse
import csv
import math
import re
from pathlib import Path

DEFAULT_BRAND = r'(?<![a-z0-9])x[\s._-]*learning(?:\.top)?(?![a-z0-9])'
ALIASES = {
    'query': ('Top queries', 'Query', '热门查询', '查询'),
    'page': ('Top pages', 'Page', '热门网页', '网页', '页面'),
    'clicks': ('Clicks', '点击次数', '点击量'),
    'impressions': ('Impressions', '展示次数', '展现量'),
    'position': ('Position', 'Average position', '排名', '平均排名'),
}

def read_export(path, dimension):
    with Path(path).open(encoding='utf-8-sig', newline='') as file:
        reader = csv.DictReader(file)
        headers = {str(name).strip().casefold(): name for name in reader.fieldnames or []}
        selected = {}
        for key in (dimension, 'clicks', 'impressions', 'position'):
            selected[key] = next((headers[name.casefold()] for name in ALIASES[key] if name.casefold() in headers), None)
            if not selected[key]:
                raise ValueError(f'{path}: missing {key} column; available: {reader.fieldnames}')
        rows = []
        for line, row in enumerate(reader, 2):
            try:
                number = lambda key: float(row[selected[key]].replace(',', '').strip())
                clicks, impressions, position = number('clicks'), number('impressions'), number('position')
                if not all(math.isfinite(value) and value >= 0 for value in (clicks, impressions, position)):
                    raise ValueError('metrics must be finite and nonnegative')
                rows.append({'label': row[selected[dimension]].strip(), 'clicks': clicks, 'impressions': impressions, 'position': position})
            except (ValueError, AttributeError) as error:
                raise ValueError(f'{path}:{line}: invalid metric ({error})') from error
        return rows

def totals(rows):
    clicks = sum(row['clicks'] for row in rows)
    impressions = sum(row['impressions'] for row in rows)
    position = sum(row['position'] * row['impressions'] for row in rows) / impressions if impressions else 0
    return clicks, impressions, f'{clicks / impressions:.2%}' if impressions else '—', f'{position:.2f}' if impressions else '—'

def safe(value):
    return str(value).replace('|', '\\|').replace('\n', ' ').replace('\r', ' ')

def summary_row(label, rows):
    clicks, impressions, ctr, position = totals(rows)
    return f'| {safe(label)} | {len(rows)} | {clicks:g} | {impressions:g} | {ctr} | {position} |'

def report(queries, pages, brand, label, limit):
    branded = [row for row in queries if brand.search(row['label'])]
    nonbrand = [row for row in queries if not brand.search(row['label'])]
    lines = [f'## {safe(label)}', '', '| Scope | Rows | Clicks | Impressions | CTR | Weighted position |', '| --- | ---: | ---: | ---: | ---: | ---: |', summary_row('Visible queries', queries), summary_row('Brand queries', branded), summary_row('Nonbrand queries', nonbrand), summary_row('Exported pages', pages), '', 'Query exports omit anonymized queries and may be row-limited. Nonbrand means only visible queries not matched by the configured expression; it is not the site-wide nonbrand total. Weighted positions summarize exported rows only.', '', f'Brand expression: `{safe(brand.pattern)}`', '', f'### Nonbrand queries (top {limit} by impressions)', '', '| Query | Clicks | Impressions | CTR | Position |', '| --- | ---: | ---: | ---: | ---: |']
    for row in sorted(nonbrand, key=lambda row: row['impressions'], reverse=True)[:limit]:
        clicks, impressions, ctr, position = totals([row])
        lines.append(f"| {safe(row['label'])} | {clicks:g} | {impressions:g} | {ctr} | {position} |")
    lines += ['', f'### Pages (top {limit} by impressions)', '', '| Page | Clicks | Impressions | CTR | Position |', '| --- | ---: | ---: | ---: | ---: |']
    for row in sorted(pages, key=lambda row: row['impressions'], reverse=True)[:limit]:
        clicks, impressions, ctr, position = totals([row])
        lines.append(f"| {safe(row['label'])} | {clicks:g} | {impressions:g} | {ctr} | {position} |")
    if queries and pages:
        lines += ['', f"Exported page clicks minus visible query clicks: {totals(pages)[0] - totals(queries)[0]:g}. This gap can include anonymized or unexported rows; it cannot be classified as brand/nonbrand."]
    return '\n'.join(lines)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--queries', required=True, type=Path)
    parser.add_argument('--pages', required=True, type=Path)
    parser.add_argument('--label', default='Current period (set the date range explicitly)')
    parser.add_argument('--previous-queries', type=Path)
    parser.add_argument('--previous-pages', type=Path)
    parser.add_argument('--previous-label', default='Previous comparable period')
    parser.add_argument('--brand-regex', default=DEFAULT_BRAND, help='case-insensitive regex; default matches xLearning and X Learning variants, not generic learning')
    parser.add_argument('--limit', type=int, default=20)
    parser.add_argument('--output', type=Path, help='optional local Markdown report')
    args = parser.parse_args()
    if bool(args.previous_queries) != bool(args.previous_pages):
        parser.error('provide both --previous-queries and --previous-pages')
    if args.limit < 1:
        parser.error('--limit must be positive')
    try:
        brand = re.compile(args.brand_regex, re.I)
        sections = [report(read_export(args.queries, 'query'), read_export(args.pages, 'page'), brand, args.label, args.limit)]
        if args.previous_queries:
            sections.append(report(read_export(args.previous_queries, 'query'), read_export(args.previous_pages, 'page'), brand, args.previous_label, args.limit))
    except (ValueError, OSError, re.error) as error:
        parser.error(str(error))
    output = '# Search Console export analysis\n\nUse identical search type, property, country/device filters and comparable date ranges. CSV totals are not substitutes for unfiltered GSC chart totals.\n\n' + '\n\n'.join(sections) + '\n'
    if args.output:
        args.output.write_text(output, encoding='utf-8')
    else:
        print(output, end='')

if __name__ == '__main__':
    main()
