# TMTA Backend

팀 일정 조율(Try Meet Time Again) 서비스를 위한 백엔드 API 서버입니다.  
Spring Boot 3.2, Java 21, JPA, Spring Security(JWT) 기반으로 구성되어 있습니다.

## 1. Tech Stack

- Java 21
- Spring Boot 3.2.0
- Spring Data JPA
- Spring Security + JWT (`jjwt`)
- MySQL 8 (runtime)
- H2 (test)
- Gradle
- Swagger/OpenAPI (`springdoc-openapi`)

## 2. 프로젝트 구조

```text
src/main/java/com/example/tmta
├── auth            # 회원가입/로그인/토큰 재발급/이메일 인증
├── controller      # Team, Appointment, Terms API
├── member          # 회원 프로필 설정 API
├── service         # 비즈니스 로직
├── repository      # JPA Repository
├── entity          # 도메인 엔티티
├── security        # JWT 필터, 시큐리티 설정
├── exception       # 공통 예외/에러코드/핸들러
└── config          # Swagger, 데이터 로더(dev)
```

## 3. 실행 방법

### 3.1 로컬 실행

1. MySQL 실행
2. DB 생성: `tmta`
3. `src/main/resources/application-dev.yml` 기준 DB 정보 확인
4. 서버 실행

```bash
./gradlew bootRun
```

기본 포트: `8080`

### 3.2 Docker Compose 실행

```bash
docker compose up -d --build
```

- App: `http://localhost:8080`
- DB: `localhost:3306`
- Compose는 `SPRING_PROFILES_ACTIVE=prod` 로 앱을 실행합니다.

## 4. 설정 값

### 4.1 프로파일

- 기본 프로파일: `dev` (`application.yml`)
- 운영 프로파일: `prod` (`application-prod.yml`)
- 테스트 프로파일: `test` (`src/test/resources/application-test.yml`)

### 4.2 주요 환경 변수 (prod)

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET_BASE64` (권장)

## 5. 인증/인가 개요

- Access Token: Authorization 헤더(`Bearer ...`)
- Refresh Token: HttpOnly 쿠키(`refresh_token`)
- 인증 없이 접근 가능한 API
- `/api/v1/auth/signup`
- `/api/v1/auth/login`
- `/api/v1/auth/refresh`
- `/api/v1/auth/email-verification/send`
- `/api/v1/auth/email-verification/confirm`
- `GET /api/v1/terms/**`
- `GET /v3/api-docs/**`, `GET /swagger-ui/**`

그 외 API는 인증 필요합니다.

## 6. 주요 API

### 6.1 Auth

- `POST /api/v1/auth/email-verification/send`
- `POST /api/v1/auth/email-verification/confirm`
- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`

### 6.2 Member

- `PATCH /api/v1/members/me/profile-setup`

### 6.3 Team

- `GET /api/v1/teams`
- `POST /api/v1/teams`
- `GET /api/v1/teams/{teamId}`
- `POST /api/v1/teams/{teamId}` (팀 가입)
- `PATCH /api/v1/teams/{teamId}/me/profile`
- `DELETE /api/v1/teams/{teamId}` (팀 탈퇴)
- `POST /api/v1/teams/{teamId}/member/{memberId}` (팀장 위임)
- `DELETE /api/v1/teams/{teamId}/member/{memberId}` (팀원 강퇴)

### 6.4 Appointment

- `POST /api/v1/teams/{teamId}/appointments`
- `GET /api/v1/teams/{teamId}/appointments/{appointmentId}`
- `GET /api/v1/teams/{teamId}/appointments/{appointmentId}/list`
- `GET /api/v1/teams/{teamId}/appointments/{appointmentId}/timetable`
- `POST /api/v1/teams/{teamId}/appointments/{appointmentId}/close`
- `POST /api/v1/teams/{teamId}/appointments/{appointmentId}`
- `POST /api/v1/teams/{teamId}/appointments/{appointmentId}/confirm`
- `PUT /api/v1/teams/{teamId}/appointments/{appointmentId}`
- `DELETE /api/v1/teams/{teamId}/appointments/{appointmentId}`

### 6.5 Terms

- `GET /api/v1/terms`
- `GET /api/v1/terms/{code}`

## 7. Swagger

- `http://localhost:8080/swagger-ui/index.html`

## 8. 테스트

```bash
./gradlew test
```

현재 기본 테스트는 Spring Context 로딩 테스트 중심입니다.

## 9. 개발 메모

- `dev` 프로파일에서 `DataLoader`가 샘플 데이터를 주입합니다.
- 이메일 인증/약관 저장소는 현재 인메모리/코드 하드코딩 기반 구현이 포함되어 있습니다.

## 10. 리스크 정리 (중요)

1. 이메일 인증코드가 고정값(`1111`)으로 구현되어 보안 취약점이 있습니다.
2. 이메일 인증코드 저장소가 인메모리여서 재시작/멀티 인스턴스 환경에서 인증 일관성이 깨질 수 있습니다.
3. Refresh 쿠키에 `secure=false`가 설정되어 HTTPS 운영 환경 보안에 취약합니다.
4. Access Token 만료시간이 과도하게 길어(약 100년) 토큰 탈취 시 피해가 장기화될 수 있습니다.
5. 약속 생성/수정 검증 정책이 불완전하여 `onlyDate=false`인데 시간값이 누락되어도 통과할 수 있습니다.
6. `availableTimeSlotCount` 필드의 명세(개수 제한)와 실제 로직(최소 시간슬롯 길이 필터) 의미가 불일치합니다.
7. 팀 이름 중복 에러코드는 존재하지만 팀 생성 시 중복 체크가 없어 중복 이름이 저장될 수 있습니다.
8. `application-dev.yml`에 로컬 DB 비밀번호가 하드코딩되어 비밀정보 관리 리스크가 있습니다.
9. `application.yml`의 JWT 시크릿 기본 fallback 값이 존재해 운영에서 오설정 시 취약해질 수 있습니다.
10. 운영 프로파일에서 `ddl-auto=update`를 사용해 스키마 변경 통제가 어려울 수 있습니다.
11. Swagger 설정에 운영 서버 IP가 하드코딩되어 인프라 정보 노출 위험이 있습니다.
12. CI/CD 워크플로 파일(`.github/workflows/deploy.yml`)이 사실상 비어 있어 자동 배포 체계가 부재합니다.

