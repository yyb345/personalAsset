# 构建与前端产物验证

生产包统一使用：

```bash
mvn clean package
python3 scripts/verify-package.py target/finance-0.0.1-SNAPSHOT.jar
```

需要 JDK 17、Maven、Node.js 20+、npm，以及首次安装依赖所需的网络。Maven 会执行 `npm ci` 和 `npm run build`，随后把 `frontend/dist` 复制到 JAR 的 `static/`。`npm run build` 会复用原版 `Landing.vue` 预渲染默认英文首页到 `dist/home.html`，按客户端构建 manifest 引入原版首页 CSS，并检查首页正文、导航、语言选择器、演示控件、HTML 元信息、构建资源和 robots.txt。预渲染临时目录 `.prerender` 构建后自动清理。第二条命令逐文件比较 JAR 与本次 dist，检查是否存在遗漏、旧资源，以及后端指南 CSS 是否被保留。

不要手动将 dist 复制到 `src/main/resources/static`。仓库中历史 `static/index.html` 和 `static/assets/` 不再参与打包；后端维护的 `static/css/` 等目录继续参与打包。使用 `clean` 可以清除前一次构建目录中残留的资源。

已经完成一次前端构建后，只运行后端测试时可跳过前端安装和构建（首页集成测试需要 `frontend/dist/home.html`）：

```bash
mvn test -Dfrontend.skip=true
```

该参数也用于 Docker 多阶段构建：Node 阶段先构建并验证前端，Maven 阶段使用该阶段输出的 dist，避免再次安装 Node 依赖。不要在发布本地 JAR 时跳过前端构建，除非已经明确构建过同一版本的 dist。

`aliyun-build.sh` 使用同一 Dockerfile，因此自动使用上述流程；本次修改不执行部署。
