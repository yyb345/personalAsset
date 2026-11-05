# Agent、基础模型与推理词库第二轮补充

整理日期：2026-11-03。本轮核对114个概念，新增105个、合并更新9个。词库从892增至 **997个独立概念、2672个名称/别名**。新增词与上一轮后训练词库一起保存在 `src/test/resources/glossary/advanced_terms.json`，总计216个专项核对记录；其中202个为新增概念，14个合入最初795个基础词条。

## 专项范围

| 领域 | 本轮核对 | 新增 | 内容示例 |
| --- | --- | --- | --- |
| Agent | 34 | 30 | agent harness、规划与执行、工具模式、Agent Skills、A2A、Agent Card、MCP原语、执行轨迹与评测 |
| 基础模型 | 50 | 47 | 编解码架构、因果语言建模、缩放定律、计算最优训练、MoE专家、RMSNorm、SwiGLU、VLM、分词和扩散语言模型 |
| 推理方法 | 26 | 24 | CoT、MCTS、程序辅助推理、验证器引导搜索、自适应计算、过度思考、解码方法与采样通过率 |
| 推理部署 | 4 | 4 | 张量并行、流水线并行、专家并行、预填充解码分离 |

9个合并更新条目沿用稳定ID：agent、sub-agent、task decomposition、plan-and-solve、masked language model、单向/因果语言模型、tokenizer、CoT和pass@k，不把同义名称另算作概念。

## 解释与匹配原则

英文解释是依据官方文档及原始论文撰写的简短概括，中文为整理翻译，不复制论文全文或声称相关机构背书。每个条目均保留HTTPS来源。保留各来源自身许可；不是对所有新增内容统一套用Google文档许可。

- 推理方法与推理部署分别解释；MCTS、CoT、beam search不是同一个概念。
- pass@k表示k个候选中至少一次成功，pass^k表示多次尝试均成功；合并归一化保留@、^、+、#，防止不同指标或语言名称被合并。
- 明确区分MCP工具、资源与提示模板，以及用于智能体间协作的A2A。
- 不把更长思考、更多智能体或更多候选等同于必然提高正确率。
- 常见复数和拼写变体作为别名处理，英文缩写仍使用完整词边界，防止CoT命中COTTON等普通单词。

## 可复现验证

```bash
python3 scripts/glossary/merge_advanced.py
python3 -m unittest discover -s scripts/glossary -p '*test.py'
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn test
# 使用现有网页缓存重建基础词库时也会自动合入两轮专项数据
python3 scripts/glossary/collect_glossary.py
```

运行时仍从统一的 `src/main/resources/glossary/terms.json` 读取，无需更改接口。内容哈希变化后，部署的新应用会在首次高亮请求自动创建并写入新的percolator索引。

## 本轮验证结果

- 本轮合并前，原词库所有概念均保留；专项数据累计216个概念，新增英文简称、中文和拼写变体均经过完整词匹配检查。
- Java测试遍历两轮专项的全部名称/别名，验证UTF-16位置和唯一概念归属；增加Agent、基础模型、推理混合文本及pass@k/pass^k区分测试。
- 108句线上字幕快照中70句命中，共178处、38个概念；较上一轮增加了agents与scaling law等实际表面形式覆盖。
- 5项Python合并测试及后端测试通过。重跑缓存基础采集后与合并结果逐字节一致。
- 本轮未启动新的真实ES实例，数据仍沿用此前已验证的percolator索引与查询链路；本轮验证重点为词条、合并、精确匹配与字幕回归。

## 本轮来源与条目清单

| 类别 | 英文名称 | 中文名称 | 来源 |
| --- | --- | --- | --- |
| Agents | LLM agent | 语言模型智能体 | [官方文档/论文](https://www.anthropic.com/research/building-effective-agents) |
| Agents | plan-and-execute agent | 规划执行型智能体 | [官方文档/论文](https://www.anthropic.com/research/building-effective-agents) |
| Agents | agent routing | 智能体路由 | [官方文档/论文](https://www.anthropic.com/research/building-effective-agents) |
| Agents | agent harness | 智能体运行框架 | [官方文档/论文](https://www.anthropic.com/research/effective-harnesses-for-long-running-agents) |
| Agents | long-horizon agent | 长程任务智能体 | [官方文档/论文](https://www.anthropic.com/research/effective-harnesses-for-long-running-agents) |
| Agents | agent checkpoint | 智能体执行检查点 | [官方文档/论文](https://www.anthropic.com/research/effective-harnesses-for-long-running-agents) |
| Agents | tool schema | 工具模式定义 | [官方文档/论文](https://www.anthropic.com/research/writing-tools-for-agents) |
| Agents | tool namespacing | 工具命名空间 | [官方文档/论文](https://www.anthropic.com/research/writing-tools-for-agents) |
| Agents | tool selection | 工具选择 | [官方文档/论文](https://www.anthropic.com/research/writing-tools-for-agents) |
| Agents | tool response | 工具返回结果 | [官方文档/论文](https://www.anthropic.com/research/writing-tools-for-agents) |
| Agents | sub-agent | 子智能体 | [官方文档/论文](https://www.anthropic.com/research/multi-agent-research-system) |
| Agents | agent delegation | 智能体任务委派 | [官方文档/论文](https://www.anthropic.com/research/multi-agent-research-system) |
| Agents | agent handoff | 智能体交接 | [官方文档/论文](https://www.anthropic.com/research/multi-agent-research-system) |
| Agents | multi-agent system | 多智能体系统 | [官方文档/论文](https://www.anthropic.com/research/multi-agent-research-system) |
| Agents | Plan-and-Solve prompting | 先规划后求解提示 | [官方文档/论文](https://arxiv.org/abs/2305.04091) |
| Agents | task decomposition | 任务分解 | [官方文档/论文](https://arxiv.org/abs/2305.04091) |
| Agents | parallel tool calling | 并行工具调用 | [官方文档/论文](https://arxiv.org/abs/2312.04511) |
| Agents | LLMCompiler | 大模型调用编译器 | [官方文档/论文](https://arxiv.org/abs/2312.04511) |
| Agents | Agent2Agent protocol | 智能体互联协议 | [官方文档/论文](https://a2a-protocol.org/latest/topics/key-concepts/) |
| Agents | Agent Card | 智能体能力卡 | [官方文档/论文](https://a2a-protocol.org/latest/topics/key-concepts/) |
| Agents | A2A artifact | A2A任务产物 | [官方文档/论文](https://a2a-protocol.org/latest/topics/key-concepts/) |
| Agents | Agent Skills | 智能体技能包 | [官方文档/论文](https://agentskills.io/home) |
| Agents | progressive disclosure for agents | 智能体渐进式信息加载 | [官方文档/论文](https://agentskills.io/home) |
| Agents | MCP tool | MCP工具 | [官方文档/论文](https://modelcontextprotocol.io/specification/2025-06-18/server/tools) |
| Agents | MCP resource | MCP资源 | [官方文档/论文](https://modelcontextprotocol.io/specification/2025-06-18/server/resources) |
| Agents | MCP prompt | MCP提示模板 | [官方文档/论文](https://modelcontextprotocol.io/specification/2025-06-18/server/prompts) |
| Agents | agent trajectory | 智能体执行轨迹 | [官方文档/论文](https://www.anthropic.com/research/demystifying-evals-for-ai-agents) |
| Agents | evaluation harness | 评测运行框架 | [官方文档/论文](https://www.anthropic.com/research/demystifying-evals-for-ai-agents) |
| Agents | outcome grading | 结果状态评分 | [官方文档/论文](https://www.anthropic.com/research/demystifying-evals-for-ai-agents) |
| Agents | trajectory grading | 执行轨迹评分 | [官方文档/论文](https://www.anthropic.com/research/demystifying-evals-for-ai-agents) |
| Agents | SWE-bench | 软件工程修复基准 | [官方文档/论文](https://arxiv.org/abs/2310.06770) |
| Agents | WebArena | 网页任务智能体基准 | [官方文档/论文](https://arxiv.org/abs/2307.13854) |
| Agents | OSWorld | 计算机操作智能体基准 | [官方文档/论文](https://arxiv.org/abs/2404.07972) |
| Agents | computer-use agent | 计算机操作智能体 | [官方文档/论文](https://arxiv.org/abs/2404.07972) |
| Foundation models | encoder-decoder architecture | 编码器解码器架构 | [官方文档/论文](https://arxiv.org/abs/1706.03762) |
| Foundation models | cross-attention | 交叉注意力 | [官方文档/论文](https://arxiv.org/abs/1706.03762) |
| Foundation models | causal attention mask | 因果注意力掩码 | [官方文档/论文](https://arxiv.org/abs/1706.03762) |
| Foundation models | residual connection | 残差连接 | [官方文档/论文](https://arxiv.org/abs/1706.03762) |
| Foundation models | encoder-only model | 纯编码器模型 | [官方文档/论文](https://arxiv.org/abs/1810.04805) |
| Foundation models | masked language model | 掩码语言模型 | [官方文档/论文](https://arxiv.org/abs/1810.04805) |
| Foundation models | decoder-only model | 纯解码器模型 | [官方文档/论文](https://arxiv.org/abs/2407.21783) |
| Foundation models | causal language modeling | 因果语言建模 | [官方文档/论文](https://arxiv.org/abs/2407.21783) |
| Foundation models | next-token prediction | 下一词元预测 | [官方文档/论文](https://arxiv.org/abs/2407.21783) |
| Foundation models | instruction-tuned model | 指令微调模型 | [官方文档/论文](https://arxiv.org/abs/2407.21783) |
| Foundation models | span corruption | 片段破坏预训练 | [官方文档/论文](https://arxiv.org/abs/1910.10683) |
| Foundation models | text-to-text modeling | 文本到文本建模 | [官方文档/论文](https://arxiv.org/abs/1910.10683) |
| Foundation models | neural scaling law | 神经网络缩放定律 | [官方文档/论文](https://arxiv.org/abs/2001.08361) |
| Foundation models | training compute budget | 训练计算预算 | [官方文档/论文](https://arxiv.org/abs/2001.08361) |
| Foundation models | compute-optimal training | 计算最优训练 | [官方文档/论文](https://arxiv.org/abs/2203.15556) |
| Foundation models | Chinchilla scaling | Chinchilla缩放规律 | [官方文档/论文](https://arxiv.org/abs/2203.15556) |
| Foundation models | training token budget | 训练词元预算 | [官方文档/论文](https://arxiv.org/abs/2203.15556) |
| Foundation models | dense model | 稠密模型 | [官方文档/论文](https://arxiv.org/abs/2101.03961) |
| Foundation models | sparse activation | 稀疏激活 | [官方文档/论文](https://arxiv.org/abs/2101.03961) |
| Foundation models | expert routing | 专家路由 | [官方文档/论文](https://arxiv.org/abs/2101.03961) |
| Foundation models | expert capacity | 专家容量 | [官方文档/论文](https://arxiv.org/abs/2101.03961) |
| Foundation models | shared expert | 共享专家 | [官方文档/论文](https://arxiv.org/abs/2401.06066) |
| Foundation models | routed expert | 路由专家 | [官方文档/论文](https://arxiv.org/abs/2401.06066) |
| Foundation models | expert specialization | 专家专门化 | [官方文档/论文](https://arxiv.org/abs/2401.06066) |
| Foundation models | activated parameters | 激活参数量 | [官方文档/论文](https://arxiv.org/abs/2412.19437) |
| Foundation models | auxiliary-loss-free load balancing | 无辅助损失负载均衡 | [官方文档/论文](https://arxiv.org/abs/2412.19437) |
| Foundation models | FP8 training | 八位浮点训练 | [官方文档/论文](https://arxiv.org/abs/2412.19437) |
| Foundation models | multi-token prediction | 多词元预测 | [官方文档/论文](https://arxiv.org/abs/2404.19737) |
| Foundation models | root mean square normalization | 均方根归一化 | [官方文档/论文](https://arxiv.org/abs/1910.07467) |
| Foundation models | SwiGLU | Swish门控线性单元 | [官方文档/论文](https://arxiv.org/abs/2002.05202) |
| Foundation models | selective state space model | 选择性状态空间模型 | [官方文档/论文](https://arxiv.org/abs/2312.00752) |
| Foundation models | Mamba architecture | Mamba架构 | [官方文档/论文](https://arxiv.org/abs/2312.00752) |
| Foundation models | vision-language model | 视觉语言模型 | [官方文档/论文](https://arxiv.org/abs/2103.00020) |
| Foundation models | image-text contrastive learning | 图文对比学习 | [官方文档/论文](https://arxiv.org/abs/2103.00020) |
| Foundation models | CLIP model | 图文对比预训练模型 | [官方文档/论文](https://arxiv.org/abs/2103.00020) |
| Foundation models | Vision Transformer | 视觉Transformer | [官方文档/论文](https://arxiv.org/abs/2010.11929) |
| Foundation models | image patch token | 图像块词元 | [官方文档/论文](https://arxiv.org/abs/2010.11929) |
| Foundation models | visual projector | 视觉投影模块 | [官方文档/论文](https://arxiv.org/abs/2304.08485) |
| Foundation models | visual instruction tuning | 视觉指令微调 | [官方文档/论文](https://arxiv.org/abs/2304.08485) |
| Foundation models | diffusion language model | 扩散语言模型 | [官方文档/论文](https://arxiv.org/abs/2502.09992) |
| Foundation models | byte-pair encoding | 字节对编码 | [官方文档/论文](https://huggingface.co/docs/tokenizers/main/en/components) |
| Foundation models | WordPiece | WordPiece子词分词 | [官方文档/论文](https://huggingface.co/docs/tokenizers/main/en/components) |
| Foundation models | Unigram tokenizer | Unigram概率分词器 | [官方文档/论文](https://huggingface.co/docs/tokenizers/main/en/components) |
| Foundation models | byte-level BPE | 字节级BPE | [官方文档/论文](https://huggingface.co/docs/transformers/tokenizer_summary) |
| Foundation models | subword tokenization | 子词分词 | [官方文档/论文](https://huggingface.co/docs/transformers/tokenizer_summary) |
| Foundation models | tokenizer | 分词器 | [官方文档/论文](https://huggingface.co/docs/transformers/tokenizer_summary) |
| Foundation models | training data deduplication | 训练数据去重 | [官方文档/论文](https://arxiv.org/abs/2107.06499) |
| Foundation models | synthetic training data | 合成训练数据 | [官方文档/论文](https://arxiv.org/abs/2306.11644) |
| Foundation models | chat template | 对话模板 | [官方文档/论文](https://huggingface.co/docs/transformers/main/en/chat_templating) |
| Foundation models | control token | 控制词元 | [官方文档/论文](https://huggingface.co/docs/transformers/main/en/chat_templating) |
| Reasoning | chain-of-thought prompting | 思维链提示 | [官方文档/论文](https://arxiv.org/abs/2201.11903) |
| Reasoning | few-shot chain-of-thought | 少样本思维链 | [官方文档/论文](https://arxiv.org/abs/2201.11903) |
| Reasoning | zero-shot chain-of-thought | 零样本思维链 | [官方文档/论文](https://arxiv.org/abs/2205.11916) |
| Reasoning | scratchpad reasoning | 草稿区推理 | [官方文档/论文](https://arxiv.org/abs/2112.00114) |
| Reasoning | program-of-thoughts prompting | 程序思维提示 | [官方文档/论文](https://arxiv.org/abs/2211.12588) |
| Reasoning | program-aided language model | 程序辅助语言模型 | [官方文档/论文](https://arxiv.org/abs/2211.10435) |
| Reasoning | tool-integrated reasoning | 工具集成推理 | [官方文档/论文](https://arxiv.org/abs/2309.17452) |
| Reasoning | ToRA reasoning agent | ToRA推理智能体 | [官方文档/论文](https://arxiv.org/abs/2309.17452) |
| Reasoning | Language Agent Tree Search | 语言智能体树搜索 | [官方文档/论文](https://arxiv.org/abs/2310.04406) |
| Reasoning | Monte Carlo tree search | 蒙特卡洛树搜索 | [官方文档/论文](https://arxiv.org/abs/2310.04406) |
| Reasoning | verifier-guided search | 验证器引导搜索 | [官方文档/论文](https://arxiv.org/abs/2408.03314) |
| Reasoning | adaptive test-time compute | 自适应测试时计算 | [官方文档/论文](https://arxiv.org/abs/2408.03314) |
| Reasoning | parallel reasoning | 并行推理路径探索 | [官方文档/论文](https://arxiv.org/abs/2408.03314) |
| Reasoning | sequential refinement | 序贯改进推理 | [官方文档/论文](https://arxiv.org/abs/2408.03314) |
| Reasoning | Self-Refine | 自反馈迭代改进 | [官方文档/论文](https://arxiv.org/abs/2303.17651) |
| Reasoning | LLM overthinking | 大模型过度思考 | [官方文档/论文](https://arxiv.org/abs/2412.21187) |
| Reasoning | counterfactual reasoning | 反事实推理 | [官方文档/论文](https://arxiv.org/abs/2307.02477) |
| Reasoning | pass@k | k次采样通过率 | [官方文档/论文](https://arxiv.org/abs/2107.03374) |
| Reasoning | pass^k | 连续k次成功率 | [官方文档/论文](https://www.anthropic.com/research/demystifying-evals-for-ai-agents) |
| Reasoning | greedy decoding | 贪婪解码 | [官方文档/论文](https://huggingface.co/docs/transformers/main/en/generation_strategies) |
| Reasoning | beam search | 束搜索 | [官方文档/论文](https://huggingface.co/docs/transformers/main/en/generation_strategies) |
| Reasoning | multinomial sampling | 多项分布采样 | [官方文档/论文](https://huggingface.co/docs/transformers/main/en/generation_strategies) |
| Reasoning | nucleus sampling | 核采样 | [官方文档/论文](https://arxiv.org/abs/1904.09751) |
| Reasoning | top-k sampling | 前k词元采样 | [官方文档/论文](https://huggingface.co/docs/transformers/main/en/main_classes/text_generation) |
| Reasoning | repetition penalty | 重复惩罚 | [官方文档/论文](https://huggingface.co/docs/transformers/main/en/main_classes/text_generation) |
| Reasoning | stop sequence | 停止序列 | [官方文档/论文](https://huggingface.co/docs/transformers/main/en/main_classes/text_generation) |
| Inference | tensor parallelism | 张量并行 | [官方文档/论文](https://docs.vllm.ai/en/latest/serving/parallelism_scaling/) |
| Inference | pipeline parallelism | 流水线并行 | [官方文档/论文](https://docs.vllm.ai/en/latest/serving/parallelism_scaling/) |
| Inference | expert parallelism | 专家并行 | [官方文档/论文](https://docs.vllm.ai/en/latest/serving/parallelism_scaling/) |
| Inference | prefill-decode disaggregation | 预填充解码分离 | [官方文档/论文](https://docs.vllm.ai/en/latest/features/disagg_prefill/) |
