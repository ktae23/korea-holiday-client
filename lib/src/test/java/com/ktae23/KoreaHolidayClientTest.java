package com.ktae23;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KoreaHolidayClientTest {

    private static final String CALL_ERROR_MESSAGE = "공휴일 API 호출 중 오류가 발생했습니다";

    private static final String HTTP_ERROR_MESSAGE = "공휴일 조회에 실패했습니다";

    private OkHttpClient mockHttpClient;

    private ObjectMapper objectMapper;

    private KoreaHolidayClient client;

    private KoreaHolidayClientCache cache;

    private String apiKey = "dummy-api-key";

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(OkHttpClient.class);
        objectMapper = new ObjectMapper();
        cache = new KoreaHolidayClientCache();
        client = new KoreaHolidayClient(apiKey, mockHttpClient, objectMapper, cache);
    }

    @Test
    void testIsHolidayTrue() throws IOException {
        String json = """
                      {
                        "response": {
                          "body": {
                            "items": {
                              "item": [
                                {
                                  "locdate": 20230101,
                                  "dateName": "신정",
                                  "isHoliday": "Y"
                                }
                              ]
                            }
                          }
                        }
                      }
                      """;

        mockHttpResponse(json);
        boolean result = client.isHoliday(LocalDate.of(2023, 1, 1));
        assertTrue(result);
    }

    @Test
    void testIsHolidayFalse() throws IOException {
        String json = """
                      {
                        "response": {
                          "body": {
                            "items": {
                              "item": [
                                {
                                  "locdate": 20230101,
                                  "dateName": "신정",
                                  "isHoliday": "Y"
                                }
                              ]
                            }
                          }
                        }
                      }
                      """;

        mockHttpResponse(json);
        boolean result = client.isHoliday(LocalDate.of(2023, 1, 2));
        assertFalse(result);
    }

    @Test
    void testIsWeekendTrue() {
        LocalDate saturday = LocalDate.of(2023, 4, 15);
        LocalDate sunday = LocalDate.of(2023, 4, 16);
        assertTrue(client.isWeekend(saturday));
        assertTrue(client.isWeekend(sunday));
    }

    @Test
    void testIsWeekendFalse() {
        LocalDate weekday = LocalDate.of(2023, 4, 17); // Monday
        assertFalse(client.isWeekend(weekday));
    }

    @Test
    void testAfterNWorkingDays() throws IOException {
        String json = """
                      {
                        "response": {
                          "body": {
                            "items": {
                              "item": [
                                { "locdate": 20250409, "dateName": "임시공휴일", "isHoliday": "Y" }
                              ]
                            }
                          }
                        }
                      }
                      """;

        mockHttpResponse(json);
        LocalDate result = client.afterNWorkingDays(LocalDate.of(2025, 4, 4), 2);
        assertEquals(LocalDate.of(2025, 4, 8), result);
    }

    @Test
    void testBeforeNWorkingDays() throws IOException {
        String json = """
                      {
                        "response": {
                          "body": {
                            "items": {
                              "item": [
                                { "locdate": 20250409, "dateName": "임시공휴일", "isHoliday": "Y" }
                              ]
                            }
                          }
                        }
                      }
                      """;

        mockHttpResponse(json);
        LocalDate result = client.beforeNWorkingDays(LocalDate.of(2025, 4, 10), 2);
        assertEquals(LocalDate.of(2025, 4, 7), result);
    }

    @Test
    void testGetHolidaysInMonth_CacheMiss() throws IOException {
        String json = """
                      {
                        "response": {
                          "body": {
                            "items": {
                              "item": [
                                { "locdate": 20230405, "dateName": "식목일", "isHoliday": "Y" },
                                { "locdate": 20230410, "dateName": "임시공휴일", "isHoliday": "Y" }
                              ]
                            }
                          }
                        }
                      }
                      """;

        mockHttpResponse(json); // fetch 호출 시의 응답 mocking
        List<LocalDate> holidays = client.getHolidaysInMonth(YearMonth.of(2023, 4));

        assertEquals(2, holidays.size());
        assertTrue(holidays.contains(LocalDate.of(2023, 4, 5)));
        assertTrue(holidays.contains(LocalDate.of(2023, 4, 10)));
    }

    @Test
    void testGetHolidaysInMonth_CacheHit() {
        getYearCache(2022);
        getYearCache(2024);
        final List<LocalDate> cachedHolidays = getYearCache(2023).stream()
                .filter(
                        holiday -> YearMonth.of(holiday.getYear(), holiday.getMonthValue()).equals(YearMonth.of(2023, 1))
                ).toList();

        List<LocalDate> holidays = client.getHolidaysInMonth(YearMonth.of(2023, 1));

        assertEquals(1, holidays.size());
        assertEquals(cachedHolidays, holidays);
    }

    @Test
    void testGetHolidaysInYear_CacheMiss() throws IOException {
        String json = """
                      {
                        "response": {
                          "body": {
                            "items": {
                              "item": [
                                { "locdate": 20230101, "dateName": "신정", "isHoliday": "Y" },
                                { "locdate": 20230928, "dateName": "추석", "isHoliday": "Y" }
                              ]
                            }
                          }
                        }
                      }
                      """;

        mockHttpResponse(json);
        List<LocalDate> holidays = client.getHolidaysInYear(2023);

        assertEquals(2, holidays.size());
        assertTrue(holidays.contains(LocalDate.of(2023, 1, 1)));
        assertTrue(holidays.contains(LocalDate.of(2023, 9, 28)));
    }

    @Test
    void testGetHolidaysInYear_CacheHit() {
        getYearCache(2022);
        getYearCache(2024);
        List<LocalDate> cachedHolidays = getYearCache(2023);

        List<LocalDate> holidays = client.getHolidaysInYear(2023);

        assertEquals(2, holidays.size());
        assertEquals(cachedHolidays, holidays);
    }

    @NotNull
    private List<LocalDate> getYearCache(final int year) {
        List<LocalDate> cachedHolidays = List.of(
                LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 25)
        );

        cache.getYearCache().put(year, cachedHolidays);
        return cachedHolidays;
    }

    @Test
    void testFetch_throwsException_whenHttpFails() throws IOException {
        mockHttpResponseWithStatus(500, "Internal Server Error");

        HolidayClientException exception = assertThrows(HolidayClientException.class, () -> {
            client.getHolidaysInMonth(YearMonth.of(2024, 4));
        });

        assertTrue(exception.getMessage().contains(HTTP_ERROR_MESSAGE));
    }

    @Test
    void testFetch_throwsException_whenBodyIsNull() throws IOException {
        mockHttpResponseWithNullBody();

        // 본문이 없는 비정상 응답은 HolidayClientException으로 감싸 던진다.
        assertThrows(
                HolidayClientException.class,
                () -> client.getHolidaysInMonth(YearMonth.of(2024, 5))
        );
    }

    @Test
    void testFetch_returnsEmptyList_whenBodyIsEmptyString() throws IOException {
        mockHttpResponse("");

        List<LocalDate> holidays = client.getHolidaysInMonth(YearMonth.of(2024, 6));
        assertTrue(holidays.isEmpty());
    }

    @Test
    void testFetch_throwsException_whenJsonParsingFails() throws IOException {
        String invalidJson = "{ invalid json }";
        mockHttpResponse(invalidJson);

        assertThrows(HolidayClientException.class, () -> {
            client.getHolidaysInMonth(YearMonth.of(2024, 7));
        });
    }

    @Test
    void testFetch_throwsException_whenNetworkFails() throws IOException {
        mockHttpNetworkFailure();

        final HolidayClientException holidayClientException = assertThrows(HolidayClientException.class, () -> {
            client.getHolidaysInMonth(YearMonth.of(2024, 8));
        });

        assertTrue(holidayClientException.getMessage().contains(HTTP_ERROR_MESSAGE));
    }

    @Test
    void testUsesCustomGatewayBaseUrl() throws IOException {
        // 게이트웨이 base URL을 지정하면 data.go.kr이 아니라 그 URL로 호출해야 한다.
        KoreaHolidayClient gwClient = new KoreaHolidayClient(
                "gw-issued-key", "https://gw.example.com/v1/holidays",
                mockHttpClient, objectMapper, cache);
        mockHttpResponse("""
                { "response": { "body": { "items": { "item": [] } } } }
                """);

        gwClient.getHolidaysInYear(2025);

        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(mockHttpClient, atLeastOnce()).newCall(captor.capture());
        String calledUrl = captor.getValue().url().toString();
        assertTrue(calledUrl.startsWith("https://gw.example.com/v1/holidays"),
                "게이트웨이 base URL로 호출되어야 한다: " + calledUrl);
        assertTrue(calledUrl.contains("ServiceKey=gw-issued-key"));
    }

    @Test
    void testConstructor_throwsWhenApiKeyBlank() {
        assertThrows(HolidayClientException.class, () -> new KoreaHolidayClient("  "));
        assertThrows(HolidayClientException.class, () -> new KoreaHolidayClient(null));
    }

    @Test
    void testFetch_doesNotLeakApiKeyInException() throws IOException {
        final String fakeKey = "SUPER_SECRET_KEY_1234567890";
        // 요청 URL에 키가 포함된 500 응답을 구성해, 예외 메시지에 키가 새지 않는지 검증한다.
        Response response = new Response.Builder()
                .request(new Request.Builder()
                        .url("http://apis.data.go.kr/x?ServiceKey=" + fakeKey)
                        .build())
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Internal Server Error")
                .body(ResponseBody.create("{}", MediaType.get("application/json")))
                .build();
        Call call = mock(Call.class);
        when(call.execute()).thenReturn(response);
        when(mockHttpClient.newCall(any())).thenReturn(call);

        final HolidayClientException ex = assertThrows(
                HolidayClientException.class,
                () -> client.getHolidaysInYear(2024));
        assertFalse(ex.getMessage().contains(fakeKey), "예외 메시지에 API 키가 노출되면 안 된다");
    }

    @Test
    void testGetHolidaysInMonth_returnsEmptyWhenNoItems() throws IOException {
        // items가 없는 정상 응답(해당 월 공휴일 없음)도 예외 없이 빈 목록을 반환해야 한다.
        String json = """
                      { "response": { "body": {} } }
                      """;
        mockHttpResponse(json);
        final List<LocalDate> holidays = client.getHolidaysInMonth(YearMonth.of(2024, 11));
        assertTrue(holidays.isEmpty());
    }

    void mockHttpResponse(String body) throws IOException {
        Response response = new Response.Builder()
                .request(new Request.Builder().url("http://localhost").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(body, MediaType.get("application/json")))
                .build();

        Call call = mock(Call.class);
        when(call.execute()).thenReturn(response);
        when(mockHttpClient.newCall(any())).thenReturn(call);
    }

    void mockHttpResponseWithStatus(int code, String message) throws IOException {
        Response response = new Response.Builder()
                .request(new Request.Builder().url("http://localhost").build())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(message)
                .body(ResponseBody.create("{}", MediaType.get("application/json")))
                .build();

        Call call = mock(Call.class);
        when(call.execute()).thenReturn(response);
        when(mockHttpClient.newCall(any())).thenReturn(call);
    }

    void mockHttpResponseWithNullBody() throws IOException {
        Response response = new Response.Builder()
                .request(new Request.Builder().url("http://localhost").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(null)
                .build();

        Call call = mock(Call.class);
        when(call.execute()).thenReturn(response);
        when(mockHttpClient.newCall(any())).thenReturn(call);
    }

    void mockHttpNetworkFailure() throws IOException {
        Response response = mock(Response.class);

        Call call = mock(Call.class);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(false);
        when(mockHttpClient.newCall(any())).thenReturn(call);
    }

}
