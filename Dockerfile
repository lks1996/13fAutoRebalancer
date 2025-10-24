# 1단계: 프로젝트를 빌드하기 위한 빌더(Builder) 환경
# Eclipse Temurin의 JDK 17 이미지 기반
FROM eclipse-temurin:17-jdk-jammy as builder

# 작업 디렉토리 설정
WORKDIR /workspace/app

# Gradle Wrapper 파일들을 먼저 복사
COPY gradlew .
COPY gradle gradle

# build.gradle 파일을 복사
COPY build.gradle .

# (만약 settings.gradle 파일이 있다면)
# COPY settings.gradle .

# 의존성을 먼저 다운로드하여 캐싱 효과를 극대화
RUN ./gradlew dependencies

# 나머지 소스 코드를 복사
COPY src src

# Gradle을 사용하여 프로젝트를 빌드 (실행 가능한 JAR 생성)
RUN ./gradlew bootJar

# 2단계: 실제 애플리케이션을 실행하기 위한 최종 환경
# 더 가볍고 안전한 JRE(Java Runtime Environment) 이미지를 사용
FROM eclipse-temurin:17-jre-jammy

# 작업 디렉토리 설정
WORKDIR /app

# 빌더 환경에서 만들어진 JAR 파일을 최종 환경으로 복사
COPY --from=builder /workspace/app/build/libs/*.jar app.jar

# 컨테이너 내부에 /config 라는 빈 디렉토리 셍성. (application.yml 파일용)
VOLUME /config

# 컨테이너가 시작될 때 이 명령어를 실행
ENTRYPOINT ["java","-jar","/app/app.jar"]