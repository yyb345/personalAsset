#!/bin/bash

# 阿里云应用托管构建部署脚本
# 用于从 Git 仓库克隆代码，构建镜像并部署
# 支持 HTTPS (Nginx + Let's Encrypt)

# ==================== 配置变量 ====================
REPO_URL="https://github.com/yyb345/personalAsset.git"
BRANCH="main"
DOCKERFILE_PATH="Dockerfile"
DOCKER_NAME="finance-app"
NGINX_NAME="nginx-proxy"
CERTBOT_NAME="certbot"
ES_NAME="es-subtitle"
NETWORK_NAME="finance-network"
APP_PORT="80"
DATA_DIR="/home/data/finance"
CERT_DIR="/home/data/certbot"
NGINX_CONF_DIR="/home/data/nginx"
ES_DATA_DIR="/home/data/elasticsearch"
DOMAIN="www.xlearning.top"
EMAIL="yybsduhpc@gmail.com"  # 修改为你的邮箱

# ES 配置（设为 false 可禁用 ES）
ES_ENABLED="true"
ES_MEMORY="512m"

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

# ==================== 创建 Docker 网络 ====================
setup_network() {
    print_info "========== 设置 Docker 网络 =========="
    if ! docker network inspect "$NETWORK_NAME" > /dev/null 2>&1; then
        print_info "创建 Docker 网络: $NETWORK_NAME"
        docker network create "$NETWORK_NAME"
    else
        print_info "Docker 网络已存在: $NETWORK_NAME"
    fi
}

# ==================== 启动 Elasticsearch ====================
start_elasticsearch() {
    if [ "$ES_ENABLED" != "true" ]; then
        print_warn "Elasticsearch 已禁用，跳过启动"
        return 0
    fi

    print_info "========== 启动 Elasticsearch =========="

    # 创建数据目录
    if [ ! -d "$ES_DATA_DIR" ]; then
        print_info "创建 ES 数据目录: $ES_DATA_DIR"
        mkdir -p "$ES_DATA_DIR"
        chmod 777 "$ES_DATA_DIR"
    fi

    # 检查是否已在运行
    if docker ps -q -f name="$ES_NAME" | grep -q .; then
        print_info "Elasticsearch 已在运行"
        return 0
    fi

    # 清理旧容器
    print_info "清理旧的 ES 容器..."
    docker stop "$ES_NAME" 2>/dev/null || true
    docker rm -f "$ES_NAME" 2>/dev/null || true

    print_info "启动 Elasticsearch 容器..."
    if docker run \
        --name "$ES_NAME" \
        --network "$NETWORK_NAME" \
        -e "discovery.type=single-node" \
        -e "xpack.security.enabled=false" \
        -e "ES_JAVA_OPTS=-Xms${ES_MEMORY} -Xmx${ES_MEMORY}" \
        -e "cluster.name=subtitle-cluster" \
        -v "$ES_DATA_DIR:/usr/share/elasticsearch/data" \
        -d \
        --restart unless-stopped \
        elasticsearch:8.12.0; then
        print_info "✓ Elasticsearch 容器启动成功"
    else
        print_error "✗ Elasticsearch 容器启动失败"
        print_warn "将禁用 ES 功能继续部署..."
        ES_ENABLED="false"
        return 1
    fi

    # 等待 ES 就绪
    print_info "等待 Elasticsearch 就绪..."
    for i in {1..30}; do
        if docker exec "$ES_NAME" curl -s http://localhost:9200 > /dev/null 2>&1; then
            print_info "✓ Elasticsearch 已就绪"
            return 0
        fi
        sleep 2
        echo -n "."
    done

    print_warn "Elasticsearch 启动超时，但将继续部署"
    return 0
}

# ==================== 创建 Nginx 配置 ====================
setup_nginx_config() {
    print_info "========== 创建 Nginx 配置 =========="

    mkdir -p "$NGINX_CONF_DIR"
    mkdir -p "$CERT_DIR/conf"
    mkdir -p "$CERT_DIR/www"

    # 检查证书是否存在，决定使用哪个配置
    if [ -f "$CERT_DIR/conf/live/$DOMAIN/fullchain.pem" ]; then
        print_info "检测到 SSL 证书，使用 HTTPS 配置"
        create_https_config
    else
        print_info "未检测到 SSL 证书，使用 HTTP 配置（用于首次申请证书）"
        create_http_config
    fi
}

create_http_config() {
    cat > "$NGINX_CONF_DIR/nginx.conf" << 'NGINX_EOF'
events {
    worker_connections 1024;
}

http {
    server {
        listen 80;
        server_name www.xlearning.top xlearning.top;

        location /.well-known/acme-challenge/ {
            root /var/www/certbot;
        }

        location / {
            proxy_pass http://finance-app:80;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }
}
NGINX_EOF
}

create_https_config() {
    cat > "$NGINX_CONF_DIR/nginx.conf" << 'NGINX_EOF'
events {
    worker_connections 1024;
}

http {
    # HTTP - 重定向到 HTTPS
    server {
        listen 80;
        server_name www.xlearning.top xlearning.top;

        location /.well-known/acme-challenge/ {
            root /var/www/certbot;
        }

        location / {
            return 301 https://www.xlearning.top$request_uri;
        }
    }

    # HTTPS - 主站点
    server {
        listen 443 ssl;
        server_name www.xlearning.top;

        ssl_certificate /etc/letsencrypt/live/www.xlearning.top/fullchain.pem;
        ssl_certificate_key /etc/letsencrypt/live/www.xlearning.top/privkey.pem;

        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
        ssl_prefer_server_ciphers off;

        location / {
            proxy_pass http://finance-app:80;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }

    # HTTPS - 裸域名重定向
    server {
        listen 443 ssl;
        server_name xlearning.top;

        ssl_certificate /etc/letsencrypt/live/www.xlearning.top/fullchain.pem;
        ssl_certificate_key /etc/letsencrypt/live/www.xlearning.top/privkey.pem;

        return 301 https://www.xlearning.top$request_uri;
    }
}
NGINX_EOF
}

# ==================== 构建函数 ====================
build() {
    print_info "========== 开始构建 =========="

    target_dir="./code_deploy_application"

    if [ -d "$target_dir" ]; then
        print_info "清理旧代码目录..."
        rm -rf "$target_dir"
    fi

    print_info "克隆代码仓库: $REPO_URL (分支: $BRANCH)"

    if ! git clone --depth 1 --single-branch --branch "$BRANCH" "$REPO_URL" "$target_dir" 2>&1; then
        print_error "✗ Git克隆失败"
        return 1
    fi

    cd "$target_dir" || { print_error "无法进入目录: $target_dir"; return 1; }
    print_info "当前目录: $(pwd)"
    print_info "当前分支: $(git branch --show-current)"
    print_info "最新提交: $(git log -1 --oneline)"

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

    cd ..
}

# ==================== 启动应用容器 ====================
start_app() {
    print_info "========== 启动应用容器 =========="

    if [ ! -d "$DATA_DIR" ]; then
        print_info "创建数据目录: $DATA_DIR"
        mkdir -p "$DATA_DIR"
    fi

    # 强制停止并删除旧容器（兼容 Podman）
    print_info "清理旧的应用容器..."
    docker stop "$DOCKER_NAME" 2>/dev/null || true
    docker rm -f "$DOCKER_NAME" 2>/dev/null || true

    # 构建 ES 环境变量
    ES_ENV=""
    if [ "$ES_ENABLED" = "true" ]; then
        ES_ENV="-e ES_HOST=$ES_NAME -e ES_PORT=9200 -e ES_ENABLED=true"
        print_info "ES 已启用，连接到: $ES_NAME:9200"
    else
        ES_ENV="-e ES_ENABLED=false"
        print_info "ES 已禁用"
    fi

    print_info "启动应用容器..."
    if docker run \
        --name "$DOCKER_NAME" \
        --network "$NETWORK_NAME" \
        -v "$DATA_DIR:/data" \
        -e TZ=Asia/Shanghai \
        -e SERVER_PORT=$APP_PORT \
        -e SPRING_DATASOURCE_URL=jdbc:sqlite:/data/finance.db \
        -e JAVA_OPTS="-Xmx1g -Xms512m" \
        $ES_ENV \
        -d \
        --restart unless-stopped \
        "$DOCKER_NAME:latest"; then
        print_info "✓ 应用容器启动成功"
    else
        print_error "✗ 应用容器启动失败"
        exit 1
    fi
}

# ==================== 启动 Nginx 容器 ====================
start_nginx() {
    print_info "========== 启动 Nginx 容器 =========="

    # 强制停止并删除旧容器（兼容 Podman）
    print_info "清理旧的 Nginx 容器..."
    docker stop "$NGINX_NAME" 2>/dev/null || true
    docker rm -f "$NGINX_NAME" 2>/dev/null || true

    print_info "启动 Nginx 容器..."
    if docker run \
        --name "$NGINX_NAME" \
        --network "$NETWORK_NAME" \
        -p 80:80 \
        -p 443:443 \
        -v "$NGINX_CONF_DIR/nginx.conf:/etc/nginx/nginx.conf:ro" \
        -v "$CERT_DIR/conf:/etc/letsencrypt:ro" \
        -v "$CERT_DIR/www:/var/www/certbot:ro" \
        -d \
        --restart unless-stopped \
        nginx:alpine; then
        print_info "✓ Nginx 容器启动成功"
    else
        print_error "✗ Nginx 容器启动失败"
        exit 1
    fi
}

# ==================== 申请 SSL 证书 ====================
request_ssl_cert() {
    print_info "========== 申请 SSL 证书 =========="

    if [ -f "$CERT_DIR/conf/live/$DOMAIN/fullchain.pem" ]; then
        print_info "SSL 证书已存在，跳过申请"
        return 0
    fi

    print_info "等待 Nginx 启动..."
    sleep 5

    print_info "申请 Let's Encrypt 证书..."
    if docker run --rm \
        -v "$CERT_DIR/conf:/etc/letsencrypt" \
        -v "$CERT_DIR/www:/var/www/certbot" \
        certbot/certbot certonly \
        --webroot \
        -w /var/www/certbot \
        -d "$DOMAIN" \
        -d "xlearning.top" \
        --email "$EMAIL" \
        --agree-tos \
        --no-eff-email \
        --non-interactive; then
        print_info "✓ SSL 证书申请成功"

        # 更新 Nginx 配置为 HTTPS
        print_info "更新 Nginx 配置为 HTTPS..."
        create_https_config

        # 重启 Nginx 加载新配置
        docker restart "$NGINX_NAME"
        print_info "✓ Nginx 已重启，HTTPS 已启用"
    else
        print_error "✗ SSL 证书申请失败"
        print_warn "网站将继续以 HTTP 方式运行"
        print_warn "请确保域名已正确解析到服务器 IP"
    fi
}

# ==================== 设置证书自动续期 ====================
setup_cert_renewal() {
    print_info "========== 设置证书自动续期 =========="

    # 创建续期脚本
    cat > "$CERT_DIR/renew.sh" << 'RENEW_EOF'
#!/bin/bash
docker run --rm \
    -v /home/data/certbot/conf:/etc/letsencrypt \
    -v /home/data/certbot/www:/var/www/certbot \
    certbot/certbot renew --quiet

docker exec nginx-proxy nginx -s reload
RENEW_EOF

    chmod +x "$CERT_DIR/renew.sh"

    # 添加 crontab（每天凌晨 3 点检查续期）
    CRON_JOB="0 3 * * * $CERT_DIR/renew.sh >> /var/log/certbot-renew.log 2>&1"

    if ! crontab -l 2>/dev/null | grep -q "certbot renew"; then
        (crontab -l 2>/dev/null; echo "$CRON_JOB") | crontab -
        print_info "✓ 已添加证书自动续期定时任务"
    else
        print_info "证书续期定时任务已存在"
    fi
}

# ==================== 健康检查 ====================
check_health() {
    print_info "========== 健康检查 =========="

    MAX_RETRIES=20
    RETRY_COUNT=0
    HEALTH_URL="http://localhost/actuator/health"

    print_info "等待应用完全启动..."
    sleep 10

    while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
        if ! docker ps -q -f name="$DOCKER_NAME" > /dev/null 2>&1; then
            print_error "✗ 应用容器已停止运行"
            docker logs "$DOCKER_NAME" 2>&1 | tail -100
            return 1
        fi

        if command -v curl &> /dev/null; then
            if curl -f -s "$HEALTH_URL" > /dev/null 2>&1; then
                print_info "✓ 应用健康检查通过"
                return 0
            fi
        fi

        RETRY_COUNT=$((RETRY_COUNT + 1))
        print_warn "健康检查失败，重试 $RETRY_COUNT/$MAX_RETRIES..."
        sleep 6
    done

    print_warn "健康检查未通过，但容器仍在运行，将继续部署流程"
    return 0
}

# ==================== 显示部署信息 ====================
show_info() {
    print_info "=========================================="
    print_info "           部署完成！                      "
    print_info "=========================================="
    print_info "应用名称: $DOCKER_NAME"
    print_info "访问地址: https://$DOMAIN"
    print_info "健康检查: https://$DOMAIN/actuator/health"
    if [ "$ES_ENABLED" = "true" ]; then
        print_info "ES 状态:  已启用 (容器: $ES_NAME)"
        print_info "搜索接口: https://$DOMAIN/api/subtitle-search?q=关键词"
    else
        print_info "ES 状态:  已禁用"
    fi
    print_info ""
    print_info "容器状态："
    docker ps -f name="$DOCKER_NAME" -f name="$NGINX_NAME" -f name="$ES_NAME" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    print_info ""
    print_info "常用命令："
    print_info "  查看应用日志: docker logs -f $DOCKER_NAME"
    print_info "  查看Nginx日志: docker logs -f $NGINX_NAME"
    print_info "  查看ES日志: docker logs -f $ES_NAME"
    print_info "  重启应用: docker restart $DOCKER_NAME"
    print_info "  重启Nginx: docker restart $NGINX_NAME"
    print_info "  重启ES: docker restart $ES_NAME"
    print_info "  手动续期证书: $CERT_DIR/renew.sh"
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

    if ! command -v docker &> /dev/null; then
        print_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi

    if ! docker info > /dev/null 2>&1; then
        print_error "Docker 未运行，请先启动 Docker"
        exit 1
    fi

    if ! command -v git &> /dev/null; then
        print_error "Git 未安装，请先安装 Git"
        exit 1
    fi

    print_info "Docker 版本: $(docker --version)"
    print_info "Git 版本: $(git --version)"
    print_info "✓ 环境检查通过"
}

# ==================== 主执行流程 ====================
main() {
    print_info "=========================================="
    print_info "  阿里云部署脚本 - Finance App (HTTPS)   "
    print_info "=========================================="
    print_info "开始时间: $(date '+%Y-%m-%d %H:%M:%S')"

    check_environment
    setup_network

    if ! build; then
        print_error "构建失败"
        exit 1
    fi

    setup_nginx_config

    # 先启动 ES（如果启用）
    start_elasticsearch

    # 再启动应用
    start_app
    start_nginx

    # 申请 SSL 证书
    request_ssl_cert
    setup_cert_renewal

    check_health
    show_info
    cleanup

    print_info "结束时间: $(date '+%Y-%m-%d %H:%M:%S')"
    print_info "部署成功完成！✅"
    exit 0
}

trap 'print_error "部署被中断"; cleanup; exit 130' INT TERM

main "$@"
