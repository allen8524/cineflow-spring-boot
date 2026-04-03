# CineFlow Spring Boot Starter

프론트엔드 템플릿을 바탕으로 스프링부트 백엔드 시작 구조를 얹은 프로젝트입니다.

## 포함 내용
- Spring Boot 3.4.4 / Java 17 / Thymeleaf / JPA
- 기본 실행 프로필: H2 메모리 DB
- 추가 프로필: local (MySQL 연결용)
- 시작용 화면 연결
  - /
  - /movies
  - /movies/{id}
  - /booking
  - /booking/history
  - /booking/seat
  - /booking/payment
  - /booking/complete
  - /support
- 샘플 데이터 자동 주입
- JSON API 예시
  - /api/movies
  - /api/movies/{id}
  - /api/bookings/current
  - /api/bookings/past

## 실행 방법
### 1) 기본 실행(H2)
Gradle이 설치된 환경에서 아래 명령을 실행합니다.

```bash
./gradlew bootRun
```

Windows에서 Gradle Wrapper가 없다면 시스템에 설치된 Gradle로 실행하거나,
IDE에서 Gradle 프로젝트로 열어 실행하면 됩니다.

### 2) MySQL로 실행
`src/main/resources/application-local.yml`의 비밀번호를 수정한 뒤 아래처럼 실행합니다.

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

## 프로젝트 구조
- `src/main/java/com/cineflow` : 백엔드 코드
- `src/main/resources/templates` : 실행용 템플릿
- `src/main/resources/static` : CSS / JS / 이미지 / 폰트
- `original-source` : 업로드된 원본 프론트엔드 소스 전체 백업

## 참고
이 압축본은 “백엔드 연동 시작용” 기준으로 구성되어 있습니다.
현재는 영화 목록, 영화 상세, 예매내역 중심으로 연결되어 있고,
다음 단계로 상영관 / 상영시간표 / 좌석 / 결제 엔티티를 이어서 확장하면 됩니다.
