# CineFlow Spring Boot

Spring Boot + Thymeleaf 기반 영화 예매 사이트입니다.  
기본 개발 모드는 H2 in-memory DB(`dev` 프로필)로 동작하고, 로컬 MySQL(`local` 프로필)도 함께 지원합니다.

## 프로필 구조

- `dev`
  - 기본 활성 프로필
  - H2 in-memory DB 사용
  - Flyway 마이그레이션 적용 후 JPA `ddl-auto=update`
  - 샘플 데이터 자동 초기화 활성화
- `local`
  - 로컬 MySQL 사용
  - Flyway 중심 스키마 관리
  - JPA `ddl-auto=validate`
  - 샘플 데이터는 기본 비활성화

## 1. dev(H2) 실행 방법

기본 실행:

```powershell
.\gradlew.bat bootRun
```

명시적으로 `dev` 프로필 실행:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

## 2. local(MySQL) 실행 방법

### 2-1. MySQL 데이터베이스 생성

아래 SQL을 먼저 실행합니다.

```sql
CREATE DATABASE cineflow
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

같은 내용은 [docs/mysql-init.sql](docs/mysql-init.sql)에도 있습니다.

### 2-2. 비밀번호 설정 위치

로컬 MySQL 프로필은 [application-local.yml](src/main/resources/application-local.yml)에서 아래 값으로 읽습니다.

- `MYSQL_HOST` 기본값: `localhost`
- `MYSQL_PORT` 기본값: `3306`
- `MYSQL_DATABASE` 기본값: `cineflow`
- `MYSQL_USERNAME` 기본값: `root`
- `MYSQL_PASSWORD` 기본값: `change-me`

가장 쉬운 방법은 환경변수로 비밀번호를 넣는 것입니다.

Windows PowerShell 예시:

```powershell
$env:MYSQL_PASSWORD="본인비밀번호"
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

또는 [application-local.yml](src/main/resources/application-local.yml)의 placeholder 기본값을 직접 수정해도 됩니다.

### 2-3. local 프로필 실행

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

샘플 데이터를 함께 넣고 싶다면:

```powershell
$env:MYSQL_PASSWORD="본인비밀번호"
$env:APP_SEED_ENABLED="true"
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

## Flyway 동작 방식

- 마이그레이션 위치: `src/main/resources/db/migration`
- 현재 포함된 파일:
  - `V1__init_schema.sql`
  - `V2__indexes_and_constraints.sql`
- `local` 프로필은 Flyway가 스키마를 먼저 만들고, JPA는 `validate`만 수행합니다.
- `dev` 프로필도 Flyway를 먼저 적용한 뒤 H2 기준 개발 편의를 위해 `ddl-auto=update`를 유지합니다.

## 처음 실행 순서

### dev(H2)

1. `.\gradlew.bat bootRun`
2. 브라우저에서 `/`, `/movies`, `/booking`, `/admin` 확인

### local(MySQL)

1. MySQL에서 `cineflow` 데이터베이스 생성
2. `MYSQL_PASSWORD` 설정 또는 `application-local.yml` 수정
3. `.\gradlew.bat bootRun --args="--spring.profiles.active=local"`
4. 필요 시 `APP_SEED_ENABLED=true`로 샘플 데이터 초기화

## 참고

- 기존 샘플 데이터 초기화는 `app.seed.enabled=true`일 때만 동작합니다.
- `local` 프로필은 기본적으로 빈 스키마만 만들고 샘플 데이터는 넣지 않습니다.
- 이미 JPA로 생성된 로컬 MySQL 스키마가 있다면, Flyway 기준으로 맞추기 위해 새 DB를 만들거나 기존 스키마를 정리한 뒤 실행하는 것을 권장합니다.
