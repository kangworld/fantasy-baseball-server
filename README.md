# fantasy-baseball-server

## 개요

판타지 야구 백엔드 서버입니다.

## 개발 환경

- Java 17
- Docker
- Docker Compose

## 기술 스택

- Spring Boot
- MySQL
- Redis
- Caffeine
- JdbcTemplate
- JUnit 5
- AssertJ
- H2 Database

## 실행 방법

1. `docker-compose -f docker-compose.local.yml up -d` 실행
2. `./gradlew bootRun --args='--spring.profiles.active=local'` 실행
3. `GET /api/health` 호출로 상태 확인

## 테스트 실행

- `./gradlew test` 실행

## 디렉터리 구조

- `src/main/java`
- `src/main/resources`
- `src/test/java`
