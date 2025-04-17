package org.example;

import com.ktae23.KoreaHolidayClient;

import java.time.LocalDate;

public class RemittanceApp {

    public static void main(String[] args) {
        LocalDate today = LocalDate.now();
        final String apiKey = "YOUR_API_KEY_HERE";
        final KoreaHolidayClient client = new KoreaHolidayClient(apiKey);

        final LocalDate scheduled = client.afterNWorkingDays(today, 3);
        System.out.println("오늘(" + today + ") 기준 3영업일 후 송금 예정일: " + scheduled);
    }

}
