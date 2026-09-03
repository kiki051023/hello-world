# 后端构建（M4-5）：Maven 编译 → 精简 JRE 运行
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# 必填：DB_PASSWORD / JWT_SECRET（启动守护会拒绝空值）
ENTRYPOINT ["java", "-jar", "app.jar"]
