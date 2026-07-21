package org.example;

import com.ktae23.KoreaHolidayClient;

import java.time.LocalDate;

/**
 * 3영업일 후 송금 예정일을 계산하는 예제.
 *
 * <p>실행 전 환경변수를 설정하세요:
 * <pre>{@code
 * export KOREA_HOLIDAY_API_KEY="발급받은_서비스키"
 * }</pre>
 */
public class RemittanceApp {

    private static final String API_KEY_ENV = "KOREA_HOLIDAY_API_KEY";

    public static void main(String[] args) {
        final String apiKey = System.getenv(API_KEY_ENV);
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("환경변수 " + API_KEY_ENV + " 가 설정되지 않았습니다.");
            System.err.println("  export " + API_KEY_ENV + "=\"발급받은_서비스키\"");
            System.exit(1);
        }

        final LocalDate today = LocalDate.now();
        final KoreaHolidayClient client = new KoreaHolidayClient(apiKey);

        final LocalDate scheduled = client.afterNWorkingDays(today, 3);
        System.out.println("오늘(" + today + ") 기준 3영업일 후 송금 예정일: " + scheduled);
    }

}
