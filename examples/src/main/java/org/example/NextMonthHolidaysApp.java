package org.example;

import com.ktae23.KoreaHolidayClient;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 다음 달 공휴일을 출력하는 예제.
 *
 * <p>실행 전 환경변수를 설정하세요:
 * <pre>{@code
 * export KOREA_HOLIDAY_API_KEY="발급받은_서비스키"
 * }</pre>
 */
public class NextMonthHolidaysApp {

    private static final String API_KEY_ENV = "KOREA_HOLIDAY_API_KEY";

    public static void main(String[] args) {
        final String apiKey = System.getenv(API_KEY_ENV);
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("환경변수 " + API_KEY_ENV + " 가 설정되지 않았습니다.");
            System.err.println("  export " + API_KEY_ENV + "=\"발급받은_서비스키\"");
            System.exit(1);
        }

        final KoreaHolidayClient client = new KoreaHolidayClient(apiKey);
        final YearMonth nextMonth = YearMonth.now().plusMonths(1);

        final List<LocalDate> holidays = client.getHolidaysInMonth(nextMonth);
        System.out.println(nextMonth + "의 공휴일:");
        for (LocalDate d : holidays) {
            System.out.println("  • " + d + " (" + d.getDayOfWeek() + ")");
        }
    }

}
