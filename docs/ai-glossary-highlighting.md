# 字幕 AI 术语解释

词库含 997 个独立概念、2672 个中英名称/别名，数据及许可见 [来源说明](ai-glossary-sources.md)。

## 本地运行

需要 Java 17、Node 及 Elasticsearch 8.12（不需要 IK 插件）。本地可运行：

```bash
docker compose -f compose.glossary.yml up -d
export JAVA_HOME=$(/usr/libexec/java_home -v 17) # macOS
mvn spring-boot:run
```

前端按现有 `frontend/README.md` 启动，打开 `/transcribe?videoId=EsaUQNx59vA`。视频必须已完成字幕解析。右侧字幕蓝色术语支持悬停和键盘聚焦查看双语解释；勾选框可开关识别，人工标注和点击字幕定位仍可使用。

已有配置 `ES_ENABLED`（默认 true）、`ES_HOST`（默认 localhost）、`ES_PORT`（默认 9200）控制连接。示例 compose 仅绑定本机，关闭安全认证用于本地开发。

第一次高亮请求自动创建并 bulk 写入 `ai_glossary_v1_<词库SHA256前16位>` 索引；`text` 使用 standard analyzer，`query` 使用 percolator 类型。每概念一条 bool/should 查询，别名用 match_phrase。创建后 refresh 再查询。词库变化创建新索引，不覆盖旧索引；旧版本索引可在确认无旧应用使用后由运维清理。多实例同时初始化支持已存在索引；中断后再次初始化会重新幂等写入全量规则。

## 接口

```bash
curl -s http://localhost:8081/api/glossary/highlight \
  -H 'Content-Type: application/json' \
  -d '{"texts":["LLM uses token and attention.","大语言模型使用词元。"],"language":"zh"}'
curl -s http://localhost:8081/api/glossary/status
```

`results` 与输入同序，每项包含原文 `text` 和 `spans`（start、end、termId、termEn、termZh、definitionEn、definitionZh）。索引为 UTF-16、end 不包含末尾，与 JS `slice` 一致。返回结构化位置而非 HTML，Vue 通过文本节点安全渲染。language 可选 zh/en；响应始终保留双语解释供客户端选择。

每批最多 100 段，每段最多 10000 UTF-16 字符；非法输入返回 400。前端每批80句，支持请求取消及重试。ES 用多文档 percolate 查询和 `_percolator_document_slot` 获得每句候选词；随后做精确字面定位，避免分析器把近似词或部分短语误高亮。英文大小写不敏感、有 ASCII 单词边界；中文可连续出现。重叠时最长词优先，相同长度用稳定 ID 排序。当前为字面匹配，不做模糊匹配、词形推断或跨字幕句合并。

响应 `engine=elasticsearch` 表示实际使用 percolator；ES 禁用/连接失败/分片失败时使用相同精确定位器全词库降级，并返回 `engine=local`，失败后60秒再尝试 ES。`status.indexReady` 表示当前进程已初始化的状态，不是实时集群健康探针。

## 验证

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn test
GLOSSARY_ES_TEST_URL=http://localhost:9200 JAVA_HOME=$(/usr/libexec/java_home -v 17) \
  mvn -Dtest=GlossaryElasticsearchTest test
cd frontend
node --test src/utils/transcriptSegments.test.js
npm run build
```

真实 ES 测试检查词库所有名称/别名的批量结果与本地结果一致，并断言 engine 为 elasticsearch，不能通过降级掩盖故障。该测试会在目标 ES 创建对应版本索引，应使用开发/测试集群。

2026-11-03 验证结果：Java 17 后端测试通过；前端 build 和3项分段测试通过；真实 Elasticsearch 8.12.0 已创建795条 percolator 规则，全部1733个名称/别名及额外混合文本批次均与本地匹配一致。临时测试实例使用19200端口，测试完成后关闭。本地数据库没有 `EsaUQNx59vA` 记录，未对该视频做端到端播放实测。

## 线上字幕快照 mock 验收

同日通过只读线上接口取得该视频108句完整字幕，保存在 `src/test/resources/glossary/EsaUQNx59vA.json`（出处见同目录 `.source.md`）。不需要导入本地数据库，也不会修改线上数据。

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -Dtest=OnlineSubtitleMockTest test
node scripts/glossary/mock-server.mjs
# 另开终端
cd frontend
VITE_API_BASE_URL=http://127.0.0.1:18081 npm run dev -- --host 127.0.0.1 --port 3100 --strictPort
```

打开 `http://localhost:3100/transcribe?videoId=EsaUQNx59vA`。mock API 默认只绑定127.0.0.1:18081；前端默认后端地址不变，只有显式设置开发环境变量才使用mock地址。

测试通过真实 Java Controller/Service 的 MockMvc 请求（80句+28句）生成 `target/glossary-online-mock.json`，mock服务回放此响应，并拒绝字幕快照以外的请求文本。此轮 engine 为 local，验证真实字幕、API序列化、精确位置和前端渲染；ES 引擎由上面的真实 ES 集成测试另行覆盖。

结果：108句中69句命中，171处高亮，35个不同概念；验证原文完全保留、位置合法不重叠、双语解释非空。真实字幕暴露 `tokens` 和 `large language models` 漏匹配，已增加这两个明确复数以及 `LLMs` 别名，同步采集脚本；词库仍为795概念，名称/别名增至1736个。字面形式之外不自动扩展词形。

浏览器验收：108句与171个术语节点渲染正确；关闭开关保留108句并移除所有术语高亮，开启恢复171处；`llm` 解释显示“大语言模型”和双语定义。修复聚焦引发滚动时解释卡片立即关闭的问题，改为滚动时更新位置；关闭术语或替换文本会清理浮层。

可通过 `GLOSSARY_MOCK_FAIL_FIRST=true node scripts/glossary/mock-server.mjs` 模拟第一次识别请求503；已在浏览器验证原字幕保留、错误提示和重试后80+28两批恢复成功。摘要、登录等接口不在这个mock范围中。

进阶专项补充后，当前词库为892个概念、2170个名称/别名；历史795条ES和171处字幕匹配数字为扩充前的验证记录。进阶内容和最新回归结果见[专项记录](ai-glossary-advanced.md)。

第二轮专项之后，当前词库为997个概念、2672个名称/别名；更早的计数属于历史验证记录。新增105个Agent、基础模型与推理概念及9个合并更新条目见[第二轮记录](ai-glossary-agents-models-reasoning.md)。
