package org.example;

import com.ktae23.KoreaHolidayClient;

import java.time.LocalDate;

public class RemittanceApp {

    public static void main(String[] args) {
        LocalDate today = LocalDate.now();
        final String apiKey = System.getenv("KOREA_HOLIDAY_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("환경변수 KOREA_HOLIDAY_API_KEY 를 설정하세요. (공공데이터포털 특일정보 서비스키)");
            System.exit(1);
        }
        final KoreaHolidayClient client = new KoreaHolidayClient(apiKey);

        final LocalDate scheduled = client.afterNWorkingDays(today, 3);
        System.out.println("오늘(" + today + ") 기준 3영업일 후 송금 예정일: " + scheduled);
    }

}
