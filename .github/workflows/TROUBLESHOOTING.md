# GitHub Actions 故障排查指南

## ❌ 常见错误及解决方案

### 错误 1: pom.xml 文件找不到

**错误信息：**
```
No file in /home/runner/work/personalAsset/personalAsset matched to [**/pom.xml], 
make sure you have checked out the target repository
```

**原因：**
- `actions/setup-java@v4` 中的 `cache: 'maven'` 选项在代码 checkout 完成前就尝试查找 `pom.xml`
- 或者 checkout 步骤失败

**解决方案：** ✅ 已修复
- 移除 `setup-java` 中的 `cache: 'maven'` 配置
- 在 checkout 后单独使用 `actions/cache` 来缓存 Maven 依赖
- 添加验证步骤确保文件存在

**正确的配置：**
```yaml
steps:
  - name: Checkout code
    uses: actions/checkout@v4
  
  - name: Cache Maven packages
    uses: actions/cache@v3
    with:
      path: ~/.m2/repository
      key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
      restore-keys: |
        ${{ runner.os }}-maven-
  
  - name: Set up JDK 17
    uses: actions/setup-java@v4
    with:
      java-version: '17'
      distribution: 'temurin'
      # 不使用 cache: 'maven'
```

---

### 错误 2: Personal Access Token 没有 workflow 权限

**错误信息：**
```
refusing to allow a Personal Access Token to create or update workflow 
`.github/workflows/cd.yml` without `workflow` scope
```

**原因：**
GitHub 要求推送 `.github/workflows/` 目录时 token 必须有 `workflow` scope

**解决方案：**
1. 访问 https://github.com/settings/tokens
2. 创建新 token，勾选：
   - ✅ `repo`
   - ✅ `workflow`
3. 更新本地 Git 凭证：
   ```bash
   git credential-osxkeychain erase
   # 输入:
   host=github.com
   protocol=https
   # 按两次回车
   ```
4. 重新推送：
   ```bash
   git push origin main
   # 使用新 token 作为密码
   ```

详见：`SETUP-GITHUB.md`

---

### 错误 3: Docker Hub 登录失败

**错误信息：**
```
Error: denied: requested access to the resource is denied
```

**原因：**
- Docker Hub credentials 未配置或配置错误
- Token 权限不足

**解决方案：**
1. 在 GitHub 仓库中添加 Secrets：
   - `DOCKER_USERNAME`: Docker Hub 用户名
   - `DOCKER_PASSWORD`: Docker Hub 密码或访问令牌

2. 获取 Docker Hub Token：
   - 登录 https://hub.docker.com/
   - Settings → Security → New Access Token
   - 选择 Read, Write, Delete 权限
   - 复制 token 并添加到 GitHub Secrets

---

### 错误 4: Maven 构建失败

**错误信息：**
```
Failed to execute goal ... compilation failure
```

**可能原因：**
- Java 版本不匹配
- 依赖下载失败
- 代码编译错误

**解决方案：**
1. 检查 Java 版本是否与 `pom.xml` 一致（应为 17）
2. 查看详细的构建日志
3. 本地测试构建：
   ```bash
   mvn clean package
   ```
4. 确保所有依赖可以正常下载

---

### 错误 5: Docker 镜像构建失败

**错误信息：**
```
ERROR: failed to solve: process "/bin/sh -c ..." did not complete successfully
```

**可能原因：**
- Dockerfile 语法错误
- 基础镜像不可用
- 网络问题

**解决方案：**
1. 本地测试 Docker 构建：
   ```bash
   docker build -t finance-app .
   ```
2. 检查 Dockerfile 中的命令
3. 确保基础镜像可访问

---

### 错误 6: Artifact 上传失败

**错误信息：**
```
Unable to find any artifacts for the associated workflow
```

**原因：**
- 构建产物路径错误
- 构建步骤失败

**解决方案：**
1. 确保 Maven 构建成功
2. 检查 `target/` 目录是否有 `.jar` 文件
3. 验证 artifact 路径配置：
   ```yaml
   - name: Upload build artifact
     uses: actions/upload-artifact@v4
     with:
       name: finance-app
       path: target/*.jar  # 确保路径正确
   ```

---

## 🔍 调试技巧

### 1. 查看工作目录内容

在 workflow 中添加调试步骤：

```yaml
- name: Debug - List files
  run: |
    pwd
    ls -la
    find . -name "pom.xml"
```

### 2. 查看环境变量

```yaml
- name: Debug - Environment
  run: |
    echo "Java version:"
    java -version
    echo "Maven version:"
    mvn -version
    echo "Working directory:"
    pwd
```

### 3. 查看 Maven 依赖树

```yaml
- name: Debug - Maven Dependencies
  run: mvn dependency:tree
```

### 4. 保存构建日志

```yaml
- name: Upload Maven logs
  if: failure()
  uses: actions/upload-artifact@v4
  with:
    name: maven-logs
    path: |
      target/surefire-reports/
      *.log
```

---

## 📊 查看 Actions 日志

1. 进入 GitHub 仓库
2. 点击 **Actions** 标签
3. 选择失败的 workflow run
4. 点击失败的 job
5. 展开失败的 step 查看详细日志

---

## ✅ 验证配置是否正确

运行以下检查清单：

- [ ] GitHub Token 有 `workflow` 权限
- [ ] GitHub Secrets 已配置（DOCKER_USERNAME, DOCKER_PASSWORD）
- [ ] `pom.xml` 在仓库根目录
- [ ] 本地可以成功构建：`mvn clean package`
- [ ] 本地可以构建 Docker 镜像：`docker build -t test .`
- [ ] Dockerfile 中的路径正确

---

## 🆘 获取帮助

如果以上方案都无法解决问题：

1. 检查 GitHub Actions 日志的完整错误信息
2. 在本地复现问题
3. 查看 GitHub Actions 文档：https://docs.github.com/en/actions
4. 检查相关 Action 的 GitHub Issues

---

## 📝 最佳实践

1. **逐步测试**：先在本地测试，再推送到 GitHub
2. **保持简单**：从最简单的 workflow 开始，逐步添加功能
3. **使用缓存**：合理使用缓存加速构建
4. **失败快速**：构建失败时立即停止，不继续后续步骤
5. **安全第一**：永远不要在日志中输出 secrets

---

## 🔄 重新运行 Workflow

如果修复了问题，有两种方式重新运行：

1. **重新运行失败的 workflow**：
   - 进入 Actions → 选择失败的 run
   - 点击右上角 "Re-run all jobs"

2. **推送新提交触发**：
   ```bash
   git commit --allow-empty -m "Trigger CI"
   git push origin main
   ```

