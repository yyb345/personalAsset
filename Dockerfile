# ============================================
# Stage 1: 构建前端 Vue 应用
# ============================================
FROM node:20-alpine AS frontend-builder

WORKDIR /frontend

# 复制前端依赖文件
COPY frontend/package*.json ./

# 安装前端依赖
RUN npm ci --only=production

# 复制前端源代码
COPY frontend/ ./

# 构建前端应用
RUN npm run build

# ============================================
# Stage 2: 构建后端 Java 应用
# ============================================
FROM maven:3.9-eclipse-temurin-17 AS backend-builder

WORKDIR /app

# 复制 pom.xml 并下载依赖（利用 Docker 缓存）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制后端源代码
COPY src ./src

# 复制前端构建产物到后端静态资源目录
COPY --from=frontend-builder /frontend/dist ./src/main/resources/static

# 构建后端应用
RUN mvn clean package -DskipTests

# ============================================
# Stage 3: 最终运行阶段
# ============================================
FROM eclipse-temurin:17-jre

WORKDIR /app

# 创建必要的目录
RUN mkdir -p /data /logs /app/uploads/subtitles /app/uploads/audio/standard /app/downloads

# 安装必要的系统工具
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    python3 \
    python3-pip \
    python3-venv \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# 安装 yt-dlp
RUN pip3 install --no-cache-dir --break-system-packages yt-dlp

# 验证 yt-dlp 安装
RUN yt-dlp --version

# 复制构建好的 jar 包
COPY --from=backend-builder /app/target/*.jar app.jar

# 设置环境变量
ENV SPRING_DATASOURCE_URL=jdbc:sqlite:/data/finance.db \
    SERVER_PORT=8080 \
    JAVA_OPTS="-Xmx512m -Xms256m" \
    TZ=Asia/Shanghai

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 运行应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

