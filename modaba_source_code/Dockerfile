# ========== Build Stage ==========
FROM gradle:8.5-jdk21-alpine AS builder

WORKDIR /app

# Gradle 래퍼 및 빌드 파일 복사
COPY server/gradlew .
COPY server/gradle gradle
COPY server/build.gradle.kts .
COPY server/settings.gradle.kts .

# 소스 코드 복사
COPY server/src src

# JAR 빌드
RUN chmod +x gradlew && \
    ./gradlew clean bootJar \
    --no-daemon \
    --no-configuration-cache \
    -Pprofiles.active=prod

# ========== Runtime Stage ==========
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 빌드 스테이지에서 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar


ENTRYPOINT ["java", "-jar", "app.jar"]
