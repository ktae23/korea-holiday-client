package org.example;

import com.ktae23.KoreaHolidayClient;

import java.time.LocalDate;
import java.util.List;

/**
 * 특정 연도의 공휴일을 조회해 출력하는 데모.
 *
 * <p>API 키는 절대 코드에 하드코딩하지 않습니다. 환경변수 {@code KOREA_HOLIDAY_API_KEY}
 * 로 주입하세요.
 *
 * <pre>{@code
 * export KOREA_HOLIDAY_API_KEY="발급받은_디코딩_키"
 * ./gradlew :examples:run
 * }</pre>
 */
public class HolidaysDemoApp {

    public static void main(String[] args) {
        final String apiKey = System.getenv("KOREA_HOLIDAY_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("환경변수 KOREA_HOLIDAY_API_KEY 를 설정하세요. (공공데이터포털 특일정보 서비스키)");
            System.exit(1);
        }

        final int year = args.length > 0 ? Integer.parseInt(args[0]) : LocalDate.now().getYear();

        final KoreaHolidayClient client = new KoreaHolidayClient(apiKey);
        final List<LocalDate> holidays = client.getHolidaysInYear(year);
        System.out.println(year + "년 공휴일 (" + holidays.size() + "일): " + holidays);
    }
}
