package com.ktae23;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 공공데이터포털 특일정보 API(getRestDeInfo)를 감싸 한국의 공휴일·주말·영업일 계산을 제공하는 클라이언트.
 *
 * <p>연/월별 공휴일 조회, 특정 날짜의 공휴일 여부 확인, N 영업일 전/후 계산을 지원하며,
 * 조회 결과는 Caffeine 캐시로 저장된다(기본 TTL 24시간). 최초 연도 조회 시 작년·올해·내년을 함께 프리캐싱한다.
 *
 * <p><b>스레드 안전성</b>: 인스턴스는 불변 필드와 스레드 세이프한 OkHttpClient/Caffeine 캐시로 구성되어
 * 여러 스레드에서 공유해도 안전하다. 애플리케이션당 하나의 인스턴스를 재사용하는 것을 권장한다.
 *
 * <p><b>API 키</b>: 공공데이터포털에서 발급받은 "특일정보" 서비스키(디코딩 키)를 사용한다.
 * 키는 절대 소스에 하드코딩하지 말고 환경변수/설정으로 주입한다.
 *
 * @since 1.1.0
 */
public class KoreaHolidayClient {

    /**
     * 기본 엔드포인트(공공데이터포털 특일정보 getRestDeInfo). 자체 게이트웨이를 쓰려면 base URL을
     * 지정하는 생성자를 사용한다. 게이트웨이는 동일한 {@code ?solYear=..&_type=json&ServiceKey=..&numOfRows=..}
     * 계약을 그대로 흉내내면 클라이언트를 그대로 재사용할 수 있다.
     */
    public static final String DEFAULT_API_URL =
            "http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo";

    private static final DateTimeFormatter LOCDATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final KoreaHolidayClientCache cache;

    private final ObjectMapper objectMapper;

    private final OkHttpClient okHttpClient;

    private final String apiKey;

    private final String yearQueryFormat;

    /**
     * 기본 설정(기본 OkHttpClient/ObjectMapper, TTL 24시간 캐시)으로 클라이언트를 생성한다.
     *
     * @param apiKey 공공데이터포털 특일정보 서비스키(디코딩 키). null/빈 문자열이면 예외
     * @throws HolidayClientException apiKey가 비어 있는 경우
     */
    public KoreaHolidayClient(final String apiKey) {
        this(apiKey, new KoreaHolidayClientCache());
    }

    /**
     * 캐시 TTL을 지정해 클라이언트를 생성한다.
     *
     * @param apiKey   공공데이터포털 특일정보 서비스키(디코딩 키). null/빈 문자열이면 예외
     * @param cacheTtl 연도별 조회 결과의 캐시 유지 기간
     * @throws HolidayClientException apiKey가 비어 있는 경우
     * @since 1.1.0
     */
    public KoreaHolidayClient(final String apiKey, final Duration cacheTtl) {
        this(apiKey, new KoreaHolidayClientCache(cacheTtl));
    }

    /**
     * 캐시 인스턴스를 직접 주입해 클라이언트를 생성한다.
     *
     * @param apiKey 공공데이터포털 특일정보 서비스키(디코딩 키). null/빈 문자열이면 예외
     * @param cache  연도별 공휴일 캐시
     * @throws HolidayClientException apiKey가 비어 있는 경우
     */
    public KoreaHolidayClient(final String apiKey, final KoreaHolidayClientCache cache) {
        this(apiKey, DEFAULT_API_URL, new OkHttpClient(), new ObjectMapper(), cache);
    }

    /**
     * 커스텀 엔드포인트(예: 자체 게이트웨이)를 지정해 클라이언트를 생성한다.
     *
     * <p>게이트웨이가 기본 엔드포인트와 동일한 쿼리 계약을 흉내내면 이 생성자로 base URL만 바꿔
     * 그대로 사용할 수 있다. {@code apiKey}는 이 경우 게이트웨이가 발급한 키가 된다.
     *
     * @param apiKey 게이트웨이(또는 공공데이터포털)에서 발급받은 키. null/빈 문자열이면 예외
     * @param apiUrl 호출할 엔드포인트 base URL(쿼리스트링 제외). null/빈 문자열이면 기본값
     * @throws HolidayClientException apiKey가 비어 있는 경우
     * @since 1.2.0
     */
    public KoreaHolidayClient(final String apiKey, final String apiUrl) {
        this(apiKey, apiUrl, new OkHttpClient(), new ObjectMapper(), new KoreaHolidayClientCache());
    }

    /**
     * 모든 협력 객체를 직접 주입한다(주로 테스트/고급 설정용). 엔드포인트는 기본값을 사용한다.
     *
     * @param apiKey       공공데이터포털 특일정보 서비스키(디코딩 키). null/빈 문자열이면 예외
     * @param okHttpClient HTTP 호출에 사용할 클라이언트
     * @param objectMapper JSON 파싱에 사용할 매퍼
     * @param cache        연도별 공휴일 캐시
     * @throws HolidayClientException apiKey가 비어 있는 경우
     */
    public KoreaHolidayClient(
            final String apiKey, final OkHttpClient okHttpClient, final ObjectMapper objectMapper,
            final KoreaHolidayClientCache cache
    ) {
        this(apiKey, DEFAULT_API_URL, okHttpClient, objectMapper, cache);
    }

    /**
     * 엔드포인트와 모든 협력 객체를 직접 주입하는 전체 생성자.
     *
     * @param apiKey       발급받은 키. null/빈 문자열이면 예외
     * @param apiUrl       호출할 엔드포인트 base URL(쿼리스트링 제외). null/빈 문자열이면 기본값
     * @param okHttpClient HTTP 호출에 사용할 클라이언트
     * @param objectMapper JSON 파싱에 사용할 매퍼
     * @param cache        연도별 공휴일 캐시
     * @throws HolidayClientException apiKey가 비어 있는 경우
     * @since 1.2.0
     */
    public KoreaHolidayClient(
            final String apiKey, final String apiUrl, final OkHttpClient okHttpClient,
            final ObjectMapper objectMapper, final KoreaHolidayClientCache cache
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new HolidayClientException("API 키가 필요합니다. 발급받은 서비스키를 주입하세요.");
        }
        final String baseUrl = (apiUrl == null || apiUrl.isBlank()) ? DEFAULT_API_URL : apiUrl;
        this.okHttpClient = okHttpClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.cache = cache;
        this.yearQueryFormat = baseUrl + "?solYear=%d&_type=json&ServiceKey=%s&numOfRows=100";
    }

    /**
     * 해당 날짜가 공휴일인지 여부를 반환한다(주말 여부는 포함하지 않는다).
     *
     * @param date 확인할 날짜
     * @return 공휴일이면 {@code true}
     */
    public boolean isHoliday(final LocalDate date) {
        final YearMonth yearMonth = YearMonth.from(date);
        final List<LocalDate> holidaysInMonth = getHolidaysInMonth(yearMonth);
        return holidaysInMonth.contains(date);
    }

    /**
     * 해당 날짜가 주말(토·일)인지 여부를 반환한다.
     *
     * @param date 확인할 날짜
     * @return 토요일 또는 일요일이면 {@code true}
     */
    public boolean isWeekend(final LocalDate date) {
        return date.getDayOfWeek().getValue() >= 6;
    }

    /**
     * 시작일로부터 N 영업일(주말·공휴일 제외) 이후의 날짜를 반환한다.
     *
     * @param startDate 기준 시작일(포함하지 않음)
     * @param n         더할 영업일 수(0 이상)
     * @return N 영업일 이후의 날짜
     */
    public LocalDate afterNWorkingDays(final LocalDate startDate, final int n) {
        LocalDate currentDate = startDate;
        int daysAdded = 0;

        while (daysAdded < n) {
            currentDate = currentDate.plusDays(1);
            if (!isHoliday(currentDate) && !isWeekend(currentDate)) {
                daysAdded++;
            }
        }
        return currentDate;
    }

    /**
     * 시작일로부터 N 영업일(주말·공휴일 제외) 이전의 날짜를 반환한다.
     *
     * @param startDate 기준 시작일(포함하지 않음)
     * @param n         뺄 영업일 수(0 이상)
     * @return N 영업일 이전의 날짜
     */
    public LocalDate beforeNWorkingDays(final LocalDate startDate, final int n) {
        LocalDate currentDate = startDate;
        int daysSubtracted = 0;

        while (daysSubtracted < n) {
            currentDate = currentDate.minusDays(1);
            if (!isHoliday(currentDate) && !isWeekend(currentDate)) {
                daysSubtracted++;
            }
        }
        return currentDate;
    }

    /**
     * 특정 연·월의 공휴일 목록을 반환한다.
     *
     * @param yearMonth 조회할 연·월
     * @return 해당 월의 공휴일 날짜 목록(없으면 빈 목록)
     */
    public List<LocalDate> getHolidaysInMonth(final YearMonth yearMonth) {
        return getHolidaysInYear(yearMonth.getYear()).stream()
                .filter(holiday -> YearMonth.of(holiday.getYear(), holiday.getMonthValue()).equals(yearMonth))
                .toList();
    }

    @NotNull
    private List<LocalDate> fetch(final String url) {
        final Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                // 주의: response.toString()에는 API 키가 포함된 URL이 들어가므로 절대 노출하지 않는다.
                throw new HolidayClientException(
                        "공휴일 조회에 실패했습니다. (HTTP " + response.code() + ")");
            }

            if (response.body() == null) {
                return List.of();
            }

            final String json = response.body().string();
            if (json.trim().isEmpty()) {
                return List.of();
            }
            final HolidayResponse holidayResponse = objectMapper.readValue(json, HolidayResponse.class);

            final List<LocalDate> holidays = new ArrayList<>();
            if (holidayResponse.response == null
                    || holidayResponse.response.body == null
                    || holidayResponse.response.body.items == null) {
                return holidays;
            }
            final List<HolidayResponse.Item> items = holidayResponse.response.body.items.item;

            if (items != null) {
                for (HolidayResponse.Item item : items) {
                    LocalDate date = LocalDate.parse(item.localDate, LOCDATE_FORMAT);
                    holidays.add(date);
                }
            }
            return holidays;
        } catch (HolidayClientException e) {
            throw e;
        } catch (Exception e) {
            // 주의: 예외 메시지/원인에 URL(=API 키)이 새지 않도록 원인 메시지는 전달하지 않는다.
            throw new HolidayClientException("공휴일 API 호출 중 오류가 발생했습니다. (" + e.getClass().getSimpleName() + ")");
        }
    }

    /**
     * 특정 연도의 공휴일 목록을 반환한다. 최초 호출 시 작년·올해·내년을 함께 캐싱한다.
     *
     * @param year 조회할 연도
     * @return 해당 연도의 공휴일 날짜 목록(없으면 빈 목록)
     * @throws HolidayClientException 네트워크/파싱/HTTP 오류 시(키는 메시지에 노출되지 않음)
     */
    public List<LocalDate> getHolidaysInYear(final int year) {
        final Cache<Integer, List<LocalDate>> yearCache = cache.getYearCache();

        final List<LocalDate> holidaysInYear = yearCache.get(year, ym -> fetch(String.format(yearQueryFormat, year, apiKey)));

        yearCache.get(year - 1, ym -> fetch(String.format(yearQueryFormat, year - 1, apiKey)));
        yearCache.get(year + 1, ym -> fetch(String.format(yearQueryFormat, year + 1, apiKey)));

        return holidaysInYear;
    }
}
