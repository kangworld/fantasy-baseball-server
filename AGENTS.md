# 프로젝트 에이전트 지침

이 저장소는 Java 17과 Spring Boot 3.5 기반의 fantasy baseball 서버다.

## 공통 지침 적용

- Java·Spring 백엔드 구현과 장애 조사에는 `spring-workbench:spring-backend-guidance`를 적용한다.
- 테스트 설계·실행, 회귀 검증, 코드 검토에는 `spring-workbench:spring-backend-verification`을 적용한다.
- 스킬이 안내하는 공통 기준과 이 프로젝트에 해당하는 기술 기준을 읽는다. 플러그인에 접근할 수 없으면 누락된 지침과 영향 범위를 알린다.
- 사용자 요청과 확정된 제품·API·DB 계약을 우선하고, 프로젝트 전용 규칙과 공통 기준의 충돌은 차이와 영향 범위를 확인한다.

## 프로젝트 기술과 검증

- 영속성은 Spring JDBC와 MySQL을 사용하며 스키마 변경은 Flyway 마이그레이션에서 관리한다. 기존 JDBC 영역의 접근 기술 변경은 별도 설계 결정으로 다룬다.
- Spring 프로파일은 `local`, `prod`를 사용한다. 운영 설정은 AWS Parameter Store에서 읽으며 Redis, SMTP, S3와 연동한다.
- 작업 시작 시 `.local/planning/`에서 관련 계획을 확인하고 현재 코드·테스트·계약과 대조한다. 외부 설정 변경은 `.local/planning/external-configuration/external-configuration-workflow.md`가 있으면 함께 확인한다.
- `./gradlew test`는 기본 테스트, `./gradlew integrationTest`는 Docker의 MySQL·Redis를 쓰는 Testcontainers 검증이며 `./gradlew check`는 둘을 함께 실행한다. Windows에서는 `./gradlew` 대신 `.\gradlew.bat`을 사용한다.
- `awsIntegrationTest`는 실제 AWS 리소스에 접근하는 별도 검증이다. 실행 전에 대상과 권한, 변경 범위를 확인한다.
- 비밀번호, 토큰, 개인 키와 웹훅 URL은 코드·문서·로그·명령 출력에 남기지 않는다.

## Git과 로컬 자료

- `master`를 기본 작업 브랜치로 사용한다. 프로젝트 지침은 루트 `AGENTS.md`에서 관리한다.
- `.local/`의 계획·검토·인수인계 자료와 로컬 도구 설정은 Git으로 추적하지 않는다.
- 기존 미커밋 변경은 사용자 작업으로 취급한다. 사용자 요청 없이 정리, stash, reset, stage, commit, 브랜치 변경, push, PR 또는 merge를 수행하지 않는다.
