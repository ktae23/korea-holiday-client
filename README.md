# 🇰🇷 KoreaHolidayClient

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
- Caffeine 기반의 캐싱 (기본 24시간)
- 최초 조회 시 조회를 시도한 작년, 올해, 내년의 공휴일을 모두 조회하여 캐싱

---

## 📦 설치 방법

GitHub Packages에서 의존성을 추가합니다.


```groovy
// settings.gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```


```kotlin
// build.gradle.kts
dependencies {
    implementation("com.github.ktae23:korea-holiday-client:${latestTag}")
}
```

## 💻 사용 예시

``` java
// API 키를 이용해 클라이언트 생성
KoreaHolidayClient client = new KoreaHolidayClient("YOUR_API_KEY")

// 특정 월의 휴일 목록 조회
List<LocalDate> holidays = client.getHolidaysInMonth(YearMonth.of(2025, 5))

// 날짜가 공휴일인지 확인
boolean isHoliday = client.isHoliday(LocalDate.of(2025, 5, 5))

// N일 후/전 영업일 계산
LocalDate nWorkingDayAfter  = client.afterNWorkingDays(LocalDate.of(2025, 5, 1), 3)
LocalDate nWorkingDayBefore  = client.beforeNWorkingDays(LocalDate.of(2025, 5, 1), 1)
```
