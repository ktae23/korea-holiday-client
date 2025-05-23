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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KoreaHolidayClientTest {

    private static final String ERROR_MESSAGE = "Error while calling holiday API";

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

        assertTrue(exception.getMessage().contains(ERROR_MESSAGE));
    }

    @Test
    void testFetch_returnsEmptyList_whenBodyIsNull() throws IOException {
        mockHttpResponseWithNullBody();

        final HolidayClientException holidayClientException = assertThrows(
                HolidayClientException.class,
                () -> client.getHolidaysInMonth(YearMonth.of(2024, 5))
        );

        assertTrue(holidayClientException.getMessage().contains(ERROR_MESSAGE));
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

        assertTrue(holidayClientException.getMessage().contains(ERROR_MESSAGE));
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
