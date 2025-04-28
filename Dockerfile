# --- 빌드 단계 ---
# Gradle과 JDK 17을 포함한 이미지를 빌드 환경으로 사용
FROM gradle:8.5-jdk17 AS builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle 설정 파일 및 래퍼 스크립트 복사 (의존성 캐싱 최적화)
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle/

# 애플리케이션 소스 코드 복사
COPY src ./src

# Gradle을 사용하여 애플리케이션 빌드 (실행 가능한 Fat JAR 생성)
# --no-daemon 옵션은 CI/컨테이너 환경에서 권장됩니다.
RUN ./gradlew bootJar --no-daemon

# --- 실행 단계 ---
# 애플리케이션 실행을 위한 최소한의 JRE 환경 사용 (Java 17)
FROM eclipse-temurin:17-jre-jammy

# 작업 디렉토리 설정
WORKDIR /app

# 빌드 단계에서 생성된 실행 가능한 JAR 파일 복사
# build/libs/*.jar 패턴으로 생성된 fat JAR 파일을 app.jar로 복사합니다.
COPY --from=builder /app/build/libs/*.jar app.jar

# 애플리케이션이 사용할 포트 (Railway가 $PORT 환경 변수를 주입)
# Dockerfile에 EXPOSE를 명시하는 것은 좋은 습관이지만,
# 실제 포트는 아래 ENTRYPOINT에서 Spring Boot가 $PORT를 사용하도록 설정됩니다.
EXPOSE 8080

# 컨테이너 시작 시 애플리케이션 실행
# Spring Boot는 PORT 환경 변수를 자동으로 감지하여 서버 포트로 사용합니다.
ENTRYPOINT ["java", "-jar", "app.jar"]
