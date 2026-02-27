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

## 2. 프로젝트 구조 (Feature-First)

```text
src/main/java/com/example/tmta
├── appointment      # 약속 도메인 (controller/service/dto/entity/repository)
├── auth             # 인증 도메인 (controller/service/dto/entity/repository/verification)
├── member           # 회원 도메인 (controller/service/dto/entity/repository)
├── team             # 팀 도메인 (controller/service/dto/entity/repository)
├── terms            # 약관 도메인 (controller/service/dto)
└── common           # 공통 인프라/횡단관심사 (security/exception/config/common dto/entity)
```

### 2.2 추가 문서

- 아키텍처/API 흐름/ERD/상태 전이 문서: `docs/ARCHITECTURE.md`

### 2.1 구조 원칙

- 도메인 단위 패키지(`appointment`, `team`, `auth` ...)를 최상위에 둡니다.
- 각 도메인 내부에 필요한 계층(`dto`, `entity`, `repository`)을 함께 둡니다.
- 공통 요소만 `common`으로 분리합니다. (보안, 예외, 공통 설정 등)
- `TmtaApplication`은 루트 패키지에 유지해 컴포넌트 스캔 범위를 안전하게 보장합니다.

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

다음과 같은 단위 테스트가 포함되어 있습니다.

- `AuthServiceTest`
- `EmailVerificationServiceTest`
- `AppointmentServiceTest`
- `AppointmentCommandPolicyTest`
- `AppointmentSlotCalculatorTest`
- `TeamServiceTest`
- `EmailVerificationTokenProviderTest`
- `TmtaApplicationTests` (컨텍스트 로딩)

## 9. 개발 메모

- `dev` 프로파일에서 `DataLoader`가 샘플 데이터를 주입합니다.
- 이메일 인증 저장소는 `dev/test`에서 인메모리, `prod`에서 Redis 구현을 사용합니다.
- 이메일 인증 코드는 `SecureRandom` 기반으로 생성되며, 저장 시 해시(`PasswordEncoder`)로 저장됩니다.
- 이메일 발송은 기본적으로 로깅 구현을 사용하고, `prod + tmta.email.ses.enabled=true`에서 AWS SES 구현을 사용합니다.
- 약관은 현재 `TermsService` 내 인메모리(하드코딩) 문서로 제공됩니다.

## 10. 우선 수정 권장 이슈 (코드 변경 보류, 문서화)

아래 항목은 현재 코드 기준으로 확인된 우선 개선 필요 사항입니다. 이번 작업에서는 **코드 수정 없이 문서에만 정리**합니다.

1. `application.yml`의 Access Token 만료시간이 과도하게 길어(약 100년) 토큰 탈취 시 피해가 장기화될 수 있습니다.
2. `AuthController`의 Refresh 쿠키 설정이 `secure=false`라서 HTTPS 운영 환경 보안에 취약합니다.
3. `application.yml`의 JWT 시크릿 기본 fallback 값이 존재해 운영에서 환경변수 오설정 시 취약하게 동작할 수 있습니다.
4. `AppointmentCommandPolicy` 검증이 불완전하여 `onlyDate=false`인데 `startTime/endTime`이 누락되어도 생성/수정이 통과할 수 있습니다.
5. `AppointmentCandidateFilter.availableTimeSlotCount`의 문서 의미(개수 제한)와 실제 로직 의미(최소 연속 슬롯 길이 필터)가 불일치합니다.
6. `TeamService.createTeam()`에서 팀 이름 중복 검사 없이 저장되어 `TEAM_NAME_DUPLICATION` 에러코드가 실제로 사용되지 않습니다.
7. `AuthService.login()` 경로에서 로그인 이메일 정규화가 일관되지 않아(회원가입은 소문자 정규화) 대소문자 입력 차이로 로그인 실패 가능성이 있습니다.
8. `application-dev.yml`에 로컬 DB 비밀번호가 하드코딩되어 있습니다.
9. `application-prod.yml`에서 `spring.jpa.hibernate.ddl-auto=update`를 사용해 운영 스키마 변경 통제가 어렵습니다.
10. `SwaggerConfig`에 운영 서버 URL/IP가 하드코딩되어 있습니다.

## 11. 구현/문서 불일치 이력 (정리)

- 이메일 인증코드가 고정값(`1111`)이라는 과거 설명은 현재 구현과 다릅니다.
  - 현재 구현: 랜덤 코드 생성 + 해시 저장 + 발송/검증 정책(쿨다운/횟수 제한) 적용
- 테스트가 "Spring Context 중심"이라는 설명은 현재와 다릅니다.
  - 현재 구현: 서비스 단위 테스트가 여러 도메인에 추가됨
