package com.ktae23;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.TimeUnit;

class KoreaHolidayClientCache {

    public Cache<YearMonth, List<LocalDate>> getYearMonthListCache() {
        return yearMonthListCache;
    }

    public Cache<Integer, List<LocalDate>> getYearListCache() {
        return yearListCache;
    }

    private final Cache<YearMonth, List<LocalDate>> yearMonthListCache;

    private final Cache<Integer, List<LocalDate>> yearListCache;

    public KoreaHolidayClientCache() {
        this.yearMonthListCache = Caffeine.newBuilder()
                .expireAfterWrite(12, TimeUnit.HOURS)
                .maximumSize(100)
                .build();
        this.yearListCache = Caffeine.newBuilder()
                .expireAfterWrite(12, TimeUnit.HOURS)
                .maximumSize(100)
                .build();
    }
}
