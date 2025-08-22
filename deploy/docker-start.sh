#!/bin/bash

# 快速启动脚本
# 用于Docker环境快速部署

set -e

echo "=========================================="
echo "  企业邮件系统 Docker 快速部署"
echo "=========================================="

# 检查Docker和Docker Compose
if ! command -v docker &> /dev/null; then
    echo "错误: Docker 未安装"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "错误: Docker Compose 未安装"
    exit 1
fi

# 停止并清理现有容器
echo "清理现有容器..."
docker-compose down -v

# 构建并启动服务
echo "构建并启动服务..."
docker-compose up --build -d

# 等待服务启动
echo "等待服务启动..."
sleep 30

# 检查服务状态
echo "检查服务状态..."
docker-compose ps

# 健康检查
echo "执行健康检查..."
if curl -f http://localhost:9000/api/health &> /dev/null; then
    echo "✅ 后端服务启动成功"
else
    echo "❌ 后端服务启动失败"
    docker-compose logs backend
    exit 1
fi

if curl -f http://localhost:8080 &> /dev/null; then
    echo "✅ 前端服务启动成功"
else
    echo "❌ 前端服务启动失败"
    docker-compose logs frontend
    exit 1
fi

echo ""
echo "🎉 部署成功！"
echo ""
echo "访问地址:"
echo "- 前端: http://localhost:8080"
echo "- 后端API: http://localhost:9000/api"
echo "- 健康检查: http://localhost:9000/api/health"
echo ""
echo "默认账户:"
echo "- 管理员: admin@system.com / admin123"
echo "- 演示用户: demo@example.com / admin123"
echo ""
echo "管理命令:"
echo "- 查看日志: docker-compose logs -f"
echo "- 停止服务: docker-compose down"
echo "- 重启服务: docker-compose restart"
echo ""