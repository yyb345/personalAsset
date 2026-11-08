#!/usr/bin/env python3
"""Read-only raw HTTP SEO checks. No browser, credentials, or third-party dependencies."""
import argparse
from concurrent.futures import ThreadPoolExecutor
from html.parser import HTMLParser
import json
import sys
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit
from urllib.request import Request, urlopen
import xml.etree.ElementTree as ET


class Page(HTMLParser):
    def __init__(self, body):
        super().__init__()
        self.canonical, self.robots, self.h1, self.structured = [], [], 0, []
        self.lang, self.alternates = None, []
        self.in_json = False
        self.json_text = ''
        self.feed(body)

    def handle_starttag(self, tag, attrs):
        a = dict(attrs)
        if tag == 'html': self.lang = a.get('lang')
        if tag == 'link' and a.get('rel') == 'alternate' and a.get('hreflang'):
            self.alternates.append((a['hreflang'], a.get('href')))
        if tag == 'link' and a.get('rel') == 'canonical': self.canonical.append(a.get('href'))
        if tag == 'meta' and a.get('name', '').lower() == 'robots': self.robots.append(a.get('content', ''))
        if tag == 'h1': self.h1 += 1
        if tag == 'script' and a.get('type') == 'application/ld+json':
            self.in_json, self.json_text = True, ''

    def handle_data(self, data):
        if self.in_json: self.json_text += data

    def handle_endtag(self, tag):
        if tag == 'script' and self.in_json:
            self.structured.append(json.loads(self.json_text))
            self.in_json = False


def fetch(url):
    try:
        with urlopen(Request(url, headers={'User-Agent': 'XLearning-SEO-Smoke/1.0'}), timeout=25) as r:
            return r.status, r.headers, r.read().decode('utf-8'), r.url
    except HTTPError as e:
        return e.code, e.headers, e.read().decode('utf-8'), e.url


def run(base, crawl_all=False):
    failures = []
    def check(condition, message):
        print(('PASS ' if condition else 'FAIL ') + message)
        if not condition: failures.append(message)

    status, _, xml, final = fetch(base + '/sitemap.xml')
    check(status == 200 and final == base + '/sitemap.xml', 'sitemap HTTP 200 without redirect')
    root = ET.fromstring(xml)
    urls = [e.text for e in root.findall('{http://www.sitemaps.org/schemas/sitemap/0.9}url/{http://www.sitemaps.org/schemas/sitemap/0.9}loc')]
    check(bool(urls) and len(urls) == len(set(urls)), f'sitemap has {len(urls)} unique URLs')
    check(all(urlsplit(u).netloc == urlsplit(base).netloc and u.startswith(base + '/') for u in urls), 'sitemap uses configured origin')
    status, _, robots, _ = fetch(base + '/robots.txt')
    check(status == 200 and 'Sitemap: ' + base + '/sitemap.xml' in robots, 'robots.txt advertises canonical sitemap')
    check(not any(line.strip().lower() == 'disallow: /' for line in robots.splitlines()), 'robots.txt has no blanket disallow')
    guides = [u for u in urls if '/guides/' in u]
    videos = [u for u in urls if '/video/' in u]
    chinese = [base + '/zh/'] if base + '/zh/' in urls else []
    sample = urls if crawl_all else list(dict.fromkeys([base + '/', base + '/guides', base + '/video'] + chinese + guides[:1] + videos[:2]))
    for url, result in zip(sample, ThreadPoolExecutor(max_workers=4).map(fetch, sample)):
        status, headers, body, final = result
        page = Page(body)
        check(status == 200 and final == url, f'{url} HTTP 200 without redirect')
        check(page.canonical == [url], f'{url} exactly one self canonical')
        directives = headers.get('X-Robots-Tag', '') + ','.join(page.robots)
        check('noindex' not in directives.lower(), f'{url} indexable directives')
        check(page.h1 == 1, f'{url} one raw HTML H1')
        if chinese and url in [base + '/', base + '/zh/']:
            expected = [('en', base + '/'), ('zh-Hans', base + '/zh/'), ('x-default', base + '/')]
            check(sorted(page.alternates) == sorted(expected), f'{url} reciprocal language alternates')
            check(page.lang == ('zh-CN' if url.endswith('/zh/') else 'en'), f'{url} correct initial HTML language')
        if '/guides/' in url or '/video/' in url:
            check(bool(page.structured), f'{url} parseable JSON-LD')
    for path in ['/login', '/dashboard/youtube', '/shadowing', '/lingq-lab', '/transcribe']:
        status, headers, body, _ = fetch(base + path)
        check(status == 200 and 'noindex' in (headers.get('X-Robots-Tag', '') + ','.join(Page(body).robots)).lower(), path + ' workspace noindex')
    for path in ['/video/seo-smoke-nonexistent', '/guides/seo-smoke-nonexistent']:
        status, _, _, _ = fetch(base + path)
        check(status == 404, path + ' real HTTP 404')
    print(f'Checked {len(sample)} public HTML pages; {len(failures)} failures.')
    return bool(failures)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', default='https://www.xlearning.top')
    parser.add_argument('--all', action='store_true', help='Fetch every sitemap URL instead of a small sample')
    args = parser.parse_args()
    try:
        sys.exit(run(args.base_url.rstrip('/'), args.all))
    except (URLError, ValueError, ET.ParseError) as e:
        print('ERROR ' + str(e), file=sys.stderr)
        sys.exit(2)
