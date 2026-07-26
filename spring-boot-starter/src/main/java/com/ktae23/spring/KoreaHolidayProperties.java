package com.ktae23.spring;

import com.ktae23.KoreaHolidayClientCache;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * {@code korea-holiday.*} 설정 프로퍼티.
 *
 * <pre>{@code
 * korea-holiday:
 *   api-key: ${KOREA_HOLIDAY_API_KEY}   # 필수. 공공데이터포털 특일정보 서비스키(디코딩 키)
 *   cache-ttl: 24h                      # 선택. 공휴일 캐시 유지 기간(기본 24h)
 * }</pre>
 */
@ConfigurationProperties(prefix = "korea-holiday")
public class KoreaHolidayProperties {

    /**
     * 공공데이터포털 특일정보 서비스키(디코딩 키). 절대 소스에 하드코딩하지 말고 환경변수/시크릿으로 주입한다.
     */
    private String apiKey;

    /**
     * 공휴일 조회 결과의 캐시 유지 기간. 기본 24시간.
     */
    private Duration cacheTtl = KoreaHolidayClientCache.DEFAULT_TTL;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(final String apiKey) {
        this.apiKey = apiKey;
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(final Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }
}
