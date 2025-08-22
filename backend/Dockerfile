FROM openjdk:17-jdk-slim

LABEL maintainer="Enterprise Email System"
LABEL description="Enterprise Email System Backend"

# 设置工作目录
WORKDIR /app

# 复制Maven配置文件
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# 下载依赖（利用Docker缓存）
RUN ./mvnw dependency:go-offline

# 复制源代码
COPY src ./src

# 构建应用
RUN ./mvnw clean package -DskipTests

# 创建应用目录
RUN mkdir -p /app/logs /data/emails

# 暴露端口
EXPOSE 9000

# 设置JVM参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# 启动应用
CMD ["sh", "-c", "java $JAVA_OPTS -jar target/*.jar"]