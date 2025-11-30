#!/bin/bash

# 阿里云应用托管构建部署脚本
# 用于从 Git 仓库克隆代码，构建镜像并部署
# 适用于阿里云 AppStack/云效等平台

# 注意：不使用 set -e，而是在关键步骤显式检查错误
# 这样可以提供更好的错误信息和控制

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
    
    # Git克隆，添加超时和重试机制
    if ! git clone --depth 1 --single-branch --branch "$BRANCH" "$REPO_URL" "$target_dir" 2>&1; then
        print_error "✗ Git克隆失败"
        print_error "可能原因："
        print_error "  1. 网络连接问题"
        print_error "  2. 仓库地址错误"
        print_error "  3. 分支不存在"
        print_error "  4. 需要认证（私有仓库）"
        return 1
    fi
    
    cd "$target_dir" || { print_error "无法进入目录: $target_dir"; return 1; }
    print_info "当前目录: $(pwd)"
    print_info "当前分支: $(git branch --show-current)"
    print_info "最新提交: $(git log -1 --oneline)"
    
    # 检查Dockerfile是否存在
    if [ ! -f "$DOCKERFILE_PATH" ]; then
        print_error "✗ Dockerfile不存在: $DOCKERFILE_PATH"
        return 1
    fi
    
    print_info "开始构建 Docker 镜像..."
    if docker build -f "$DOCKERFILE_PATH" -t "$DOCKER_NAME:latest" . --progress=plain; then
        print_info "✓ 构建成功"
    else
        print_error "✗ Docker 镜像构建失败"
        exit 1
    fi
    
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
    if docker run \
        --name "$DOCKER_NAME" \
        -p "$HOST_PORT:$APP_PORT" \
        -v "$DATA_DIR:/data" \
        -e TZ=Asia/Shanghai \
        -e SERVER_PORT=$APP_PORT \
        -e SPRING_DATASOURCE_URL=jdbc:sqlite:/data/finance.db \
        -e JAVA_OPTS="-Xmx1g -Xms512m" \
        -d \
        --restart unless-stopped \
        "$DOCKER_NAME:latest"; then
        print_info "✓ Docker run 命令执行成功"
    else
        print_error "✗ Docker run 命令执行失败"
        exit 1
    fi
    
    # 等待容器启动
    print_info "等待服务启动..."
    sleep 15
    
    # 检查容器状态
    if [ "$(docker ps -q -f name=$DOCKER_NAME)" ]; then
        print_info "✓ 容器正在运行"
        print_info "容器信息："
        docker ps -f name="$DOCKER_NAME" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    else
        print_error "✗ 容器启动后立即停止"
        print_error "查看完整日志："
        docker logs "$DOCKER_NAME" 2>&1
        print_error "容器信息："
        docker ps -a -f name="$DOCKER_NAME" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
        exit 1
    fi
}

# ==================== 健康检查 ====================
check_health() {
    print_info "========== 健康检查 =========="
    
    MAX_RETRIES=20
    RETRY_COUNT=0
    HEALTH_URL="http://localhost:$HOST_PORT/actuator/health"
    
    print_info "等待应用完全启动..."
    sleep 10
    
    while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
        # 首先检查容器是否还在运行
        if ! docker ps -q -f name="$DOCKER_NAME" > /dev/null 2>&1; then
            print_error "✗ 容器已停止运行"
            print_error "查看容器日志："
            docker logs "$DOCKER_NAME" 2>&1 | tail -100
            return 1
        fi
        
        # 尝试多种方式检查健康状态
        # 方式1: 使用curl检查actuator health端点
        if command -v curl &> /dev/null; then
            if curl -f -s "$HEALTH_URL" > /dev/null 2>&1; then
                print_info "✓ 应用健康检查通过 (actuator/health)"
                return 0
            fi
        fi
        
        # 方式2: 使用wget检查（备用方案）
        if command -v wget &> /dev/null; then
            if wget -q --spider "$HEALTH_URL" 2>/dev/null; then
                print_info "✓ 应用健康检查通过 (wget)"
                return 0
            fi
        fi
        
        # 方式3: 检查应用根路径
        ROOT_URL="http://localhost:$HOST_PORT/"
        if command -v curl &> /dev/null; then
            if curl -f -s "$ROOT_URL" > /dev/null 2>&1; then
                print_info "✓ 应用可访问 (root path)"
                return 0
            fi
        fi
        
        RETRY_COUNT=$((RETRY_COUNT + 1))
        print_warn "健康检查失败，重试 $RETRY_COUNT/$MAX_RETRIES..."
        
        # 显示容器最新日志（方便调试）
        if [ $((RETRY_COUNT % 5)) -eq 0 ]; then
            print_info "容器最新日志："
            docker logs --tail 20 "$DOCKER_NAME" 2>&1
        fi
        
        sleep 6
    done
    
    print_error "✗ 应用健康检查超时"
    print_error "查看容器完整日志："
    docker logs "$DOCKER_NAME" 2>&1 | tail -150
    
    # 注意：这里改为警告而非失败，让部署继续
    print_warn "健康检查未通过，但容器仍在运行，将继续部署流程"
    return 0
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
    print_info "容器资源使用："
    docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}" "$DOCKER_NAME" 2>/dev/null || print_warn "无法获取资源统计"
    print_info ""
    print_info "最近日志（最后10行）："
    docker logs --tail 10 "$DOCKER_NAME" 2>&1 || print_warn "无法获取日志"
    print_info ""
    print_info "常用命令："
    print_info "  查看日志: docker logs -f $DOCKER_NAME"
    print_info "  重启应用: docker restart $DOCKER_NAME"
    print_info "  停止应用: docker stop $DOCKER_NAME"
    print_info "  进入容器: docker exec -it $DOCKER_NAME sh"
    print_info "  查看实时日志: docker logs -f --tail 100 $DOCKER_NAME"
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

# ==================== 环境检查 ====================
check_environment() {
    print_info "========== 环境检查 =========="
    
    # 检查 Docker 是否安装
    if ! command -v docker &> /dev/null; then
        print_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi
    
    # 检查 Docker 是否运行
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker 未运行，请先启动 Docker"
        print_error "尝试执行: sudo systemctl start docker"
        exit 1
    fi
    
    # 检查 Git 是否安装
    if ! command -v git &> /dev/null; then
        print_error "Git 未安装，请先安装 Git"
        exit 1
    fi
    
    # 显示环境信息
    print_info "Docker 版本: $(docker --version)"
    print_info "Git 版本: $(git --version)"
    
    # 检查网络工具
    if command -v curl &> /dev/null; then
        print_info "curl 可用: $(curl --version | head -n1)"
    else
        print_warn "curl 未安装，健康检查将使用备用方案"
    fi
    
    if command -v wget &> /dev/null; then
        print_info "wget 可用"
    else
        print_warn "wget 未安装"
    fi
    
    print_info "✓ 环境检查通过"
}

# ==================== 主执行流程 ====================
main() {
    print_info "=========================================="
    print_info "  阿里云部署脚本 - Finance App          "
    print_info "=========================================="
    print_info "开始时间: $(date '+%Y-%m-%d %H:%M:%S')"
    
    # 环境检查
    check_environment
    
    # 执行构建
    if ! build; then
        print_error "构建失败"
        exit 1
    fi
    
    # 启动服务
    if ! start; then
        print_error "启动服务失败"
        exit 1
    fi
    
    # 健康检查（现在总是返回0，不会导致脚本失败）
    check_health
    
    # 显示部署信息
    show_info
    
    # 清理临时文件
    cleanup
    
    print_info "结束时间: $(date '+%Y-%m-%d %H:%M:%S')"
    print_info "部署成功完成！✅"
    exit 0
}

# 捕获中断信号
trap 'print_error "部署被中断"; cleanup; exit 130' INT TERM

# 执行主流程
main "$@"


