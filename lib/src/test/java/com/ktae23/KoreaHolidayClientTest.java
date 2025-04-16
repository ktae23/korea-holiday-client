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

    private void mockHttpResponse(String json) throws IOException {
        Call mockCall = mock(Call.class);
        ResponseBody body = ResponseBody.create(json, MediaType.get("application/json"));

        Response response = new Response.Builder()
                .request(new Request.Builder().url("http://test").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(body)
                .build();

        when(mockHttpClient.newCall(any())).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(response);
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
        YearMonth yearMonth = YearMonth.of(2023, 4);
        List<LocalDate> cachedHolidays = getMonthCache(yearMonth);

        List<LocalDate> holidays = client.getHolidaysInMonth(yearMonth);

        assertEquals(2, holidays.size());
        assertEquals(cachedHolidays, holidays);
    }

    @NotNull
    private List<LocalDate> getMonthCache(final YearMonth yearMonth) {
        List<LocalDate> cachedHolidays = List.of(
                LocalDate.of(2023, 4, 1),
                LocalDate.of(2023, 4, 2)
        );

        cache.getYearMonthListCache().put(yearMonth, cachedHolidays);
        return cachedHolidays;
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
        int year = 2023;
        List<LocalDate> cachedHolidays = getYearCache(year);

        List<LocalDate> holidays = client.getHolidaysInYear(year);

        assertEquals(2, holidays.size());
        assertEquals(cachedHolidays, holidays);
    }

    @NotNull
    private List<LocalDate> getYearCache(final int year) {
        List<LocalDate> cachedHolidays = List.of(
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 12, 25)
        );

        cache.getYearListCache().put(year, cachedHolidays);
        return cachedHolidays;
    }
}
