package org.example;

import com.ktae23.KoreaHolidayClient;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class NextMonthHolidaysApp {

    public static void main(String[] args) {
        final String apiKey = System.getenv("KOREA_HOLIDAY_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("환경변수 KOREA_HOLIDAY_API_KEY 를 설정하세요. (공공데이터포털 특일정보 서비스키)");
            System.exit(1);
        }
        final KoreaHolidayClient client = new KoreaHolidayClient(apiKey);
        YearMonth nextMonth = YearMonth.now().plusMonths(1);

        List<LocalDate> holidays = client.getHolidaysInMonth(nextMonth);
        System.out.println(nextMonth + "의 공휴일:");
        for (LocalDate d : holidays) {
            System.out.println("  • " + d + " (" + d.getDayOfWeek() + ")");
        }
    }

}
