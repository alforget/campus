# ========== 阶段 1：构建阶段 ==========
FROM maven:3.8-openjdk-17 AS builder
WORKDIR /build

# 🚨 核心：给 Docker 里的 Maven 配置阿里云镜像，极大提升下载速度
RUN mkdir -p /usr/share/maven/ref \
  && echo "Configuring Maven Central Mirror..." \
  && echo '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd"> <mirrors> <mirror> <id>aliyunmaven</id> <mirrorOf>*</mirrorOf> <name>阿里云公共仓库</name> <url>https://maven.aliyun.com/repository/public</url> </mirror> </mirrors> </settings>' > /usr/share/maven/conf/settings.xml

# 先拷贝 pom.xml，利用 Docker 缓存下载依赖
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 再拷贝源码并打包（跳过测试以加快构建速度）
COPY src ./src
RUN mvn package -DskipTests -B

# ========== 阶段 2：运行阶段 ==========
FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache wget
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]