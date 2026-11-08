# SEO 第一批实现

## 页面与索引

- `/` 和 `/index.html` 返回构建时从原有 `Landing.vue` 预渲染的首页，canonical 统一为 `/`，服务端按站点配置写入 canonical。初始 HTML 包含正文、导航和原版样式，无需 JavaScript 即可读取。`/` 固定为英文，新增 `/zh/` 固定为中文；显式 URL 优先于已保存偏好。切换语言会导航到对应首页 URL，工作台仍保存语言偏好，移动菜单及演示交互沿用原组件。两种语言各自 canonical，并互相提供 `en`、`zh-Hans` 和 `x-default` hreflang。没有独立维护另一套首页设计。
- `/guides`、`/guides/{slug}`、`/video` 和符合条件的 `/video/{videoId}` 可索引。
- Vue 账号、账单、学习工作台及当前工具页面返回 `X-Robots-Tag: noindex, follow`。工具需要独立介绍页后再开放索引。
- 视频公开页面仅包含处理完成且至少存在一条非空字幕的视频。详情、列表和 sitemap 共用数据库条件；未知或不符合条件的详情返回 404。
- 这是最低内容质量条件，不是人工发布审核流程；没有修改数据库结构，也没有改变工作台视频访问权限。

## 配置

`site.base-url`（环境变量 `SITE_BASE_URL`）设置 canonical 的 HTTP(S) 域名，默认 `https://www.xlearning.top`；`site.name`（`SITE_NAME`）设置站点名称。sitemap、robots.txt 的 sitemap 地址及公开页面 canonical 共用该域名。应用域名跳转仍需在反向代理配置 HTTP → HTTPS、裸域 → 主域的永久重定向。

## 构建与上线验收

按 [build.md](build.md) 构建与验证 JAR。首页源码在 `frontend/src/views/Landing.vue`，沿用原有样式与双语内容。构建先生成客户端资源，再使用 Vue server renderer 输出 `frontend/dist/home.html` 和 `frontend/dist/home-zh.html`；后端分别在 `/` 和 `/zh/` 读取对应文件，工作台仍读取空的 `index.html` 应用入口。生产和开发共用同一组件，工作台可通过 Vue Router 返回首页。Vite 开发模式仍为客户端渲染，需访问后端地址验证预渲染 HTML。

部署后检查首页原始 HTML、`/robots.txt`、`/sitemap.xml`，以及指南和视频详情的 canonical；确认账号和工作台响应有 noindex，未知详情返回 404。最后在 Search Console 检查代表 URL 并提交 sitemap。代码尚未部署；已有线上核查和 Search Console 操作记录见 `seo-live-audit.md`。

中文指南、中文视频独立 URL、视频人工发布状态和列表分页留待后续迭代；当前只为首页建立中英文独立 URL。

## Discovery Loop 双语文章

- 英文：`/articles/jeff-dean-discovery-loop-ai-scientific-discovery`
- 中文：`/zh/articles/jeff-dean-discovery-loop-ai-scientific-discovery`
- 正文与本地化 SEO 字段：`frontend/src/features/articles/discovery-loop.js`。
- 页面组件：`frontend/src/views/DiscoveryLoopArticle.vue`，包含语言切换、目录、FAQ 和视频字幕入口；首页页脚提供文章链接。
- 前端构建输出 `article-discovery-loop-en.html` 与 `article-discovery-loop-zh.html`，由 `HomeController` 提供公开可索引的完整 HTML。两版分别设置 canonical、双向 hreflang、Open Graph 与 Article JSON-LD，并纳入 `/sitemap.xml`。
- `npm --prefix frontend run build` 检查两版正文与元信息；`HomeControllerTest` 验证服务端路由、本地化正文及配置域名下的 SEO 地址。
