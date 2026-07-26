# 🇰🇷 KoreaHolidayClient

[![CI](https://github.com/ktae23/korea-holiday-client/actions/workflows/ci.yml/badge.svg)](https://github.com/ktae23/korea-holiday-client/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.ktae23/korea-holiday-client.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.ktae23/korea-holiday-client)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)

공공데이터포털 특일정보 API를 활용해 공휴일, 주말, 임시공휴일 등을 간편하게 조회하고, 영업일 계산까지 할 수 있는 Java 라이브러리입니다.

---

## 📌 소개

공공데이터포털에서 제공하는 특일정보 API는 공휴일, 주말, 임시공휴일 등의 정보를 JSON 형식으로 제공합니다.
검색해보면 코드 예시만 있을 뿐, 실무에서 바로 사용할 수 있는 완성형 클라이언트는 찾기 어렵습니다.

“누가 처음부터 끝까지 만들어서 올려뒀으면...”
그 ‘누가’가 제가 되어 보기로 했습니다.

영업일 계산, 휴일 체크, 일정 조정 등의 다양한 작업에 바로 사용할 수 있도록 구성되었습니다.

---

## ✨ 주요 기능

- 월별 / 연도별 공휴일 조회
- 특정 날짜가 공휴일인지 여부 확인
- N 영업일 후 / 전 영업일 계산
- Caffeine 기반의 캐싱 (기본 24시간, TTL 설정 가능)
- 최초 조회 시 작년·올해·내년의 공휴일을 모두 조회하여 캐싱
- Spring Boot 자동설정 스타터 제공

---

## 🔑 API 키 발급

1. [공공데이터포털](https://www.data.go.kr)에 가입/로그인합니다.
2. **"특일정보"** (`한국천문연구원_특일 정보`) 오픈API 활용을 신청합니다.
3. 승인 후 마이페이지에서 **일반 인증키(디코딩)** 를 확인합니다. 이 값을 서비스키로 사용합니다.

> ⚠️ **API 키는 절대 소스 코드에 하드코딩하지 마세요.** 환경변수, 설정 파일, 시크릿 매니저 등으로 주입하고,
> 실수로 커밋되지 않도록 주의하세요. 이 라이브러리는 예외 메시지·로그에 키(및 키가 포함된 URL)를 출력하지 않습니다.

---

## 📦 설치 방법

[Maven Central](https://central.sonatype.com/artifact/io.github.ktae23/korea-holiday-client)에서 배포됩니다. (`io.github.ktae23`)

### 순수 클라이언트

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.ktae23:korea-holiday-client:1.1.0")
}
```

```groovy
// build.gradle
dependencies {
    implementation "io.github.ktae23:korea-holiday-client:1.1.0"
}
```

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.github.ktae23</groupId>
    <artifactId>korea-holiday-client</artifactId>
    <version>1.1.0</version>
</dependency>
```

### Spring Boot 스타터

```kotlin
dependencies {
    implementation("io.github.ktae23:korea-holiday-spring-boot-starter:1.1.0")
}
```

> **마이그레이션 안내**: 1.0.x 는 JitPack(`com.github.ktae23`)으로 배포되었습니다.
> 1.1.0부터 Maven Central의 `io.github.ktae23` 로 그룹 ID가 변경되었습니다.

---

## 💻 사용 예시

```java
// API 키는 환경변수 등에서 읽어 주입 (하드코딩 금지)
String apiKey = System.getenv("KOREA_HOLIDAY_API_KEY");
KoreaHolidayClient client = new KoreaHolidayClient(apiKey);

// 캐시 TTL을 직접 지정할 수도 있다
KoreaHolidayClient custom = new KoreaHolidayClient(apiKey, Duration.ofHours(6));

// 특정 월의 휴일 목록 조회
List<LocalDate> holidays = client.getHolidaysInMonth(YearMonth.of(2025, 5));

// 특정 연도의 휴일 목록 조회
List<LocalDate> yearly = client.getHolidaysInYear(2025);

// 날짜가 공휴일인지 확인
boolean isHoliday = client.isHoliday(LocalDate.of(2025, 5, 5));

// N 영업일 후/전 계산 (주말·공휴일 제외)
LocalDate after  = client.afterNWorkingDays(LocalDate.of(2025, 5, 1), 3);
LocalDate before = client.beforeNWorkingDays(LocalDate.of(2025, 5, 1), 1);
```

### Spring Boot에서 사용

`application.yml`:

```yaml
korea-holiday:
  api-key: ${KOREA_HOLIDAY_API_KEY}   # 필수 (환경변수/시크릿으로 주입)
  cache-ttl: 24h                      # 선택 (기본 24시간)
```

```java
@Service
public class ScheduleService {

    private final KoreaHolidayClient holidayClient; // 자동 주입

    public ScheduleService(KoreaHolidayClient holidayClient) {
        this.holidayClient = holidayClient;
    }

    public LocalDate nextSettlementDay() {
        return holidayClient.afterNWorkingDays(LocalDate.now(), 2);
    }
}
```

`korea-holiday.api-key` 가 설정되어 있으면 `KoreaHolidayClient` 빈이 자동 등록됩니다.
직접 `KoreaHolidayClient` 빈을 정의하면 그 빈이 우선합니다.

---

## 🧩 예제 실행

`examples` 모듈에서 동작을 확인할 수 있습니다. API 키는 환경변수로 주입합니다.

```bash
export KOREA_HOLIDAY_API_KEY="발급받은_디코딩_키"
./gradlew :examples:run --args="2026"
```

---

## 🚀 릴리스 / 배포

Maven Central 배포 절차와 필요한 설정은 [RELEASING.md](./RELEASING.md)를 참고하세요.

---

## 📝 License

This project is licensed under the [MIT License](https://opensource.org/licenses/MIT).
