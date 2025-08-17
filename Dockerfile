FROM gradle:jdk17 AS builder

WORKDIR /build

COPY . .

RUN ./gradlew build -x test --no-daemon


FROM openjdk:17-slim

WORKDIR /app

COPY --from=builder /build/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]