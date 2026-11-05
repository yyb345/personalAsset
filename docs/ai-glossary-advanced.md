# 进阶 LLM 术语专项补充

> 本文记录第一轮后训练专项。第二轮已扩充至997个概念、2672个名称/别名，见[Agent、基础模型与推理](ai-glossary-agents-models-reasoning.md)。

整理日期：2026-11-03。此次核对 **102 个进阶概念**，其中新增97个，SFT、PEFT、LoRA、ToT、HITL等5个沿用原ID合并更新。词库由795增至 **892个独立概念、2170个名称/别名**，未把缩写或中文翻译另算成概念。

优先覆盖后训练、偏好优化、奖励与过程监督、参数高效微调、测试时扩展、缓存与推测解码、注意力结构、量化、智能体与上下文工程、检索和评价。

## 内容与来源

此次条目依据下列官方文档及原始论文人工核对后撰写简短英文概括和中文解释，不是直接复制论文全文，也不表示相关机构背书。初始Google词库的CC BY 4.0归属仍保留；新来源各自的许可不因收录而改变。每条资源记录均包含HTTPS来源，独立整理数据在 `scripts/glossary/advanced_terms.json`，运行时数据仍统一为 `src/main/resources/glossary/terms.json`。

不同概念保留区分：post-training是预训练后的训练阶段，post-training quantization是训练后量化；DPO与PPO/GRPO不是同一算法；过程奖励与结果奖励不是同一监督粒度。ReAct仅添加带技术限定词的匹配形式，避免把英文动词react错误高亮；不添加裸的target model等含义过宽的别名。

## 重建与验证

```bash
# 不联网，把已核对进阶词条合入当前词库（可重复运行）
python3 scripts/glossary/merge_advanced.py
# 重新采集基础词库时也会自动合并进阶词条
python3 scripts/glossary/collect_glossary.py
# 合并、重复运行、别名冲突与字段校验
python3 -m unittest discover -s scripts/glossary -p '*test.py'
# 所有进阶名称/别名的实际Java匹配，以及108句线上字幕快照回归
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn test
```

合并以稳定ID为准；发现新旧概念之间的别名归属冲突时明确报错，要求先合并概念，不静默覆盖。同一脚本重复运行不再新增条目，也不会丢失其他基础词库内容。以当前缓存重跑完整采集，输出与合并前逐字节相同。

本轮验证：102个核对概念的438个名称/别名均能在实际Java匹配器中完整定位到对应概念；中英混合、emoji偏移、post-training与post-training quantization重叠、普通react与endpoints防误匹配测试通过。108句线上字幕快照命中69句、173处、36个概念（扩充前171处、35个概念），新增命中包括连续思维链。4项Python合并测试通过，后端测试、前端构建和3项文本分段测试通过。本轮未重新启动真实ES实例，实际ES查询链路沿用此前的集成测试；本轮新增内容测试覆盖数据合并与精确匹配。

部署后词库内容哈希会改变，应用第一次高亮请求将自动创建并填充新版本ES索引，无需手动导入。该步骤沿用现有索引生命周期，没有新增持续爬取任务。

## 本次核对清单

| 分类 | 英文标准名 | 中文名 | 来源 |
| --- | --- | --- | --- |
| Post-training | post-training | 后训练 | [文档/论文](https://github.com/huggingface/trl) |
| Post-training | continued pretraining | 持续预训练 | [文档/论文](https://arxiv.org/abs/2004.10964) |
| Post-training | domain-adaptive pretraining | 领域自适应预训练 | [文档/论文](https://arxiv.org/abs/2004.10964) |
| Post-training | task-adaptive pretraining | 任务自适应预训练 | [文档/论文](https://arxiv.org/abs/2004.10964) |
| Post-training | direct preference optimization | 直接偏好优化 | [文档/论文](https://arxiv.org/abs/2305.18290) |
| Post-training | preference optimization | 偏好优化 | [文档/论文](https://arxiv.org/abs/2305.18290) |
| Post-training | reference model | 参考模型 | [文档/论文](https://arxiv.org/abs/2305.18290) |
| Post-training | group relative policy optimization | 组相对策略优化 | [文档/论文](https://huggingface.co/docs/trl/main/en/grpo_trainer) |
| Post-training | rollout | 轨迹采样 | [文档/论文](https://huggingface.co/docs/trl/main/en/grpo_trainer) |
| Post-training | KL regularization | KL正则化 | [文档/论文](https://huggingface.co/docs/trl/main/en/grpo_trainer) |
| Post-training | proximal policy optimization | 近端策略优化 | [文档/论文](https://arxiv.org/abs/1707.06347) |
| Post-training | REINFORCE leave-one-out | 留一法REINFORCE | [文档/论文](https://huggingface.co/docs/trl/main/en/rloo_trainer) |
| Post-training | on-policy training | 同策略训练 | [文档/论文](https://huggingface.co/docs/trl/main/en/rloo_trainer) |
| Post-training | Kahneman-Tversky optimization | 卡尼曼-特沃斯基优化 | [文档/论文](https://arxiv.org/abs/2402.01306) |
| Post-training | odds ratio preference optimization | 赔率比偏好优化 | [文档/论文](https://arxiv.org/abs/2403.07691) |
| Post-training | simple preference optimization | 简单偏好优化 | [文档/论文](https://arxiv.org/abs/2405.14734) |
| Post-training | reinforcement learning from AI feedback | 基于AI反馈的强化学习 | [文档/论文](https://arxiv.org/abs/2212.08073) |
| Post-training | Constitutional AI | 宪法式AI | [文档/论文](https://arxiv.org/abs/2212.08073) |
| Post-training | AI alignment | AI对齐 | [文档/论文](https://arxiv.org/abs/2212.08073) |
| Post-training | reinforcement learning with verifiable rewards | 可验证奖励强化学习 | [文档/论文](https://arxiv.org/abs/2501.12948) |
| Post-training | cold-start data | 冷启动数据 | [文档/论文](https://arxiv.org/abs/2501.12948) |
| Post-training | reasoning distillation | 推理蒸馏 | [文档/论文](https://arxiv.org/abs/2501.12948) |
| Post-training | rejection sampling fine-tuning | 拒绝采样微调 | [文档/论文](https://arxiv.org/abs/2501.12948) |
| Post-training | reward model | 奖励模型 | [文档/论文](https://huggingface.co/docs/trl/main/en/reward_trainer) |
| Post-training | outcome reward model | 结果奖励模型 | [文档/论文](https://huggingface.co/docs/trl/main/en/reward_trainer) |
| Post-training | preference dataset | 偏好数据集 | [文档/论文](https://huggingface.co/docs/trl/main/en/reward_trainer) |
| Reasoning | process reward model | 过程奖励模型 | [文档/论文](https://arxiv.org/abs/2305.20050) |
| Reasoning | process supervision | 过程监督 | [文档/论文](https://arxiv.org/abs/2305.20050) |
| Reasoning | outcome supervision | 结果监督 | [文档/论文](https://arxiv.org/abs/2305.20050) |
| Reasoning | best-of-N sampling | N选优采样 | [文档/论文](https://arxiv.org/abs/2305.20050) |
| Post-training | reward hacking | 奖励投机 | [文档/论文](https://arxiv.org/abs/2210.10760) |
| Post-training | reward overoptimization | 奖励过度优化 | [文档/论文](https://arxiv.org/abs/2210.10760) |
| Fine-tuning | supervised fine-tuning | 监督微调 | [文档/论文](https://huggingface.co/docs/trl/main/en/sft_trainer) |
| Fine-tuning | full-parameter fine-tuning | 全参数微调 | [文档/论文](https://huggingface.co/docs/trl/main/en/sft_trainer) |
| Fine-tuning | sequence packing | 序列打包 | [文档/论文](https://huggingface.co/docs/trl/main/en/sft_trainer) |
| Fine-tuning | assistant-only loss | 仅助手输出损失 | [文档/论文](https://huggingface.co/docs/trl/main/en/sft_trainer) |
| Fine-tuning | parameter-efficient fine-tuning | 参数高效微调 | [文档/论文](https://huggingface.co/docs/peft/main/en/conceptual_guides/lora) |
| Fine-tuning | low-rank adaptation | 低秩适配 | [文档/论文](https://huggingface.co/docs/peft/main/en/conceptual_guides/lora) |
| Fine-tuning | LoRA adapter | LoRA适配器 | [文档/论文](https://huggingface.co/docs/peft/main/en/conceptual_guides/lora) |
| Fine-tuning | quantized low-rank adaptation | 量化低秩适配 | [文档/论文](https://arxiv.org/abs/2305.14314) |
| Fine-tuning | 4-bit NormalFloat | 四比特正态浮点 | [文档/论文](https://arxiv.org/abs/2305.14314) |
| Fine-tuning | double quantization | 双重量化 | [文档/论文](https://arxiv.org/abs/2305.14314) |
| Fine-tuning | paged optimizer | 分页优化器 | [文档/论文](https://arxiv.org/abs/2305.14314) |
| Reasoning | test-time scaling | 测试时扩展 | [文档/论文](https://arxiv.org/abs/2501.19393) |
| Reasoning | test-time compute | 测试时计算 | [文档/论文](https://arxiv.org/abs/2501.19393) |
| Reasoning | budget forcing | 预算强制 | [文档/论文](https://arxiv.org/abs/2501.19393) |
| Reasoning | reasoning budget | 推理预算 | [文档/论文](https://arxiv.org/abs/2501.19393) |
| Reasoning | self-consistency decoding | 自一致性解码 | [文档/论文](https://arxiv.org/abs/2203.11171) |
| Reasoning | majority voting | 多数投票 | [文档/论文](https://arxiv.org/abs/2203.11171) |
| Reasoning | latent reasoning | 潜在空间推理 | [文档/论文](https://arxiv.org/abs/2412.06769) |
| Reasoning | Chain of Continuous Thought | 连续思维链 | [文档/论文](https://arxiv.org/abs/2412.06769) |
| Reasoning | tree of thoughts | 思维树 | [文档/论文](https://arxiv.org/abs/2305.10601) |
| Reasoning | reasoning trace | 推理轨迹 | [文档/论文](https://arxiv.org/abs/2501.12948) |
| Reasoning | reasoning model | 推理模型 | [文档/论文](https://arxiv.org/abs/2501.12948) |
| Inference | key-value cache | 键值缓存 | [文档/论文](https://huggingface.co/docs/transformers/kv_cache) |
| Inference | static KV cache | 静态键值缓存 | [文档/论文](https://huggingface.co/docs/transformers/kv_cache) |
| Inference | dynamic KV cache | 动态键值缓存 | [文档/论文](https://huggingface.co/docs/transformers/kv_cache) |
| Inference | KV cache offloading | 键值缓存卸载 | [文档/论文](https://huggingface.co/docs/transformers/kv_cache) |
| Inference | KV cache quantization | 键值缓存量化 | [文档/论文](https://huggingface.co/docs/transformers/kv_cache) |
| Inference | prefix caching | 前缀缓存 | [文档/论文](https://docs.vllm.ai/en/latest/features/automatic_prefix_caching/) |
| Inference | speculative decoding | 推测解码 | [文档/论文](https://huggingface.co/docs/transformers/main/en/assisted_decoding) |
| Inference | draft model | 草稿模型 | [文档/论文](https://huggingface.co/docs/transformers/main/en/assisted_decoding) |
| Inference | target model in speculative decoding | 推测解码目标模型 | [文档/论文](https://huggingface.co/docs/transformers/main/en/assisted_decoding) |
| Inference | prompt lookup decoding | 提示查找解码 | [文档/论文](https://huggingface.co/docs/transformers/main/en/assisted_decoding) |
| Inference | continuous batching | 连续批处理 | [文档/论文](https://huggingface.co/docs/transformers/main/en/continuous_batching) |
| Inference | prefill phase | 预填充阶段 | [文档/论文](https://huggingface.co/docs/transformers/main/en/continuous_batching) |
| Inference | decode phase | 解码阶段 | [文档/论文](https://huggingface.co/docs/transformers/main/en/continuous_batching) |
| Inference | chunked prefill | 分块预填充 | [文档/论文](https://docs.vllm.ai/en/latest/configuration/optimization/) |
| Inference | time to first token | 首词元延迟 | [文档/论文](https://docs.vllm.ai/en/latest/configuration/optimization/) |
| Inference | inter-token latency | 词元间延迟 | [文档/论文](https://docs.vllm.ai/en/latest/configuration/optimization/) |
| Inference | PagedAttention | 分页注意力 | [文档/论文](https://arxiv.org/abs/2309.06180) |
| Inference | FlashAttention | 高效精确注意力 | [文档/论文](https://arxiv.org/abs/2205.14135) |
| Architecture | multi-query attention | 多查询注意力 | [文档/论文](https://arxiv.org/abs/1911.02150) |
| Architecture | grouped-query attention | 分组查询注意力 | [文档/论文](https://arxiv.org/abs/2305.13245) |
| Architecture | multi-head latent attention | 多头潜在注意力 | [文档/论文](https://arxiv.org/abs/2405.04434) |
| Architecture | rotary position embedding | 旋转位置编码 | [文档/论文](https://arxiv.org/abs/2104.09864) |
| Quantization | post-training quantization | 训练后量化 | [文档/论文](https://huggingface.co/docs/transformers/main/en/quantization/concept_guide) |
| Quantization | quantization-aware training | 量化感知训练 | [文档/论文](https://huggingface.co/docs/transformers/main/en/quantization/concept_guide) |
| Quantization | activation-aware weight quantization | 激活感知权重量化 | [文档/论文](https://arxiv.org/abs/2306.00978) |
| Quantization | GPTQ | GPTQ量化 | [文档/论文](https://arxiv.org/abs/2210.17323) |
| Quantization | SmoothQuant | 平滑量化 | [文档/论文](https://arxiv.org/abs/2211.10438) |
| Agents | tool calling | 工具调用 | [文档/论文](https://www.anthropic.com/research/building-effective-agents) |
| Agents | orchestrator-workers workflow | 编排器与工作者工作流 | [文档/论文](https://www.anthropic.com/research/building-effective-agents) |
| Agents | evaluator-optimizer workflow | 评价与优化工作流 | [文档/论文](https://www.anthropic.com/research/building-effective-agents) |
| Agents | human-in-the-loop | 人在回路 | [文档/论文](https://www.anthropic.com/research/building-effective-agents) |
| Agents | ReAct prompting | ReAct推理行动框架 | [文档/论文](https://arxiv.org/abs/2210.03629) |
| Agents | Reflexion framework | Reflexion反思框架 | [文档/论文](https://arxiv.org/abs/2303.11366) |
| Agents | Toolformer | 工具学习模型Toolformer | [文档/论文](https://arxiv.org/abs/2302.04761) |
| Agents | context engineering | 上下文工程 | [文档/论文](https://www.anthropic.com/research/effective-context-engineering-for-ai-agents) |
| Agents | context compaction | 上下文压缩 | [文档/论文](https://www.anthropic.com/research/effective-context-engineering-for-ai-agents) |
| Agents | agent memory | 智能体记忆 | [文档/论文](https://www.anthropic.com/research/effective-context-engineering-for-ai-agents) |
| Agents | context rot | 上下文腐化 | [文档/论文](https://www.anthropic.com/research/effective-context-engineering-for-ai-agents) |
| Agents | Model Context Protocol | 模型上下文协议 | [文档/论文](https://modelcontextprotocol.io/docs/learn/architecture) |
| Agents | MCP server | MCP服务端 | [文档/论文](https://modelcontextprotocol.io/docs/learn/architecture) |
| Agents | MCP client | MCP客户端 | [文档/论文](https://modelcontextprotocol.io/docs/learn/architecture) |
| Retrieval | contextual retrieval | 上下文化检索 | [文档/论文](https://www.anthropic.com/research/contextual-retrieval) |
| Retrieval | hybrid retrieval | 混合检索 | [文档/论文](https://www.anthropic.com/research/contextual-retrieval) |
| Retrieval | dense retrieval | 稠密检索 | [文档/论文](https://www.anthropic.com/research/contextual-retrieval) |
| Retrieval | sparse retrieval | 稀疏检索 | [文档/论文](https://www.anthropic.com/research/contextual-retrieval) |
| Retrieval | query rewriting | 查询改写 | [文档/论文](https://arxiv.org/abs/2305.14283) |
| Retrieval | hypothetical document embeddings | 假设文档嵌入 | [文档/论文](https://arxiv.org/abs/2212.10496) |
| Evaluation | LLM-as-a-judge | 大模型裁判 | [文档/论文](https://arxiv.org/abs/2306.05685) |

共核对 46 个官方文档/原始论文来源页面。
