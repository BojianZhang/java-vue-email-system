# 构建阶段
FROM node:18-alpine as build-stage

LABEL maintainer="Enterprise Email System"
LABEL description="Enterprise Email System Frontend Build"

WORKDIR /app

# 复制package文件
COPY package*.json ./

# 安装依赖
RUN npm ci --only=production

# 复制源代码
COPY . .

# 构建应用
RUN npm run build

# 生产阶段
FROM nginx:alpine as production-stage

LABEL maintainer="Enterprise Email System"
LABEL description="Enterprise Email System Frontend"

# 复制构建结果
COPY --from=build-stage /app/dist /usr/share/nginx/html

# 复制Nginx配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 暴露端口
EXPOSE 80

# 启动Nginx
CMD ["nginx", "-g", "daemon off;"]