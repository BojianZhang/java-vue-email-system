#!/bin/bash

# 测试脚本
# 验证系统所有功能是否正常工作

set -e

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

BASE_URL="http://localhost:9000/api"
FRONTEND_URL="http://localhost:8080"

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# 测试后端健康检查
test_health_check() {
    log_info "测试后端健康检查..."
    
    response=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/health")
    if [ "$response" -eq 200 ]; then
        log_info "✅ 后端健康检查通过"
    else
        log_error "❌ 后端健康检查失败，HTTP状态码: $response"
        return 1
    fi
}

# 测试前端访问
test_frontend() {
    log_info "测试前端访问..."
    
    response=$(curl -s -o /dev/null -w "%{http_code}" "$FRONTEND_URL")
    if [ "$response" -eq 200 ]; then
        log_info "✅ 前端访问正常"
    else
        log_error "❌ 前端访问失败，HTTP状态码: $response"
        return 1
    fi
}

# 测试用户登录
test_login() {
    log_info "测试用户登录..."
    
    response=$(curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"email":"admin@system.com","password":"admin123"}')
    
    if echo "$response" | jq -e '.code == 200' > /dev/null 2>&1; then
        log_info "✅ 用户登录测试通过"
        
        # 提取token用于后续测试
        token=$(echo "$response" | jq -r '.data.accessToken')
        export AUTH_TOKEN="Bearer $token"
    else
        log_error "❌ 用户登录测试失败"
        echo "响应: $response"
        return 1
    fi
}

# 测试获取用户资料
test_get_profile() {
    log_info "测试获取用户资料..."
    
    if [ -z "$AUTH_TOKEN" ]; then
        log_error "❌ 未获取到认证令牌"
        return 1
    fi
    
    response=$(curl -s -H "Authorization: $AUTH_TOKEN" "$BASE_URL/users/profile")
    
    if echo "$response" | jq -e '.code == 200' > /dev/null 2>&1; then
        log_info "✅ 获取用户资料测试通过"
    else
        log_error "❌ 获取用户资料测试失败"
        echo "响应: $response"
        return 1
    fi
}

# 测试安全监控功能
test_security_monitoring() {
    log_info "测试安全监控功能..."
    
    if [ -z "$AUTH_TOKEN" ]; then
        log_error "❌ 未获取到认证令牌"
        return 1
    fi
    
    # 测试获取登录历史
    response=$(curl -s -H "Authorization: $AUTH_TOKEN" "$BASE_URL/security/login-monitoring?limit=10")
    
    if echo "$response" | jq -e '.code == 200' > /dev/null 2>&1; then
        log_info "✅ 安全监控功能测试通过"
    else
        log_error "❌ 安全监控功能测试失败"
        echo "响应: $response"
        return 1
    fi
}

# 测试数据库连接
test_database() {
    log_info "测试数据库连接..."
    
    if command -v mysql &> /dev/null; then
        result=$(mysql -u email_user -pemail_password_123 -e "SELECT COUNT(*) FROM enterprise_email.users;" 2>/dev/null || echo "error")
        
        if [ "$result" != "error" ]; then
            log_info "✅ 数据库连接测试通过"
        else
            log_error "❌ 数据库连接测试失败"
            return 1
        fi
    else
        log_warn "⚠️  MySQL客户端未安装，跳过数据库连接测试"
    fi
}

# 测试Redis连接
test_redis() {
    log_info "测试Redis连接..."
    
    if command -v redis-cli &> /dev/null; then
        result=$(redis-cli ping 2>/dev/null || echo "error")
        
        if [ "$result" = "PONG" ]; then
            log_info "✅ Redis连接测试通过"
        else
            log_error "❌ Redis连接测试失败"
            return 1
        fi
    else
        log_warn "⚠️  Redis客户端未安装，跳过Redis连接测试"
    fi
}

# 性能测试
test_performance() {
    log_info "执行性能测试..."
    
    # 测试并发登录
    log_info "测试并发登录性能..."
    
    for i in {1..5}; do
        curl -s -X POST "$BASE_URL/auth/login" \
            -H "Content-Type: application/json" \
            -d '{"email":"demo@example.com","password":"admin123"}' \
            > /dev/null &
    done
    
    wait
    log_info "✅ 并发登录测试完成"
    
    # 测试响应时间
    log_info "测试API响应时间..."
    
    response_time=$(curl -s -o /dev/null -w "%{time_total}" "$BASE_URL/health")
    
    if (( $(echo "$response_time < 2.0" | bc -l) )); then
        log_info "✅ API响应时间正常: ${response_time}s"
    else
        log_warn "⚠️  API响应时间较慢: ${response_time}s"
    fi
}

# 主测试函数
main() {
    echo "=========================================="
    echo "  企业邮件系统功能测试"
    echo "=========================================="
    
    # 检查依赖工具
    if ! command -v jq &> /dev/null; then
        log_error "需要安装 jq 工具来解析JSON响应"
        log_info "安装命令: sudo apt-get install jq"
        exit 1
    fi
    
    test_count=0
    passed_count=0
    
    # 执行所有测试
    tests=(
        "test_health_check"
        "test_frontend" 
        "test_database"
        "test_redis"
        "test_login"
        "test_get_profile"
        "test_security_monitoring"
        "test_performance"
    )
    
    for test in "${tests[@]}"; do
        echo ""
        ((test_count++))
        
        if $test; then
            ((passed_count++))
        fi
    done
    
    echo ""
    echo "=========================================="
    echo "  测试结果汇总"
    echo "=========================================="
    echo "总测试数: $test_count"
    echo "通过数量: $passed_count"
    echo "失败数量: $((test_count - passed_count))"
    
    if [ $passed_count -eq $test_count ]; then
        log_info "🎉 所有测试通过！系统运行正常"
        exit 0
    else
        log_error "❌ 有测试失败，请检查系统配置"
        exit 1
    fi
}

# 执行测试
main "$@"