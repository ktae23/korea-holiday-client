package org.example;

import com.ktae23.KoreaHolidayClient;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class NextMonthHolidaysApp {

    public static void main(String[] args) {
        final String apiKey = "YOUR_API_KEY_HERE";
        final KoreaHolidayClient client = new KoreaHolidayClient(apiKey);
        YearMonth nextMonth = YearMonth.now().plusMonths(1);

        List<LocalDate> holidays = client.getHolidaysInMonth(nextMonth);
        System.out.println(nextMonth + "의 공휴일:");
        for (LocalDate d : holidays) {
            System.out.println("  • " + d + " (" + d.getDayOfWeek() + ")");
        }
    }

}
