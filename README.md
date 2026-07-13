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

Spring 프로파일은 `local`, `prod` 두 개만 사용합니다.

전체 로컬 환경을 Docker로 실행:

```shell
docker compose -f docker-compose.local.yml up --build
```

애플리케이션은 IDE/Gradle로 실행하고 MySQL과 Redis만 Docker로 실행:

```shell
docker compose -f docker-compose.local.yml up -d mysql redis
./gradlew bootRun --args='--spring.profiles.active=local'
```

## 테스트 실행

- `./gradlew test` 실행

## 디렉터리 구조

- `src/main/java`
- `src/main/resources`
- `src/test/java`
