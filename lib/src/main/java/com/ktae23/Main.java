package com.ktae23;

import java.time.LocalDate;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        final KoreaHolidayClient koreaHolidayClient = new KoreaHolidayClient(
                "***REMOVED***");
        final List<LocalDate> holidaysInYear = koreaHolidayClient.getHolidaysInYear(2025);
        System.out.println("holidaysInYear = " + holidaysInYear);
    }
}
