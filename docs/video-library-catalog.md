# 视频分类与专题维护

来源：2026-10-29 线上公开视频库，共 84 个视频。按真实标题与频道整理，未逐条观看；难度未确认时保留待标注。

## 维护方式

- `frontend/src/features/video-library/production-editorial.js`：按 YouTube ID 保存人工分类、专题顺序及部分学习目标。
- `frontend/src/features/video-library/catalog.js`：新增视频的自动分类、筛选和人工配置合并。人工配置优先；视频时长不用于推断难度。
- 难度沿用已有 `difficultyLevel`（easy/medium/hard），其余为待标注。人工复核后可在对应配置添加 `difficulty`。
- 精选路线按显式 ID 顺序展示，只纳入 completed 视频；不会自动塞入不相关内容。
- 新目录接口 `/api/youtube/catalog` 供全库筛选，前端分页。数据规模增大时应迁移为服务端筛选与分页。
- 游客预览资格在筛选前按原始目录赋予，分类、排序与分页不重新分配资格。现有页面限制并非后端访问控制。

## 精选路线

### foundations

1. LLM vs. SLM vs. FM: Choosing the Right AI Model — `AVQzG2MY858`
2. Andrej Karpathy: Software Is Changing (Again) — `LCEmiRjPEtQ`
3. Post-Training Is How You Keep Your Taste | Fireworks CEO Lin Qiao — `yAvJ7b_FxUA`
4. Stanford CS25: Transformers United V6 I From Representation Learning to World Modeling — `GBd7iuJkW08`
5. Stanford CS336 Language Modeling from Scratch | Spring 2026 | Lecture 4: Attention Alternatives — `cKSwj_qZ8Jg`

### building

1. What AI Agent Skills Are and How They Work — `Lg-meK5IU8Q`
2. RL Environments Explained: How AI Agents Learn Real-World Work | Brendan Foody, Mercor — `a00xIn5kwhM`
3. Factory’s Matan Grinberg and Eno Reyes Unleash the Droids on Software Development | Training Data — `BDgGDCQX3G4`
4. YC's Head of Design Shows You How To Design With AI — `VbqaL_eHhKY`
5. How Anthropic’s product team moves faster than anyone else | Cat Wu (Head of Product, Claude Code) — `PplmzlgE0kg`

### english

1. Energy Management Tips that I Learnt From Professors — `Wxad6IONBvE`
2. How to build self-discipline | small habits big changes — `WH-PXI6rbUw`
3. day in the life at big tech | managing stress at work — `ATz5oDYMDMY`
4. All My Job Offers In Tech — `rtuKId76DZI`
5. Interview with a Software Engineer from Stanford — `_p64_S4bbjU`

## AI 基础与行业观点（7）

| YouTube ID | 标题 | 状态 |
|---|---|---|
| EsaUQNx59vA | 专访前FAIR研究总监田渊栋：Meta裁员之后，对AI的一些遗憾与思考【对话】 | completed |
| sJ4VJWycX9M | Alexandr Wang: “This is a Once-in-a-Civilization Opportunity” | completed |
| CxXgV54KzpQ | Jeff Dean: The 1% Rule for Building in AI | completed |
| Mklj3Y2-fNg | Sam Altman's Vision For the Future! | completed |
| LCEmiRjPEtQ | Andrej Karpathy: Software Is Changing (Again) | completed |
| cWSTqbt5dyg | How Robots Can Backflip with RL (Sim-to-Real, Kinematic Retargetting, Isaac Lab vs Mujoco) | completed |
| 4E27qlfYw0A | Stanford CS230 ／ Autumn 2025 ／ Lecture 5: Deep Reinforcement Learning | completed |

## 大模型（6）

| YouTube ID | 标题 | 状态 |
|---|---|---|
| u8MPjmuCkEQ | 硅谷坐标 x Fireworks 联创Benny Chen：开源模型、token增速、推理优化和模型定制 | failed |
| AVQzG2MY858 | LLM vs. SLM vs. FM: Choosing the Right AI Model | completed |
| yWTVEG9UIHo | 江鋆晨: KV Cache，大模型记忆，清华姚班，CMU，教授，开源，视频流媒体 | failed |
| yAvJ7b_FxUA | Post-Training Is How You Keep Your Taste ／ Fireworks CEO Lin Qiao | completed |
| GBd7iuJkW08 | Stanford CS25: Transformers United V6 I From Representation Learning to World Modeling | completed |
| cKSwj_qZ8Jg | Stanford CS336 Language Modeling from Scratch ／ Spring 2026 ／ Lecture 4: Attention Alternatives | completed |

## Agent 智能体（4）

| YouTube ID | 标题 | 状态 |
|---|---|---|
| UqMtkgQe-kI | Manus' Final Interview Before the Acquisition: Oh, the Surreal Odyssey of 2025… | failed |
| BDgGDCQX3G4 | Factory’s Matan Grinberg and Eno Reyes Unleash the Droids on Software Development ／ Training Data | completed |
| a00xIn5kwhM | RL Environments Explained: How AI Agents Learn Real-World Work ／ Brendan Foody, Mercor | completed |
| Lg-meK5IU8Q | What AI Agent Skills Are and How They Work | completed |

## AI 应用与产品（8）

| YouTube ID | 标题 | 状态 |
|---|---|---|
| -MD9NsVj2xo | Stanford AI Club: Alfred Lin on Building in the AI Era | completed |
| WriCcrOP8s4 | 复旦讲座：品牌、AI与真实商业世界的底层逻辑 | failed |
| n3XmK7rKPvs | AI时代，专业人士如何越活越值钱？——对话《令人心动的offer》带教律师史欣悦｜How to Become More Valuable in the Age of AI | failed |
| VbqaL_eHhKY | YC's Head of Design Shows You How To Design With AI | completed |
| eh8bcBIAAFo | The design process is dead. Here’s what’s replacing it. ／ Jenny Wen (head of design at Claude) | completed |
| PplmzlgE0kg | How Anthropic’s product team moves faster than anyone else ／ Cat Wu (Head of Product, Claude Code) | completed |
| s5r4wdOWLjk | AI Instead of a Degree: How to Build a $1B Company | completed |
| U7PcyE0p54s | $20B AI CEO: The ONLY trait for success in the AI era ／ Aravind Srinivas, Co-founder of Perplexity | completed |

## 英语听力与职场表达（47）

| YouTube ID | 标题 | 状态 |
|---|---|---|
| _p64_S4bbjU | Interview with a Software Engineer from Stanford | completed |
| GkrwjFvnW3E | 'Be your own fish': Eileen Gu speaks at Singapore Management University women leadership event | completed |
| ZVP8cmlHwgU | 开车英语 ／ 零基础英语 ／ driving a car ／ 从零开始学英文 ／ 生活英语口语 ／ 基础英语词汇 | failed |
| L_WK9XUwhXk | it's hard to make new friends when I live such a quiet and private life | completed |
| 0-O_t_SstEs | Being Forced to Confront Your Past ／ Suits | completed |
| cH9auq8TVXs | A Double Wedding Feature ／ Suits | completed |
| VOqb01Bln_s | when you feel uncertain about what's next ／ life in twenties | completed |
| 3C3wOhIuTgY | UC Berkeley Grad 10 Years Later. What I Wish I Knew | completed |
| Y3vulWfEpEE | My Wharton MBA is Useless in This Market | completed |
| cnG5plbTssA | turns out, it wasn't a dream job | completed |
| kQ6WIAb8Wsc | i quit my job without a backup plan: 6 month update | completed |
| ZyEMuN8NYw8 | big tech employee vs startup founder ep.4 ／ NYC meetup, self-doubt, performance reviews | completed |
| 52d1ec7B_gU | i quit my job with no backup plan: how my life is changing | completed |
| ATz5oDYMDMY | day in the life at big tech ／ managing stress at work | completed |
| Ja2qxjUnw9Q | living alone and finding comfort in solitude ／ PhD diaries | completed |
| WH-PXI6rbUw | How to build self-discipline ／ small habits big changes | completed |
| awSZylgjIlc | 💫 Becoming a triathlete with zero experience ／ my race schedule, running, limiting beliefs | completed |
| _AR345TtYGE | big tech employee vs startup founder ep.3 ／ reorgs, first-time founder woes, tough feedback | completed |
| -r8FU-gV8Ss | big tech vs startups: getting promoted | completed |
| KhwO6gQtyAE | Jennifer Lawrence talks new film 'No Hard Feelings' l GMA | completed |
| tv2nyeQdCl8 | Dakota Johnson Talks Billie Eilish Prank and Pedro Pascal Stealing Her Clothes, Plays Fast Forward | completed |
| c58UpZLN4Ug | Mike and Harvey's Epic Bromance ／ Suits | completed |
| 8OzUGQLR7ys | 🎓My First Week at Columbia Business School ／ MBA Orientation + Main Campus Tour | completed |
| xpyrefzvTpI | Leonardo DiCaprio winning Best Actor ／ 88th Oscars (2016) | completed |
| VVqGRv5fnSQ | HARVARD FIRST DAY OF SCHOOL ／ Sophomore Year | completed |
| WLy9Tab_8XQ | Finding A Job After Post Grad: from two perspectives working in tech and marketing | completed |
| RdZ_shAlGsI | big tech vs startups: a week in the life | completed |
| kBz51Ldxw9M | what we would tell our younger selves ／ turning late 20s | completed |
| 5tJa81SIXOQ | Reflections on Working Four Years at Notion | completed |
| jF2TJVCRdFc | What I wish I knew before working at a startup | completed |
| XxyNyyoj_Kg | What no one tells you about working at a startup | completed |
| 05TLyn5vgEA | Going all in or starting over? Startups vs. big tech | completed |
| 6icayqBzeBg | A day in my life as a PhD candidate ／ time management, living alone chic chat | completed |
| 85yYJByDPtk | Living Alone in Melbourne🏡 My 1B1B Apartment Tour | completed |
| Wses7f_czfE | Week in my life as a university tutor📚 moving, challenges of teaching | completed |
| Wxad6IONBvE | Energy Management Tips that I Learnt From Professors | completed |
| Li4RdAQx11g | apartment hunting in melbourne🏠 one bedroom tours & rent prices | completed |
| w5JfEBFIvtA | From PhD to Marriage to Moving Abroad ／ My Life Reset at 30 | completed |
| -ZVOwqJpuBY | When Georgie Met Mandy: Part 1 ／ Young Sheldon Compilation | completed |
| CL5WS5pao7Y | Rachel Zane Plays Hard to Get ／ SEASON 1 ／ Suits | completed |
| nLRm8348sPY | Most Watched Scenes from Season 3 ／ Suits | completed |
| FD12D9kySCg | Why I don’t post on Joma Tech… But New Channel! | completed |
| AGovqGO2t0M | day in the life of a software engineer ／ 6+ hours productivity ／ coding my social networking app ☕️ | completed |
| rtuKId76DZI | All My Job Offers In Tech | completed |
| Fp_o3jJrk9I | 1 week as an investment banker in NYC | completed |
| uxRf7KS3abo | i finally got a software engineering job | completed |
| O0WFna-1gkM | Laid Off After 25 Years in Tech: How a 50% Savings Rate Made It Stress-Free | completed |

## 创业与商业（5）

| YouTube ID | 标题 | 状态 |
|---|---|---|
| Za6c0NcAmOk | 马云早期演讲：讲述创业过程 分享管理之道 | completed |
| abtHadERzXU | Live Office Hours with Yuri Sagalov and Sam Altman - Stanford CS183F: Startup School | completed |
| URiIsrdplbo | How to Get Users and Grow - Alex Schultz, VP of Growth at Facebook - Stanford CS183F: Startup School | completed |
| I79cK9iV-vQ | 【硅谷帮 第三季】EP09 用创业赢回自己人生 Use Entrepreneurship to Win Back Your Own Life｜Zoil Li/ Echo Zhong/ Carly Wu | completed |
| DGU0wXMKqf4 | Sam Altman explains how to come up with a great startup idea | completed |

## 编程与计算机（5）

| YouTube ID | 标题 | 状态 |
|---|---|---|
| c-MnSFGTSN8 | 孟子立: 15岁，清华，HKUST，教授，家庭计算中心，云游戏，WiCi，网络系统，创业 | completed |
| jYUZAF3ePFE | Linus Torvalds Guided Tour of His Home Office | completed |
| rkZzg7Vowao | The Man Who Revolutionized Computer Science With Math | completed |
| sW9npZVpiMI | why you NEED math for programming | completed |
| 0KmUoTfGa34 | Coding a whole web app ONLY using my Phone. | completed |

## 日语练习（2）

| YouTube ID | 标题 | 状态 |
|---|---|---|
| pXh1Mc5LonQ | 【N5-N3】Buying a new Macbook Pro at ヨドバシカメラ ／ Japanese immersion practice | completed |
| MmvaYPjogwQ | 1 Hour Real Japanese Conversation ／ About Life, Friends, and Travel #72 | completed |

