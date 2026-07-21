package com.ktae23;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 한국천문연구원 특일 정보 API 기반 공휴일 조회 클라이언트.
 *
 * <p>공공데이터포털에서 발급한 서비스키가 필요하다.
 * <a href="https://www.data.go.kr/data/15012690/openapi.do">특일 정보 API</a>
 *
 * <pre>{@code
 * KoreaHolidayClient client = new KoreaHolidayClient(System.getenv("KOREA_HOLIDAY_API_KEY"));
 * boolean holiday = client.isHoliday(LocalDate.of(2026, 5, 1));
 * LocalDate payday = client.afterNWorkingDays(LocalDate.now(), 3);
 * }</pre>
 *
 * <p><b>서비스키</b>는 Encoding·Decoding 어느 쪽을 넣어도 된다.
 * 이미 URL 인코딩된 키(<code>%2B</code> 등 포함)는 그대로 쓰고,
 * 원본 키({@code +} {@code /} {@code =} 포함)는 자동으로 인코딩한다.
 *
 * <p>이 클래스는 스레드 안전하다.
 */
public class KoreaHolidayClient {

    private static final String API_HOST = "apis.data.go.kr";
    private static final String API_PATH = "/B090041/openapi/service/SpcdeInfoService/getHoliDeInfo";

    private static final DateTimeFormatter LOCDATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** %XX 형태가 있으면 이미 URL 인코딩된 키로 본다. */
    private static final Pattern PERCENT_ENCODED = Pattern.compile("%[0-9A-Fa-f]{2}");

    /** 국내 공휴일은 연 20~30건이라 100이면 충분하다. */
    private static final int NUM_OF_ROWS = 100;

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private final KoreaHolidayClientCache cache;
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private final String apiKey;
    private final boolean apiKeyPreEncoded;

    public KoreaHolidayClient(final String apiKey) {
        this(apiKey, defaultHttpClient(), new ObjectMapper(), new KoreaHolidayClientCache());
    }

    public KoreaHolidayClient(
            final String apiKey,
            final OkHttpClient okHttpClient,
            final ObjectMapper objectMapper,
            final KoreaHolidayClientCache cache
    ) {
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey must not be null");
        if (apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey must not be blank");
        }
        this.okHttpClient = Objects.requireNonNull(okHttpClient, "okHttpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.cache = Objects.requireNonNull(cache, "cache must not be null");
        this.apiKeyPreEncoded = PERCENT_ENCODED.matcher(apiKey).find();
    }

    private static OkHttpClient defaultHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_TIMEOUT)
                .readTimeout(DEFAULT_TIMEOUT)
                .writeTimeout(DEFAULT_TIMEOUT)
                .build();
    }

    // ────────────────────────── 조회 ──────────────────────────

    /** 해당 날짜가 공휴일인지. 주말 여부는 보지 않는다. */
    public boolean isHoliday(final LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        return holidaySetOf(date.getYear()).contains(date);
    }

    /** 토·일 여부. */
    public boolean isWeekend(final LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        final DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    /** 공휴일도 주말도 아닌 날. */
    public boolean isWorkingDay(final LocalDate date) {
        return !isWeekend(date) && !isHoliday(date);
    }

    /** {@code startDate} 다음 {@code n} 영업일. {@code n}이 0이면 {@code startDate} 그대로. */
    public LocalDate afterNWorkingDays(final LocalDate startDate, final int n) {
        return shiftWorkingDays(startDate, n, 1);
    }

    /** {@code startDate} 이전 {@code n} 영업일. */
    public LocalDate beforeNWorkingDays(final LocalDate startDate, final int n) {
        return shiftWorkingDays(startDate, n, -1);
    }

    private LocalDate shiftWorkingDays(final LocalDate startDate, final int n, final int step) {
        Objects.requireNonNull(startDate, "startDate must not be null");
        if (n < 0) {
            throw new IllegalArgumentException("n must not be negative: " + n);
        }
        LocalDate current = startDate;
        int moved = 0;
        while (moved < n) {
            current = current.plusDays(step);
            if (isWorkingDay(current)) {
                moved++;
            }
        }
        return current;
    }

    /** 해당 월의 공휴일. 날짜 오름차순. */
    public List<LocalDate> getHolidaysInMonth(final YearMonth yearMonth) {
        Objects.requireNonNull(yearMonth, "yearMonth must not be null");
        return holidaySetOf(yearMonth.getYear()).stream()
                .filter(holiday -> YearMonth.from(holiday).equals(yearMonth))
                .sorted()
                .toList();
    }

    /** 해당 연도의 공휴일. 날짜 오름차순. */
    public List<LocalDate> getHolidaysInYear(final int year) {
        return holidaySetOf(year).stream().sorted().toList();
    }

    /**
     * 캐시를 비운다. 임시공휴일이 새로 지정돼 즉시 반영이 필요할 때 호출한다.
     * 다음 조회에서 API를 다시 부른다.
     */
    public void refresh() {
        cache.invalidateAll();
    }

    // ────────────────────────── 내부 ──────────────────────────

    private Set<LocalDate> holidaySetOf(final int year) {
        final Cache<Integer, Set<LocalDate>> yearCache = cache.getYearCache();

        final Set<LocalDate> cached = yearCache.getIfPresent(year);
        if (cached != null) {
            return cached;
        }

        final Set<LocalDate> holidays = yearCache.get(year, this::fetchYear);

        // 연말·연초 영업일 계산이 연도 경계를 넘어가므로 인접 연도를 미리 채운다.
        // 인접 연도 조회 실패가 본 조회를 깨뜨리면 안 되므로 예외를 삼킨다.
        prefetchQuietly(yearCache, year - 1);
        prefetchQuietly(yearCache, year + 1);

        return holidays;
    }

    private void prefetchQuietly(final Cache<Integer, Set<LocalDate>> yearCache, final int year) {
        if (yearCache.getIfPresent(year) != null) {
            return;
        }
        try {
            yearCache.get(year, this::fetchYear);
        } catch (RuntimeException ignored) {
            // 미래 연도는 아직 API에 없을 수 있다. 본 조회에는 영향을 주지 않는다.
        }
    }

    private Set<LocalDate> fetchYear(final int year) {
        final HttpUrl.Builder url = new HttpUrl.Builder()
                .scheme("https")
                .host(API_HOST)
                .addPathSegments(API_PATH.substring(1))
                .addQueryParameter("solYear", String.valueOf(year))
                .addQueryParameter("_type", "json")
                .addQueryParameter("numOfRows", String.valueOf(NUM_OF_ROWS));

        // Encoding 키는 그대로, Decoding 키는 OkHttp가 인코딩하게 한다.
        if (apiKeyPreEncoded) {
            url.addEncodedQueryParameter("ServiceKey", apiKey);
        } else {
            url.addQueryParameter("ServiceKey", apiKey);
        }

        return fetch(url.build());
    }

    private Set<LocalDate> fetch(final HttpUrl url) {
        final Request request = new Request.Builder().url(url).get().build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new HolidayClientException(
                        "Failed to fetch holidays: HTTP " + response.code() + " " + response.message());
            }
            if (response.body() == null) {
                return Set.of();
            }

            final String json = response.body().string();
            if (json.isBlank()) {
                return Set.of();
            }

            final HolidayResponse parsed = objectMapper.readValue(json, HolidayResponse.class);
            final List<HolidayResponse.Item> items = itemsOf(parsed);
            if (items.isEmpty()) {
                return Set.of();
            }

            final Set<LocalDate> holidays = new LinkedHashSet<>(items.size());
            for (HolidayResponse.Item item : items) {
                if (item == null || item.localDate == null) {
                    continue;
                }
                // 특일 API는 공휴일 외 항목도 섞여 나올 수 있어 isHoliday 를 확인한다.
                if (item.isHoliday != null && !"Y".equalsIgnoreCase(item.isHoliday.trim())) {
                    continue;
                }
                holidays.add(LocalDate.parse(item.localDate.trim(), LOCDATE));
            }
            return holidays;
        } catch (HolidayClientException e) {
            throw e;
        } catch (Exception e) {
            throw new HolidayClientException("Error while calling holiday API", e);
        }
    }

    private static List<HolidayResponse.Item> itemsOf(final HolidayResponse parsed) {
        if (parsed == null || parsed.response == null || parsed.response.body == null
                || parsed.response.body.items == null || parsed.response.body.items.item == null) {
            return List.of();
        }
        return parsed.response.body.items.item;
    }
}
