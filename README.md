# Java Vue 企业级邮件系统

## 项目概述

企业级邮件收发系统，支持多域名和多别名，具备完整的安全监控功能。

### 技术栈

**后端技术栈：**
- Java 17
- Spring Boot 3.2
- Spring Security 6
- MyBatis Plus
- MySQL 8.0
- Redis (缓存和会话存储)
- Spring Mail (邮件服务)

**前端技术栈：**
- Vue 3
- Element Plus
- Pinia (状态管理)
- Vue Router 4
- Axios
- Vite

### 核心功能

#### 邮件系统功能
- ✅ 多域名邮件支持
- ✅ 用户多别名管理
- ✅ 邮件收发功能
- ✅ SMTP/IMAP集成
- ✅ 邮件附件处理

#### 安全监控功能  
- ✅ 用户登录IP监控
- ✅ 地理位置异常检测
- ✅ 多地同时登录检测
- ✅ 管理员异常通知
- ✅ 风险评分系统
- ✅ 设备指纹识别
- ✅ 活跃会话管理
- ✅ 可信设备管理

#### 管理功能
- ✅ 用户管理
- ✅ 域名管理  
- ✅ 安全监控仪表板
- ✅ 系统配置管理

## 项目结构

```
java-vue-email-system/
├── backend/                 # Spring Boot后端
│   ├── src/main/java/
│   │   └── com/enterprise/email/
│   │       ├── config/     # 配置类
│   │       ├── controller/ # 控制器
│   │       ├── entity/     # 实体类
│   │       ├── mapper/     # MyBatis映射
│   │       ├── service/    # 服务层
│   │       ├── security/   # 安全相关
│   │       └── utils/      # 工具类
│   ├── src/main/resources/
│   │   ├── mapper/         # MyBatis XML
│   │   └── application.yml # 配置文件
│   └── pom.xml
├── frontend/               # Vue.js前端
│   ├── src/
│   │   ├── components/     # 组件
│   │   ├── views/          # 页面
│   │   ├── stores/         # Pinia状态
│   │   ├── router/         # 路由配置
│   │   └── utils/          # 工具函数
│   ├── package.json
│   └── vite.config.js
├── database/               # 数据库脚本
├── deploy/                 # 部署脚本
├── docker-compose.yml      # Docker配置
└── README.md
```

## 快速开始

### 自动化部署
```bash
# 执行自动化部署脚本
chmod +x deploy/setup.sh
./deploy/setup.sh
```

### 手动部署
```bash
# 1. 数据库初始化
mysql -u root -p < database/init.sql

# 2. 后端启动
cd backend
mvn spring-boot:run

# 3. 前端启动
cd frontend
npm install
npm run dev
```

## 访问地址

- 前端界面: http://localhost:8080
- 后端API: http://localhost:9000
- 管理后台: http://localhost:8080/admin

## 默认账户

- 管理员: admin@system.com / admin123
- 测试用户: user@demo.com / user123