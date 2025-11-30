#!/bin/bash

# 阿里云应用托管构建部署脚本
# 用于从 Git 仓库克隆代码，构建镜像并部署
# 适用于阿里云 AppStack/云效等平台

set -e

# ==================== 配置变量 ====================
REPO_URL="https://github.com/yyb345/personalAsset.git"
BRANCH="main"
DOCKERFILE_PATH="Dockerfile"
DOCKER_NAME="finance-app"
APP_PORT="8080"           # 应用实际端口
HOST_PORT="8080"            # 宿主机映射端口（可根据需要修改）
DATA_DIR="/home/data/finance"  # 数据持久化目录

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# ==================== 辅助函数 ====================
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# ==================== 构建函数 ====================
build() {
    print_info "========== 开始构建 =========="
    
    # 复制仓库到指定目录
    target_dir="./code_deploy_application"
    
    if [ -d "$target_dir" ]; then
        print_info "清理旧代码目录..."
        rm -rf "$target_dir"
    fi
    
    print_info "克隆代码仓库: $REPO_URL (分支: $BRANCH)"
    git clone --branch "$BRANCH" "$REPO_URL" "$target_dir"
    
    cd "$target_dir"
    print_info "当前目录: $(pwd)"
    print_info "当前分支: $(git branch --show-current)"
    print_info "最新提交: $(git log -1 --oneline)"
    
    print_info "开始构建 Docker 镜像..."
    docker build -f "$DOCKERFILE_PATH" -t "$DOCKER_NAME:latest" . --progress=plain
    
    print_info "✓ 构建成功"
    
    # 返回上级目录
    cd ..
}

# ==================== 启动函数 ====================
start() {
    print_info "========== 开始启动服务 =========="
    
    # 创建数据目录
    if [ ! -d "$DATA_DIR" ]; then
        print_info "创建数据目录: $DATA_DIR"
        mkdir -p "$DATA_DIR"
    fi
    
    # 停止并删除旧容器（如果存在）
    if [ "$(docker ps -aq -f name=$DOCKER_NAME)" ]; then
        print_warn "发现已存在的容器，正在停止并删除..."
        docker stop "$DOCKER_NAME" 2>/dev/null || true
        docker rm "$DOCKER_NAME" 2>/dev/null || true
        print_info "旧容器已清理"
    fi
    
    # 启动新容器
    print_info "启动容器..."
    docker run \
        --name "$DOCKER_NAME" \
        -p "$HOST_PORT:$APP_PORT" \
        -v "$DATA_DIR:/data" \
        -e TZ=Asia/Shanghai \
        -e SERVER_PORT=$APP_PORT \
        -e SPRING_DATASOURCE_URL=jdbc:sqlite:/data/finance.db \
        -e JAVA_OPTS="-Xmx1g -Xms512m" \
        -d \
        --restart unless-stopped \
        "$DOCKER_NAME:latest"
    
    # 等待容器启动
    print_info "等待服务启动..."
    sleep 10
    
    # 检查容器状态
    if [ "$(docker ps -q -f name=$DOCKER_NAME)" ]; then
        print_info "✓ 容器启动成功"
    else
        print_error "✗ 容器启动失败"
        print_error "查看日志："
        docker logs "$DOCKER_NAME" 2>&1 | tail -50
        exit 1
    fi
}

# ==================== 健康检查 ====================
check_health() {
    print_info "========== 健康检查 =========="
    
    MAX_RETRIES=12
    RETRY_COUNT=0
    HEALTH_URL="http://localhost:$HOST_PORT/actuator/health"
    
    print_info "等待应用完全启动..."
    sleep 5
    
    while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
        if curl -f "$HEALTH_URL" > /dev/null 2>&1; then
            print_info "✓ 应用健康检查通过"
            return 0
        fi
        
        RETRY_COUNT=$((RETRY_COUNT + 1))
        print_warn "健康检查失败，重试 $RETRY_COUNT/$MAX_RETRIES..."
        sleep 5
    done
    
    print_error "✗ 应用健康检查失败"
    print_error "查看容器日志："
    docker logs "$DOCKER_NAME" 2>&1 | tail -100
    return 1
}

# ==================== 显示部署信息 ====================
show_info() {
    print_info "=========================================="
    print_info "           部署完成！                      "
    print_info "=========================================="
    print_info "应用名称: $DOCKER_NAME"
    print_info "访问地址: http://localhost:$HOST_PORT"
    print_info "健康检查: http://localhost:$HOST_PORT/actuator/health"
    print_info ""
    print_info "容器状态："
    docker ps -f name="$DOCKER_NAME" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    print_info ""
    print_info "常用命令："
    print_info "  查看日志: docker logs -f $DOCKER_NAME"
    print_info "  重启应用: docker restart $DOCKER_NAME"
    print_info "  停止应用: docker stop $DOCKER_NAME"
    print_info "  进入容器: docker exec -it $DOCKER_NAME sh"
    print_info "=========================================="
}

# ==================== 清理函数 ====================
cleanup() {
    print_info "清理构建缓存..."
    if [ -d "./code_deploy_application" ]; then
        rm -rf ./code_deploy_application
        print_info "✓ 临时文件已清理"
    fi
}

# ==================== 主执行流程 ====================
main() {
    print_info "=========================================="
    print_info "  阿里云部署脚本 - Finance App          "
    print_info "=========================================="
    
    # 检查 Docker 是否运行
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker 未运行，请先启动 Docker"
        exit 1
    fi
    
    # 执行构建
    build
    
    # 启动服务
    start
    
    # 健康检查
    if check_health; then
        show_info
        
        # 清理临时文件
        cleanup
        
        print_info "部署成功完成！✅"
        exit 0
    else
        print_error "部署失败，请检查日志 ❌"
        exit 1
    fi
}

# 捕获错误
trap 'print_error "部署过程中发生错误"; exit 1' ERR

# 执行主流程
main


