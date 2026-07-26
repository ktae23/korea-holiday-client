package com.ktae23;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * 연도별 공휴일 조회 결과를 담는 Caffeine 기반 캐시.
 *
 * <p>기본 TTL은 24시간, 최대 100개 연도를 보관한다. {@link KoreaHolidayClient}가 내부적으로 사용하며,
 * TTL을 조정하려면 {@link #KoreaHolidayClientCache(Duration)}를 사용한다.
 */
public class KoreaHolidayClientCache {

    /** 기본 캐시 유지 기간(24시간). */
    public static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private static final long MAXIMUM_SIZE = 100;

    private final Cache<Integer, List<LocalDate>> yearCache;

    /**
     * 기본 TTL(24시간)로 캐시를 생성한다.
     */
    public KoreaHolidayClientCache() {
        this(DEFAULT_TTL);
    }

    /**
     * 지정한 TTL로 캐시를 생성한다.
     *
     * @param ttl 캐시 항목 유지 기간. null이면 기본값(24시간)
     */
    public KoreaHolidayClientCache(final Duration ttl) {
        this.yearCache = Caffeine.newBuilder()
                .expireAfterWrite(ttl == null ? DEFAULT_TTL : ttl)
                .maximumSize(MAXIMUM_SIZE)
                .build();
    }

    /**
     * 연도 → 공휴일 목록 캐시를 반환한다.
     *
     * @return 연도별 캐시
     */
    public Cache<Integer, List<LocalDate>> getYearCache() {
        return yearCache;
    }
}
