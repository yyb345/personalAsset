# Production SEO audit — 2026-11-08

Production origin: https://www.xlearning.top. Observed at approximately 19:55–20:00 Asia/Singapore (UTC+8), using unauthenticated raw HTTP GET requests. These observations describe the deployed site at that time, not all changes in the current working tree. No deployment was performed by this audit.

## Live evidence

| Endpoint | Actual result |
| --- | --- |
| `/` | HTTP 200; rendered HTML contains one H1, navigation/content, `/guides` and `/video` links; app root has `data-prerendered="true"`; canonical `https://www.xlearning.top/` |
| `/robots.txt` | HTTP 200, text/plain; `User-agent: *`, `Allow: /`; advertises canonical sitemap |
| `/sitemap.xml` | HTTP 200, application/xml; valid XML with 92 unique canonical URLs: homepage, video hub, guide hub, 10 guides, 79 video details |
| `/guides` | HTTP 200; self canonical; H1 and guide links present in raw HTML |
| `/guides/how-to-get-youtube-transcript` | HTTP 200; unique title, self canonical, raw article body, parseable Article/breadcrumb JSON-LD |
| `/video` | HTTP 200; self canonical; video cards and H1 present in raw HTML |
| `/video/rtuKId76DZI` | HTTP 200; title “All My Job Offers In Tech”; self canonical, transcript present, parseable Article JSON-LD, `index, follow` |
| `/video/sW9npZVpiMI` | HTTP 200; title “why you NEED math for programming”; self canonical, transcript present, parseable Article JSON-LD, `index, follow` |
| `/login`, `/dashboard/youtube` | HTTP 200; `X-Robots-Tag: noindex, follow` on actual response |
| Nonexistent video and guide | Real HTTP 404 with `noindex, follow` meta; not a soft 404 |
| `http://www.xlearning.top/` | HTTP 301, Location `https://www.xlearning.top/` |
| `https://xlearning.top/` | HTTP 301, Location `https://www.xlearning.top/` |

The baseline SEO changes are therefore already visible in production: the homepage is prerendered, private workspaces receive noindex, and video structured data describes an Article rather than claiming unsupported VideoObject metadata. The current online homepage still uses its previously deployed English copy; local content improvements require a later deployment.

The private SPA shell currently contains the homepage canonical. Its response-level noindex correctly excludes those URLs. Do not add robots disallow rules for these pages: crawlers need to fetch them to see noindex.

The repeatable full-sitemap smoke run completed on November 8: **92 public HTML pages checked; zero failures**. Every listed URL returned HTTP 200 without a redirect, one self canonical, one raw H1, and no noindex directive; guide/video detail JSON-LD parsed successfully. Five SPA routes (`/login`, `/dashboard/youtube`, `/shadowing`, `/lingq-lab`, `/transcribe`) returned noindex; both synthetic missing detail routes returned HTTP 404. The script validates syntax and crawl signals, not Google rich-result eligibility, content quality, or rendering after JavaScript hydration.

## Search Console status: separate from HTTP evidence

Earlier supplied Search Console reports showed:

- Sitemap last read June 18, with 73 discovered URLs.
- October 23 indexing snapshot: six indexed; `/video`, `/video/rtuKId76DZI`, and `/video/sW9npZVpiMI` crawled but not indexed.
- August 7–November 6 performance: 77 clicks, all attributed to homepage.

Those historical reports are not a fresh crawl of today's 92-URL sitemap. The current live HTTP responses above confirm accessibility, not Google indexing or ranking.

On November 8 the root task successfully resubmitted `sitemap.xml` in Search Console. The UI confirmed “已成功提交站点地图” and the submitted date became November 8; last read still showed June 18 while processing was pending. Evidence screenshot: `/tmp/xlearning-sitemap-submitted.png` (temporary session file). This is submission success only; it does not establish a completed recrawl, updated discovery count, or indexing.

The November 8 URL Inspection for the homepage reported it as indexed. Its last crawl was November 6 at 14:07:43, Googlebot mobile; crawling succeeded and was allowed. Both declared and Google-selected canonical were `https://www.xlearning.top/`. These are Search Console inspection facts, distinct from this audit’s current raw GET checks.

The root task then requested homepage indexing on November 8. Search Console confirmed “已请求编入索引” (priority crawl queue); evidence `/tmp/xlearning-home-index-request.png` (temporary session file). This confirms queue submission, not a new completed crawl; the displayed last indexed crawl remained November 6.

## Repeatable verification

Run a small read-only public sample before and after deploying:

```bash
python3 scripts/check-seo-live.py
```

To check every public URL listed in the sitemap (four concurrent requests maximum):

```bash
python3 scripts/check-seo-live.py --all
```

To check another deployment:

```bash
python3 scripts/check-seo-live.py --base-url https://your-canonical-host.example
```

The script uses only Python standard libraries. It checks sitemap XML/origin/duplicates, robots sitemap discovery, self canonicals, raw H1s, index directives, parseable detail JSON-LD, private-route noindex, and real missing-page 404s. It exits 1 for failed assertions and 2 for transport/parse errors. It does not execute JavaScript, authenticate, change production data, or submit URLs. Host/protocol redirect checks were performed separately in this audit; redirects are followed by the smoke checker and rejected for canonical sitemap URLs.

After deployment, rerun this check, then compare Search Console's sitemap last-read date and indexing report over time. Submission and technically correct pages do not guarantee indexing; evaluate content improvements against later query impressions and page-level indexing results.

## Search Console actions completed in this session

- Homepage: Google confirmed the November 8 indexing request and added it to the priority crawl queue. Evidence: `/tmp/xlearning-home-index-request.png`.
- `/guides`: URL Inspection reported “Discovered — currently not indexed”, no previous crawl timestamp. Google then confirmed the November 8 indexing request and added it to the priority crawl queue. Evidence: `/tmp/xlearning-guides-index-request.png`.
- These confirmations are queue receipts, not promises of indexing or refreshed report data. New video lesson content and analytics in this branch have not been deployed; request another inspection of those changed URLs only after deployment and the public smoke checks.
