package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktae23.KoreaHolidayClient;
import okhttp3.*;
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

public class KoreaHolidayClientTest {

    private OkHttpClient mockHttpClient;
    private ObjectMapper objectMapper;
    private KoreaHolidayClient client;
    private String apiKey = "dummy-api-key";

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(OkHttpClient.class);
        objectMapper = new ObjectMapper();
        client = new KoreaHolidayClient(apiKey, mockHttpClient, objectMapper);
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
    void testGetHolidaysInMonth() throws IOException {
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

        mockHttpResponse(json);
        List<LocalDate> holidays = client.getHolidaysInMonth(YearMonth.of(2023, 4));
        assertEquals(2, holidays.size());
        assertTrue(holidays.contains(LocalDate.of(2023, 4, 5)));
        assertTrue(holidays.contains(LocalDate.of(2023, 4, 10)));
    }

    @Test
    void testGetHolidaysInYear() throws IOException {
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
}
