# English learning guide pages

The `/guides` hub and ten `/guides/{slug}` articles are rendered by Spring/Thymeleaf, so their content and links are present without JavaScript. Content lives in `src/main/resources/seo/guides.json`. The catalog is also the source for sitemap entries. Unknown slugs return HTTP 404 with `noindex, follow`.

## Content inventory

1. `/guides/how-to-get-youtube-transcript` — transcript discovery and caption verification.
2. `/guides/english-shadowing-for-beginners` — a beginner speaking routine.
3. `/guides/active-listening-with-youtube` — a timed listening session.
4. `/guides/learn-english-with-subtitles` — choosing and removing captions.
5. `/guides/english-dictation-practice` — dictation and error classification.
6. `/guides/build-vocabulary-from-videos` — phrase selection and retrieval.
7. `/guides/choose-english-learning-videos` — matching clips to learner needs.
8. `/guides/take-notes-from-english-videos` — timestamped study notes.
9. `/guides/shadowing-vs-listen-and-repeat` — choosing between two exercises.
10. `/guides/seven-day-english-listening-plan` — a flexible practice schedule.

Every article has unique metadata, a canonical URL, Article and BreadcrumbList structured data, visible navigation, a practical exercise, and related guide links. The home page footer and transcript library link to the hub. English content follows the existing public transcript pages; this is a topic selection decision, not a claim about measured keyword volume.

## Validation and release

Run `mvn -Dtest=GuideControllerTest test` with JDK 17 and `npm run build` in `frontend`. The tests render every article through Thymeleaf and validate metadata, body content, JSON-LD, hub links, 404 status and sitemap XML.

Deploy both backend resources and the frontend build using the existing release process. Verify `/guides`, an article URL, an unknown guide URL, `/css/guides.css` and `/sitemap.xml` on production. Use Search Console to inspect representative URLs and submit the sitemap if needed. Indexing and ranking are not guaranteed. No production deployment or Search Console submission is performed by this change.

Editorial approach: actionable, distinct content rather than keyword-swapped pages, following https://developers.google.com/search/docs/fundamentals/creating-helpful-content . Recheck platform UI instructions when updating the transcript guide. Do not add invented author credentials, performance claims or publication dates.
