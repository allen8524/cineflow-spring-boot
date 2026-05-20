# CineFlow Spring Boot

CineFlow는 Spring Boot와 Thymeleaf 기반의 영화 예매 사이트입니다. 영화 탐색, 빠른예매, 좌석 선택, 결제 흐름, 예매 내역 조회와 취소, 관리자 운영 화면을 하나의 웹 애플리케이션으로 구성했습니다.

## 주요 기능

- 영화 목록 및 상세 페이지
- TMDB 메타데이터 연동 및 로컬 seed/fallback 데이터 제공
- 빠른예매: 영화, 극장, 날짜, 상영시간 선택
- 좌석 선택 및 인원별 금액 계산
- 결제 처리 흐름과 예매 완료 화면
- 예매 내역 조회, 최근 티켓, 예매 취소
- 회원/비회원 예매 조회
- 관리자 페이지: 영화, 극장, 상영관, 상영일정, 예매 운영
- Flyway 기반 DB 마이그레이션

## 실행 환경

- Java 17
- Gradle Wrapper
- `dev`: H2 in-memory DB
- `local`: MySQL

기본 active profile은 `dev`입니다. MySQL로 실행할 때만 `local` 프로필을 명시합니다.

## dev 실행 방법

Windows PowerShell 기준:

```powershell
.\gradlew.bat bootRun
```

또는 명시적으로 `dev` 프로필을 지정할 수 있습니다.

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

dev 프로필은 H2 in-memory DB를 사용하며, Flyway 마이그레이션과 개발용 seed 데이터로 바로 시연할 수 있게 구성되어 있습니다.

## local 실행 방법

MySQL을 사용하는 로컬 실행은 `local` 프로필을 명시해야 합니다.

1. MySQL 데이터베이스 생성

```sql
CREATE DATABASE cineflow
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

동일한 SQL은 [docs/mysql-init.sql](docs/mysql-init.sql)에서도 확인할 수 있습니다.

2. MySQL 접속 정보 설정

`src/main/resources/application-local.yml`은 다음 환경변수를 사용합니다.

- `MYSQL_HOST` 기본값: `localhost`
- `MYSQL_PORT` 기본값: `3306`
- `MYSQL_DATABASE` 기본값: `cineflow`
- `MYSQL_USERNAME` 기본값: `root`
- `MYSQL_PASSWORD`: 로컬 MySQL 비밀번호

PowerShell 예시:

```powershell
$env:MYSQL_PASSWORD="your-local-mysql-password"
$env:SPRING_PROFILES_ACTIVE="local"
.\gradlew.bat bootRun
```

또는 실행 인자로 지정할 수 있습니다.

```powershell
$env:MYSQL_PASSWORD="your-local-mysql-password"
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

필요할 때만 seed 데이터를 켭니다.

```powershell
$env:APP_SEED_ENABLED="true"
```

## TMDB 설정

TMDB API를 사용하려면 bearer token을 환경변수로 설정합니다.

```powershell
$env:TMDB_BEARER_TOKEN="your-tmdb-bearer-token"
```

실제 토큰 값은 README, 설정 파일, 커밋 기록에 넣지 마세요. 토큰이 없더라도 로컬 seed/fallback 데이터로 주요 화면이 깨지지 않도록 설계되어 있습니다.

## 관리자 계정

dev 시연 환경에서는 seed 설정으로 관리자 계정을 생성할 수 있습니다. 계정 정보는 다음 환경변수로 지정할 수 있습니다.

- `ADMIN_USERNAME`
- `ADMIN_PASSWORD`
- `ADMIN_EMAIL`
- `ADMIN_NAME`
- `ADMIN_PHONE`

운영 또는 공개 환경에서는 반드시 `ADMIN_PASSWORD`를 안전한 값으로 바꿔서 실행하세요.

## 시연 URL

- `/`
- `/movies`
- `/booking`
- `/booking/history`
- `/admin`

## Flyway

- 마이그레이션 위치: `src/main/resources/db/migration`
- 현재 마이그레이션:
  - `V1__init_schema.sql`
  - `V2__indexes_and_constraints.sql`
- `local` 프로필은 Flyway로 스키마를 관리하고 JPA는 `validate`만 수행합니다.
- `dev` 프로필은 H2 기반 개발 편의를 위해 seed 데이터와 함께 실행됩니다.

## 보안 주의

- `TMDB_BEARER_TOKEN`은 환경변수로만 관리하고 커밋하지 않습니다.
- `.env`, `application-secret.yml`, `application-private.yml` 같은 개인 설정 파일은 커밋하지 않습니다.
- `ADMIN_PASSWORD`는 시연/운영 전에 반드시 변경하는 것을 권장합니다.
- 공개 저장소에 토큰이나 비밀번호가 올라간 적이 있다면 해당 secret은 재발급하세요.

## 테스트

```powershell
.\gradlew.bat test
```

GitHub Actions에서는 Ubuntu 환경에서 Java 17과 Gradle Wrapper로 `./gradlew test`를 실행합니다.
