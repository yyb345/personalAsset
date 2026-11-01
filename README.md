http://www.xlearning.top


/dashboard/youtube?debug=true

## Google OAuth 配置

在 Google Cloud 创建 Web application 类型的 OAuth 客户端。
本地回调地址为 `http://localhost:8081/login/oauth2/code/google`；
线上回调地址为 `https://你的域名/login/oauth2/code/google`。

后端读取环境变量 `GOOGLE_CLIENT_ID` 和 `GOOGLE_CLIENT_SECRET`。
本地也可以在项目根目录创建已被 Git 忽略的 `application-local.properties`：

```properties
spring.security.oauth2.client.registration.google.client-id=YOUR_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_CLIENT_SECRET
```

线上通过容器环境变量或服务器上的私密 env 文件注入凭据，修改后重新创建容器。
不要将真实凭据提交到 Git，也不要将本地配置文件打包进镜像。

### 视频 AI Summary

`/transcribe?videoId=...` 的 Export 左侧提供 AI Summary。首次点击读取该视频全部原始字幕，长字幕分段汇总后合并；成功后写入 `youtube_videos.ai_summary`，之后点击直接从数据库读取。失败不保存，可重试。当前单实例内同一视频的并发生成请求会串行执行，避免重复调用；多实例部署需增加分布式锁。

默认接入 DeepSeek 官方接口 `https://api.deepseek.com/chat/completions`，模型为 `deepseek-v4-flash`，关闭思考模式以降低摘要延迟。只需在启动后端的环境中设置密钥：

```bash
export DEEPSEEK_API_KEY='your-deepseek-key'
```

DeepSeek 官方当前将旧模型名 `deepseek-v4-flash` 的请求路由到 V4.1 Flash；新模型名是 `deepseek-flash`，可通过 `AI_MODEL` 覆盖。参见 [DeepSeek 官方文档](https://api-docs.deepseek.com/quick_start/)。

如需更换兼容的提供商，可覆盖 `AI_ENDPOINT`、`AI_API_KEY`、`AI_MODEL`。`AI_API_KEY` 优先于 `DEEPSEEK_API_KEY`；不支持 `thinking` 参数的提供商需设置 `AI_THINKING_MODE=''`。

也可在不提交版本库的 `application-local.properties` 配置 `ai.endpoint`、`ai.api-key`、`ai.model`。密钥只在服务端使用。现有 `spring.jpa.hibernate.ddl-auto=update` 会在启动时添加摘要字段。若部署环境禁用自动建表，先执行：

```sql
ALTER TABLE youtube_videos ADD COLUMN ai_summary TEXT;
```

接口：`POST /api/youtube/summary/{YouTube videoId}`，返回 `{ "summary": "...", "cached": true/false }`。可复用接口 `LanguageModelClient.complete(instruction, content)` 与视频业务、数据库无关。

### ECS 运行时密钥

ECS 部署脚本将 `/etc/finance/secrets` 只读挂载到容器 `/run/secrets`。目录权限为 `700`，其中 `ai.api-key` 文件权限为 `600`、所有者为 root，内容仅为 DeepSeek API Key。Spring Boot 通过 configtree 读取该文件；密钥不写入应用 properties、Git、镜像或容器环境变量。此方式仍在服务器磁盘保存密钥，root 和容器应用可以读取。

可在 ECS 的 Bash 终端中执行以下命令后，在隐藏提示中粘贴已有密钥（输入值不显示、不作为 shell 命令保存）：

```bash
set +x
install -d -m 700 /etc/finance/secrets
IFS= read -r -s -p 'DeepSeek API Key: ' deepseek_key
printf '\n'
if [ -n "$deepseek_key" ]; then
  (umask 077; printf '%s' "$deepseek_key" > /etc/finance/secrets/ai.api-key)
  chmod 600 /etc/finance/secrets/ai.api-key
fi
unset deepseek_key
```

首次接入需要使用更新后的部署脚本重建应用容器，单纯 `docker restart` 不会新增挂载。后续修改密钥后重启应用即可读取。不要通过 `cat`、调试日志或 `set -x` 输出密钥。
