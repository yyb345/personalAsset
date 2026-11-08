# Curated video study notes

The five editorial entries in `src/main/resources/seo/video-lessons.json` were checked against the publicly served transcript HTML on **2026-11-08 (Asia/Singapore)**. This date records the editorial review, not the video's upload or publication date. The notes paraphrase the opening explanations and provide original study exercises; they do not assert current model capabilities, hiring policies, CEFR levels, or independently measured statistics.

## Source evidence

| Public transcript source | Observed caption start seconds and short expression | Editorial focus |
| --- | --- | --- |
| [IBM Technology: model comparison](https://www.xlearning.top/video/AVQzG2MY858) | 23.3 umbrella term; 61.4 parameters; 83.5 generalists; 135.9 specialists | Definitions and comparisons; task-specific model selection in the speaker's framing |
| [IBM Technology: agent skills](https://www.xlearning.top/video/Lg-meK5IU8Q) | 36.9 procedural knowledge; 155.1 trigger condition; 274.6 progressive disclosure | Skill metadata, instructions, and loading information when needed |
| [Andrej Karpathy: Software Is Changing](https://www.xlearning.top/video/LCEmiRjPEtQ) | 102.3 software 1.0; 106.8 neural networks; 199.6 prompts | Speaker's three-way comparison of explicit code, weights, and prompts |
| [Joma Tech: All My Job Offers In Tech](https://www.xlearning.top/video/rtuKId76DZI) | 60.6 internships; 83.7 interview; 89.5 ranked | Distinguishing personal career narrative from general hiring advice |
| [Joma Tech: why you NEED math for programming](https://www.xlearning.top/video/sW9npZVpiMI) | 64.8 figured it out; 66.6 projection equation; 118.2 render; 174.5 computer graphics | Understanding the terminal-rendering scene and the speaker's subsequent argument |

Source titles and timestamped captions were downloaded read-only from the public `/video` hub and the linked detail pages. All 17 timestamp/expression pairs were programmatically compared with those HTML captions. The local SQLite database was opened read-only but contained only two older vlogs, so it was not used to infer production video content.

Caption timestamps are retained as decimal seconds in the data. Visible `mm:ss` labels round down to the whole second; original YouTube links seek to that second. Both Joma pages and the Karpathy page contain overlapping/repeated automatic captions; warnings in the notes ask readers to verify wording against the original audio. The transcript itself is preserved.

## Rendering and maintenance

- New notes appear only on their exact video IDs, after the existing public-transcript access check.
- Each expression must still occur in an actual stored caption within two seconds of its reviewed start time. Missing or changed evidence hides that lesson rather than publishing mismatched instructions.
- Other public videos continue rendering their original transcripts without generated teaching claims.
- The hub's full public video grid remains intact; curated entries and related video links are filtered through the existing public-video catalog.
- Related guides resolve through the guide catalog; missing guide slugs are omitted.
- Chinese editorial sections carry `lang="zh-CN"`, with original English expressions marked `lang="en"`.

When editing a lesson, revisit the original audio and current transcript, update the term timing only from observed captions, review the exercises and answer checks, then update `reviewedOn`. Add a new entry only after the same source check; do not populate it from a title alone.
