# SEO 测量与复盘

## 已知基线：2026-08-07 至 2026-11-06

本次任务已获得的 Search Console 基线为 **77 次点击、2,170 次展示、CTR 3.6%、平均排名 5.1**；页面数据中的 77 次点击均来自首页。可见查询中，3 个品牌词贡献 58 次点击，查询导出只显示 24 行。品牌点击占总点击至少约 75%，但**不能把剩余 19 次直接认定为非品牌点击**：匿名查询和导出覆盖范围会造成查询行汇总小于图表总数。此基线不代表代码改动后的效果；本次未读取 GA 报表。

根据后续明确的用户请求，首页新增 `/zh/` 独立中文预渲染入口。中文增长效果尚未验证，应与英文 `/` 分开观察；是否扩展中文指南和视频 URL，依据后续查询词及页面维度数据决定。

## 每次重复执行的非品牌分析

在 GSC 使用同一资源、搜索类型、国家/设备筛选条件，导出两个完整 CSV：查询（Queries）和网页（Pages）。保留导出日期范围和筛选条件；比较等长时间段，记录季节性和发布时间。脚本离线运行，不上传查询或 URL。

```bash
python3 scripts/analyze-search-console.py \
  --queries /absolute/path/current/Queries.csv \
  --pages /absolute/path/current/Pages.csv \
  --label '2026-11-08 至 2026-12-05，Web，全部国家和设备' \
  --previous-queries /absolute/path/previous/Queries.csv \
  --previous-pages /absolute/path/previous/Pages.csv \
  --previous-label '对应的上一等长周期' \
  --output /absolute/path/seo-report.md
```

支持 GSC 英文与常见中文列名。默认品牌表达式只匹配 `xLearning`、`X Learning`、`x-learning`、`xlearning.top` 等变体，**不把单独的 learning / English learning 算作品牌**。使用 `--brand-regex` 调整并在每期报告保留同一口径。输出展示品牌/非品牌可见查询、网页及按展示量排序的机会词；CTR 由点击/展示重算，排名按展示量加权。导出行不足、匿名查询与筛选差异不能靠脚本还原，不将查询缺口归类。

推荐人工检查三类变化：非品牌词有没有获得展示；首页以外的指南与视频页是否获得点击；展示增加却 CTR 很低的页面，其标题是否准确回应查询。小样本下先记录，不设未经验证的增长承诺。

## 学习转化埋点

同一个 `/js/learning-analytics.js` 由首页、指南、视频公开页和 Vue 工作台加载。Vue router 只通知这个共享脚本，不再反复发送 GA `config`。脚本保留现有 GA ID `G-6MKRR655RZ`，默认配置 `send_page_view: false`，自行发送规范化页面事件。生产域名之外不加载 Google 标签，仍可在本地 `window.dataLayer` 检查事件。

| 事件 | 触发时机 | 用途 |
| --- | --- | --- |
| `page_view` | 初始文档及成功的 Vue 页面导航；同一路径的初始化去重 | 页面访问 |
| `seo_landing` | 当前 tab 的 30 分钟活动归因上下文中，第一次访问公开页 | 公开内容入口；并不自动等同搜索访问 |
| `start_learning` | 点击标记的公开页/首页学习入口 | 表达进入视频或阅读学习的意图；不代表材料加载成功 |
| `material_opened` | 视频字幕成功加载且非空、工作台播放器就绪，或打开阅读材料 | 区分只点入口与实际打开材料；不代表完成学习 |

共享属性 `landing_page`、`entry_source` 将公开入口关联到后续事件。`entry_source` 取固定值 `search_referrer`、`external_referrer`、`direct_or_unknown`、`internal`；来源仅根据 referrer 主机分类；GA 的 page_referrer 仅保留合法 HTTP(S) 来源的 origin，不含用户名、路径、查询或 hash，帮助保留原生来源识别。`search_referrer` 是浏览器观察到搜索来源的近似分组，不能区分所有自然/付费流量，也无法覆盖被浏览器隐藏的来源。它不是 GSC 点击数的逐用户匹配结果。

允许逐页保留：英文 `/` 和中文 `/zh/` 首页、固定列表页、10 个已知公开指南路径和格式为 11 位 YouTube ID 的公开视频 URL；查询参数、hash、私有工作台材料 ID 与未知路径均不会进入自定义参数。阅读材料不发送 ID、标题或导入地址。笔记、字幕、搜索文本、邮箱及其他输入也不作为事件参数。视频 ID 只在已经公开的 `/video/{videoId}` 页面路径中保留；私有路径统一为 `/dashboard/study-workspace/:videoId`。

归因暂存只包含已校验的公开入口、固定来源分类和到期时间，使用 `sessionStorage`，30 分钟无活动后失效；禁用存储时降级为当前文档内存。它是 tab 级上下文，不是 GA 用户身份或跨设备归因。

## GA 配置与上线验收

代码不能读取或修改 GA 资源的管理设置。上线后由资源管理员检查：

1. **关闭 Enhanced Measurement 的页面历史变化自动 page_view**，避免它与手动 Vue 页面事件重复。为了保持这里定义的最小采集范围，关闭自动表单、站内搜索和其他不需要的增强型衡量事件；外部标签管理器中也不应另设同一 ID 的初始化或历史监听。代码只对自己的手动事件去重，不能保证远端自动配置不会另外采集。
2. 在 GA 注册事件范围自定义维度 `landing_page`、`entry_source`、`learning_type`、`material_type`、`source_page`，才便于在探索报表中比较。
3. 在测试访问中用浏览器 dataLayer 和 GA DebugView/实时报告核验：公开页进入一次、点击一次、材料成功打开一次；失败或空字幕不计打开；查询参数和私有内容不出现在参数中。这里未声称已完成线上 GA 验收。
4. 建立按 `landing_page` 分组的事件序列探索：`seo_landing` → `start_learning` → `material_opened`，搜索近似入口使用 `entry_source=search_referrer`。统计完成序列的会话或用户，**不要直接用事件次数相除当作会话转化率**：一个人可多次点击、打开材料，也可能直接打开材料而跳过 CTA。

GSC 与 GA 采用不同计量方法、时区、隐私处理和来源判断，放在同一周报中观察趋势，不进行用户级拼接或强行要求点击数相等。

## 每期复盘模板

- 日期范围、对比周期、GSC 资源及筛选条件：
- 图表总点击/展示/CTR/平均排名（单独抄录，不由查询行推算）：
- 导出查询行数、可见品牌与非品牌点击、未覆盖差额及口径变更：
- 首页以外获得展示/点击的页面及对应的非品牌查询机会：
- 按公开入口分组的搜索来源近似学习漏斗（使用同一 GA 序列口径）：
- 资料是否成功打开、异常是否来自加载失败、统计配置是否仍有效：
- 本期结论、下一次要验证的单一内容或入口调整：

运行本地验证：

```bash
node --test frontend/tests/learning-analytics.test.mjs
python3 -m unittest discover -s scripts/tests -p 'test_search_analysis.py'
```
