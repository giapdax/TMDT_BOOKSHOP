# Sử dụng JDK 17 làm base image
FROM maven:3.9.9-amazoncorretto-17 AS build

# Đặt thư mục làm việc trong container
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY upload ./upload

RUN  mvn package -DskipTests





FROM amazoncorretto:17.0.12
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]
