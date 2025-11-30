# 使用多阶段构建来减小最终镜像大小
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 复制 pom.xml 并下载依赖（利用 Docker 缓存）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并构建
COPY src ./src
RUN mvn clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 创建数据目录和日志目录
RUN mkdir -p /data /logs

# 复制构建好的 jar 包
COPY --from=builder /app/target/*.jar app.jar

# 设置环境变量
ENV SPRING_DATASOURCE_URL=jdbc:sqlite:/data/finance.db
ENV SERVER_PORT=8080

# 暴露端口
EXPOSE 8080

# 安装 wget 用于健康检查
RUN apk add --no-cache wget

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 运行应用
ENTRYPOINT ["java", "-jar", "app.jar"]

