# Changelog

이 프로젝트의 주요 변경 사항을 기록합니다. 형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.1.0/)를 따르며,
버전은 [Semantic Versioning](https://semver.org/lang/ko/)을 따릅니다.

## [1.1.0] - 2026-07-26

### Added
- Spring Boot 스타터 모듈(`korea-holiday-spring-boot-starter`) 추가 — `korea-holiday.api-key`, `korea-holiday.cache-ttl` 프로퍼티로 `KoreaHolidayClient` 빈 자동설정.
- 캐시 TTL을 지정하는 생성자 `KoreaHolidayClient(String apiKey, Duration cacheTtl)` 및 `KoreaHolidayClientCache(Duration ttl)` 추가.
- 공개 API 전반에 Javadoc 추가.
- API 키가 비어 있을 때 명확히 실패하도록 생성자 검증 추가.
- 예외 메시지에 API 키가 노출되지 않는지 검증하는 회귀 테스트 추가.

### Changed
- **배포처를 JitPack(`com.github.ktae23`)에서 Maven Central(`io.github.ktae23`)로 이전.** 그룹 ID 변경(하위호환 깨짐).
- 특일정보 엔드포인트를 `getHoliDeInfo`에서 `getRestDeInfo`로 변경 — 대체·임시공휴일을 포함한 전체 공휴일 반환.
- 기본 캐시 TTL을 12시간에서 24시간으로 변경(문서와 일치).
- 공개 API로 노출되는 의존성(okhttp, jackson, caffeine)을 `api` 스코프로 조정.

### Fixed
- **보안: 예외 메시지에 API 키가 포함된 요청 URL이 노출되던 문제 수정.** HTTP 실패 시 `Response.toString()`(요청 URL 포함)을 더 이상 출력하지 않는다.
- 응답 본문의 `response`/`body`/`items`가 없을 때 발생할 수 있는 NPE 방지.

### Removed
- 라이브러리에서 `Main` 진입점 제거(데모는 `examples` 모듈로 이동, API 키는 환경변수로 주입).
- 미사용 의존성(`cron-utils`) 및 라이브러리에 부적절한 `slf4j-simple` 런타임 바인딩 제거.
- JitPack 자동 태깅 릴리스 워크플로우 제거(→ GitHub Release 기반 Maven Central 배포로 대체).

### Security
- 과거 커밋 히스토리에 하드코딩되어 있던 공공데이터포털 서비스키를 `git filter-repo`로 전체 히스토리에서 제거하고 force-push.

## [1.0.x]

JitPack(`com.github.ktae23:korea-holiday-client`) 기반 초기 릴리스. 월/연 공휴일 조회, 공휴일 여부 확인,
N 영업일 전/후 계산, Caffeine 캐싱 기능 제공.

[1.1.0]: https://github.com/ktae23/korea-holiday-client/releases/tag/v1.1.0
