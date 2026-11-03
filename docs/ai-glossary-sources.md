# AI / LLM 双语术语词库

## 数据与来源

采集日期：2026-11-03。`src/main/resources/glossary/terms.json` 包含 **795 个独立概念**（不是把中英文、缩写分别计数），覆盖 LLM、生成式 AI、智能体、Transformer、训练、评估、检索、机器学习基础、负责任 AI、视觉、语音及模型部署基础设施。每项均包含英文名称、中文名称、英文解释、中文解释、匹配别名、分类和原始来源 URL。

实际通过 HTTPS 抓取以下 Google 官方公开页面，而非生成没有出处的解释：

- [Google Machine Learning Glossary](https://developers.google.com/machine-learning/glossary)，[简体中文](https://developers.google.com/machine-learning/glossary?hl=zh-cn)。大部分基础和 LLM 条目的来源。
- [Google Cloud / Vertex AI Glossary](https://cloud.google.com/vertex-ai/docs/glossary)，[简体中文](https://cloud.google.com/vertex-ai/docs/glossary?hl=zh-cn)。补充嵌入检索、模型评估、流水线、特征管理和部署术语。

Google 页面说明其文档内容除另有说明外使用 [Creative Commons Attribution 4.0](https://creativecommons.org/licenses/by/4.0/)；代码示例的许可与文档内容不同。数据中的解释是来源定义的短摘录，移除 HTML、图片、公式渲染标签，适当截短；中文大部分沿用官方中文翻译。每个概念通过 `sources` 保留双语页面及定位锚点。本项目修改了清洗、截短、术语合并及少量常见中文名称（例如 token → 词元、attention → 注意力机制），这些修改不表示 Google 对本项目的背书。发布本词库时应保留本来源和许可说明。

## 可复现采集

```bash
python3 -m pip install beautifulsoup4
python3 scripts/glossary/collect_glossary.py
```

采集器默认缓存原始网页到 `/tmp/ai-glossary-cache`。使用 `--cache-dir PATH` 可指定其他位置。删除缓存后可重新下载；源站文档的后续更新会影响数量和内容，重新采集后应审阅 diff。运行需要互联网访问。

处理流程：

1. 分别下载英文和简体中文 HTML，按页面的稳定锚点配对。
2. 提取定义段落和必要列表，删除 HTML、重复空白和字体私用区字符；保留最多 850 个英文字符或 450 个中文字符，以便悬浮卡片展示。
3. 将只写“Abbreviation / Acronym / See / Synonym”的重定向条目合并进真实概念的 aliases；别名不增加概念数量。
4. 对跨来源英文概念做大小写、标点归一去重，合并别名和来源。
5. 为有括号缩写的名称提取简称，并保留中英文名称。补入常用中文的“词元 / 令牌”“注意力 / 注意力机制”。
6. 写入 UTF-8 JSON；采集时检查至少 500 条、ID 唯一。

## 验证与使用边界

本次验证：795 个 ID 唯一；795 项都有非空中英文解释；所有中文解释均包含汉字；规范化英文名称无重复；包含 token / 词元 / 令牌、LLM / 大语言模型、RAG / 检索增强生成、Transformer、attention / 注意力机制等关键词。

这是官方词汇表的广覆盖种子库，不是对所有最新 LLM 论文和厂商产品的完整枚举。没有为了凑足 1000 而给同一概念重复编号。部分专有名称在中文官方页仍用英文，解释为中文。原文的图片或数学公式不作为 HTML 注入字幕，少量高度依赖公式的定义在纯文本形式下会损失排版；匹配与解释应使用安全的文本节点渲染。
