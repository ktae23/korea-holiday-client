package com.ktae23;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 연도별 공휴일 캐시.
 *
 * <p>기본값은 12시간 TTL, 최대 100개 연도다. 정책을 바꾸려면
 * {@link #KoreaHolidayClientCache(long, TimeUnit, long)} 을 쓰거나
 * 직접 만든 {@link Cache} 를 주입한다.
 *
 * <p>값을 {@link Set} 으로 보관해 날짜 포함 여부 판정이 {@code O(1)} 이다.
 * 영업일 계산처럼 반복 호출되는 경로에서 차이가 난다.
 */
public class KoreaHolidayClientCache {

    private static final long DEFAULT_TTL = 12L;
    private static final TimeUnit DEFAULT_TTL_UNIT = TimeUnit.HOURS;
    private static final long DEFAULT_MAX_SIZE = 100L;

    private final Cache<Integer, Set<LocalDate>> yearCache;

    public KoreaHolidayClientCache() {
        this(DEFAULT_TTL, DEFAULT_TTL_UNIT, DEFAULT_MAX_SIZE);
    }

    public KoreaHolidayClientCache(final long ttl, final TimeUnit ttlUnit, final long maximumSize) {
        this.yearCache = Caffeine.newBuilder()
                .expireAfterWrite(ttl, ttlUnit)
                .maximumSize(maximumSize)
                .build();
    }

    public KoreaHolidayClientCache(final Cache<Integer, Set<LocalDate>> yearCache) {
        this.yearCache = yearCache;
    }

    public Cache<Integer, Set<LocalDate>> getYearCache() {
        return yearCache;
    }

    /**
     * 캐시를 전부 비운다.
     * 임시공휴일이 갑자기 지정돼 즉시 재조회가 필요할 때 쓴다.
     */
    public void invalidateAll() {
        yearCache.invalidateAll();
    }

    /** 특정 연도만 캐시에서 비운다. */
    public void invalidate(final int year) {
        yearCache.invalidate(year);
    }
}
