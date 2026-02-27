# TMTA Backend Architecture

이 문서는 `backend` 프로젝트의 현재 구현(코드 기준)을 빠르게 이해하기 위한 아키텍처 문서입니다.

## 1. 개요

TMTA 백엔드는 팀 단위 일정 조율 서비스를 위한 REST API 서버입니다.

- 런타임: Java 21, Spring Boot 3.2
- 저장소: MySQL (dev/prod), H2 (test)
- 인증: Spring Security + JWT (Access/Refresh)
- 보조 저장소: Redis (prod 이메일 인증 코드 저장)
- API 문서: springdoc-openapi (Swagger UI)

핵심 도메인:

- `auth`: 회원가입/로그인/토큰/이메일 인증/소셜 로그인
- `member`: 내 프로필 설정/조회
- `team`: 팀 생성/가입/팀 멤버십 관리
- `appointment`: 약속 생성/가능시간 등록/확정
- `terms`: 약관 목록/상세 조회

## 2. 패키지 구조 원칙 (Feature-First)

최상위는 기능 기준으로 분리하고, 기능 내부에 계층을 함께 둡니다.

```text
com.example.tmta
├── auth
├── member
├── team
├── appointment
├── terms
└── common
```

`common`에는 다음과 같은 횡단 관심사를 둡니다.

- `security`: JWT, 필터, SecurityConfig, 현재 사용자 조회
- `exception`: `BusinessException`, `ErrorCode`, 전역 예외 처리
- `config`: Swagger, 개발용 DataLoader 등
- `entity`: 공통 베이스 엔티티

## 3. 요청 처리 구조

일반적인 요청 흐름:

1. `Controller`가 HTTP 요청/응답 처리 및 DTO 바인딩
2. `Service`가 권한/검증/비즈니스 규칙 처리
3. `Repository`가 JPA 기반 데이터 접근
4. `QueryAssembler`가 조회 응답 DTO 조립 (팀/약속)
5. 예외는 `GlobalExceptionHandler`에서 `ErrorResponse`로 통일

특징:

- 명령/조회가 완전히 CQRS로 분리된 것은 아니지만, 조회 응답 조립은 `*QueryAssembler`로 분리되어 있음
- 도메인 서비스에서 `CurrentMemberProvider`를 통해 인증 주체를 공통 조회

## 4. 인증/인가 아키텍처

### 4.1 Security Filter Chain

`SecurityConfig` 기준:

- CSRF/폼로그인/httpBasic 비활성화
- 세션 stateless
- `JwtAuthenticationFilter`를 `UsernamePasswordAuthenticationFilter` 앞에 배치
- 일부 엔드포인트 permitAll:
  - 회원가입/로그인/리프레시
  - 이메일 인증/비밀번호 재설정 관련
  - `GET /api/v1/terms/**`
  - Swagger/OpenAPI

그 외 요청은 인증 필요.

### 4.2 JWT 처리

- `JwtTokenProvider`
  - Access Token / Refresh Token 생성
  - 검증 및 토큰에서 `UserPrincipal` 복원
  - 만료시각 추출 (RefreshToken 엔티티 저장용)
- `JwtAuthenticationFilter`
  - `Authorization: Bearer ...` 파싱
  - 유효하면 `SecurityContext`에 인증 주체 설정

### 4.3 현재 사용자 조회

`CurrentMemberProvider`

- `SecurityContext`의 `UserPrincipal` 확인
- DB에서 `Member` 재조회
- 없으면 `MEMBER_NOT_FOUND`, 인증 주체 없으면 `UNAUTHORIZED`

도메인 서비스(`TeamService`, `AppointmentService`)는 추가로 `profileSetupCompleted` 검사까지 수행하는 경우가 많음.

## 5. 인증 플로우 (API 흐름)

### 5.1 로컬 회원가입 플로우

1. `POST /api/v1/auth/email-verification/send`
2. `POST /api/v1/auth/email-verification/confirm`
   - 성공 시 회원가입용 verification token(JWT) 반환
3. `POST /api/v1/auth/signup`
   - 필수 약관 동의 검증
   - verification token의 subject(email) 검증
   - 이메일 중복 검사 후 회원 생성

구현 포인트:

- 이메일은 trim + lowercase 정규화
- 인증 코드는 랜덤 생성 후 해시 저장
- 재전송 쿨다운 / 시간창 기준 전송 횟수 제한 / 검증 시도 제한 있음

### 5.2 로그인/토큰 재발급 플로우

로그인:

1. `POST /api/v1/auth/login`
2. `AuthenticationManager`로 이메일/비밀번호 인증
3. Access Token + Refresh Token 발급
4. Refresh Token은 DB(`refresh_token`)에 저장/회전
5. Refresh Token을 HttpOnly 쿠키로 내려줌

재발급:

1. `POST /api/v1/auth/refresh`
2. 쿠키 `refresh_token` 추출
3. JWT 유효성 검사
4. DB 저장 토큰 일치 여부/만료 확인
5. Access/Refresh 재발급 및 DB 토큰 회전

### 5.3 소셜 로그인 플로우

`POST /api/v1/auth/social/login`

- `SocialVerifierRegistry`가 provider별 검증기 선택
- Google/Apple: JWKS 기반 ID Token 검증 지원 구조
- Kakao/Naver: UserInfo 기반 검증 구조
- provider + providerId로 기존 회원 조회
- 없으면 소셜 회원 신규 생성

## 6. 도메인 모델 아키텍처

### 6.1 Member / Team 관계

`Team`과 `Member`는 직접 ManyToMany 대신 `TeamMembers` 엔티티로 연결됩니다.

- 장점:
  - 팀 전용 닉네임/프로필 보관 가능
  - 팀 역할(ADMIN/GENERAL) 저장 가능
  - 팀 프로필 설정 완료 여부를 별도 관리 가능

### 6.2 Appointment 모델

약속(`Appointment`)은 다음 하위 엔티티를 가짐:

- `AppointmentDate`: 후보 날짜 목록
- `AppointmentSetting`: `onlyDate`, 마감일시
- `FinalTime`: 최종 확정 날짜/시간
- `AvailableTime`: 멤버별 가능시간 구간 (`AppointmentDate` 단위)
- `OptionalTime`, `Vote`: 확장용 투표 모델(현재 핵심 흐름에서는 사용 비중 낮음)

## 7. 텍스트 ERD (핵심)

아래는 코드 기준 핵심 엔티티 관계 요약입니다.

```text
Member
 - id (PK)
 - email (unique)
 - password
 - authProvider
 - providerId
 - nickName
 - profileImage
 - profileSetupCompleted
 - role

RefreshToken
 - id (PK)
 - member_id (FK, unique -> Member.id)   // 1:1
 - token (unique)
 - expires_at

Team
 - id (PK, UUID)
 - name
 - profileImage
 - postPermission
 - namePolicy

TeamMembers
 - id (PK)
 - team_id (UUID)
 - member_id
 - team_role
 - teamNickName
 - teamProfileImage
 - teamProfileSetupCompleted
 - UNIQUE(team_id, member_id)
 // Team <-> Member 연결 테이블 + 팀 전용 프로필/권한

Appointment
 - id (PK, UUID)
 - team_id
 - created_by_member_id
 - name
 - description
 - startTime
 - endTime
 - state

AppointmentDate
 - id (PK)
 - appointment_id (FK -> Appointment.id)
 - date
 - UNIQUE(appointment_id, date)

AppointmentSetting
 - id (PK)
 - appointment_id (FK, unique -> Appointment.id) // 1:1
 - onlyDate
 - deadlineDateTime

FinalTime
 - id (PK)
 - appointment_id (FK, unique -> Appointment.id) // 1:1
 - date
 - startTime
 - endTime

AvailableTime
 - id (PK)
 - member_id
 - appointment_date_id (FK -> AppointmentDate.id)
 - start_time
 - end_time
 - UNIQUE(member_id, appointment_date_id, start_time, end_time)

OptionalTime
 - id (PK)
 - appointment_id (FK -> Appointment.id)
 - date/startTime/endTime

Vote
 - id (PK)
 - member_id (FK -> Member.id)
 - optional_time_id (FK -> OptionalTime.id)
```

주의:

- `TeamMembers`, `Appointment` 등은 일부 관계를 엔티티 연관 대신 ID 필드(`teamId`, `memberId`)로 보관함
- FK 제약 유무는 DB 생성 전략/DDL에 따라 달라질 수 있으나, 애플리케이션 레벨 검증은 서비스에서 수행

## 8. 도메인별 핵심 규칙

### 8.1 Member

- 인증된 사용자만 `내 정보`, `프로필 설정` 가능
- `profileSetupCompleted`는 프로필 업데이트 시 서버에서 true로 관리

### 8.2 Team

- 대부분 API는 인증 + 회원 프로필 설정 완료 필요
- 팀 생성 시 생성자는 자동으로 `ADMIN`
- 팀 탈퇴:
  - 일반 멤버만 가능
  - 팀장(`ADMIN`)은 탈퇴 불가
- 팀장 위임/강퇴:
  - 현재 요청자가 팀장이어야 함

### 8.3 Appointment

- 대부분 API는 인증 + 회원 프로필 설정 완료 + 팀 멤버십 필요
- 약속 관리 권한:
  - 팀장 또는 약속 생성자
- 가능시간 등록:
  - 약속 상태가 `SCHEDULING` 또는 `CREATING`일 때만 가능
  - 등록 전 기존 내 가능시간 삭제 후 재등록(덮어쓰기 형태)
- 확정:
  - 후보 날짜 포함 여부 확인
  - `onlyDate=false`면 시작/종료시간 유효성 확인

## 9. 약속 가능시간 계산 구조

`AppointmentSlotCalculator` 역할:

1. `AvailableTime` 레코드를 `(date, start, end)` 단위로 그룹핑
2. 그룹별 참여 멤버 수/이름 집계
3. 필터 적용
   - 특정 사용자 포함
   - 최소 참여 인원
   - 최소 슬롯 길이(현재 `availableTimeSlotCount`가 이 의미로 동작)
4. 목록/타임테이블 응답 DTO 변환은 `AppointmentQueryAssembler`가 담당

시간 등록 시:

- 클라이언트가 30분 단위 슬롯 목록을 보내면
- 서버가 연속 구간으로 압축해 `AvailableTime`에 저장

예:

- 입력 슬롯: `10:00`, `10:30`, `11:30`
- 저장 구간:
  - `10:00 ~ 11:00`
  - `11:30 ~ 12:00`

## 10. 약속 상태 전이 (현재 구현 기준)

`AppointmentState` 전체 enum은 더 있을 수 있으나, 서비스 로직에서 주요하게 다루는 상태는 아래와 같습니다.

```text
생성(create)
  -> SCHEDULING

마감(closeScheduling)
  SCHEDULING or CREATING
  -> SCHEDULING_CLOSED

확정(confirm)
  (CONFIRMED/COMPLETE/CANCELED/EXPIRED 제외 상태에서 가능)
  -> CONFIRMED

추가로 enum 상 존재/사용 가능 상태:
  CREATING, COMPLETE, CANCELED, EXPIRED ...
```

구현상 주의점:

- 생성 시 실제 상태는 `SCHEDULING`으로 시작
- 일부 API 설명/테스트/샘플 데이터에는 `CREATING` 상태도 등장
- `deadlineDateTime` 기반 자동 상태 전이(스케줄러)는 현재 코드에서 보이지 않음

## 11. 설정/프로파일 운영 구조

### 11.1 프로파일

- `application.yml`: 공통 + 기본 profile=`dev`
- `application-dev.yml`: MySQL 로컬 개발 설정 + `DataLoader`
- `application-prod.yml`: env 기반 DB/Redis 설정
- `application-test.yml`: H2 in-memory

### 11.2 이메일 인증 저장소 전략

- `dev`, `test`: `InMemoryEmailVerificationStore`
- `prod`: `RedisEmailVerificationStore`

### 11.3 이메일 발송 전략

- 기본(SES disabled): `LoggingEmailVerificationSender`
- `prod + tmta.email.ses.enabled=true`: `SesEmailVerificationSender`

## 12. 테스트 전략 (현재 상태)

현재 테스트는 컨텍스트 로딩 외에도 서비스/정책/계산기 단위 테스트를 포함합니다.

중점 커버리지:

- 인증/리프레시 토큰 회전
- 이메일 인증 검증 정책
- 약속 생성/가능시간 등록/확정 권한/검증
- 약속 슬롯 압축/필터 계산
- 팀 생성/가입/권한 위임 규칙

## 13. 현재 아키텍처의 장점과 한계

장점:

- 기능 단위 구조가 명확하고 탐색성이 좋음
- 보안/예외 처리 일관성 확보
- 조회 조립(`QueryAssembler`) 분리로 서비스 복잡도 완화
- 이메일 인증/소셜 로그인 확장 포인트가 준비되어 있음

한계(개선 여지):

- 일부 입력 검증이 DTO가 아닌 서비스/정책에만 있고 누락 지점 존재
- 운영 보안 설정(JWT 만료, secure cookie 등) 강화 필요
- 문서상 필드 의미와 실제 로직이 일부 불일치
- `ddl-auto` 의존 대신 마이그레이션 도구(Flyway/Liquibase) 도입 필요
