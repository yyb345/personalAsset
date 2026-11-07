# SEO 第一批实现

## 页面与索引

- `/` 和 `/index.html` 保留原有 Vue 双语首页、布局、语言切换及演示交互，canonical 统一为 `/`，服务端按站点配置写入 canonical。首页正文仍依赖 JavaScript 渲染；后续如增加预渲染，应直接复用原组件，不另建不同设计的首页。
- `/guides`、`/guides/{slug}`、`/video` 和符合条件的 `/video/{videoId}` 可索引。
- Vue 账号、账单、学习工作台及当前工具页面返回 `X-Robots-Tag: noindex, follow`。工具需要独立介绍页后再开放索引。
- 视频公开页面仅包含处理完成且至少存在一条非空字幕的视频。详情、列表和 sitemap 共用数据库条件；未知或不符合条件的详情返回 404。
- 这是最低内容质量条件，不是人工发布审核流程；没有修改数据库结构，也没有改变工作台视频访问权限。

## 配置

`site.base-url`（环境变量 `SITE_BASE_URL`）设置 canonical 的 HTTP(S) 域名，默认 `https://www.xlearning.top`；`site.name`（`SITE_NAME`）设置站点名称。sitemap、robots.txt 的 sitemap 地址及公开页面 canonical 共用该域名。应用域名跳转仍需在反向代理配置 HTTP → HTTPS、裸域 → 主域的永久重定向。

## 构建与上线验收

按 [build.md](build.md) 构建与验证 JAR。首页源码在 `frontend/src/views/Landing.vue`，沿用原有样式与双语内容；生产和开发共用同一组件，工作台可通过 Vue Router 返回首页。

部署后检查首页原始 HTML、`/robots.txt`、`/sitemap.xml`，以及指南和视频详情的 canonical；确认账号和工作台响应有 noindex，未知详情返回 404。最后在 Search Console 检查代表 URL 并提交 sitemap。当前修改未执行线上部署或 Search Console 操作。

内容扩充、视频人工发布状态、列表分页和独立多语言 URL 留待后续迭代。
