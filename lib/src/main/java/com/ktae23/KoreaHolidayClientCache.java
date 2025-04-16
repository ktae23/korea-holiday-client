package com.ktae23;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.TimeUnit;

class KoreaHolidayClientCache {

    public Cache<Integer, List<LocalDate>> getYearCache() {
        return yearCache;
    }

    private final Cache<Integer, List<LocalDate>> yearCache;

    public KoreaHolidayClientCache() {
        this.yearCache = Caffeine.newBuilder()
                .expireAfterWrite(12, TimeUnit.HOURS)
                .maximumSize(100)
                .build();
    }
}
