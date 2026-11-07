#!/bin/bash

# 启动后端应用的脚本
# 此脚本会设置 PATH 环境变量，确保 Java 进程能找到 yt-dlp

# 添加 yt-dlp 的安装路径到 PATH
export PATH="/Library/Frameworks/Python.framework/Versions/3.11/bin:${PATH}"

# 检查 yt-dlp 是否可用
if ! command -v yt-dlp &> /dev/null; then
    echo "警告: yt-dlp 未找到，请确保已安装 yt-dlp"
    echo "安装命令: python3 -m pip install yt-dlp"
fi

# 进入项目目录
cd "$(dirname "$0")"

# 检查是否有编译好的 jar 文件
if [ -f "target/finance-0.0.1-SNAPSHOT.jar" ]; then
    echo "使用编译好的 jar 文件启动..."
    java -jar target/finance-0.0.1-SNAPSHOT.jar
elif [ -f "target/*.jar" ]; then
    echo "使用 jar 文件启动..."
    java -jar target/*.jar
else
    echo "未找到 jar 文件，使用 Maven 启动..."
    mvn spring-boot:run
fi


