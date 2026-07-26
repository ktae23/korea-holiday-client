package com.ktae23.spring;

import com.ktae23.KoreaHolidayClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * {@link KoreaHolidayClient}를 스프링 빈으로 자동 등록하는 자동설정.
 *
 * <p>{@code korea-holiday.api-key} 프로퍼티가 설정되어 있고, 사용자가 직접 {@link KoreaHolidayClient}
 * 빈을 정의하지 않은 경우에만 기본 빈을 생성한다.
 */
@AutoConfiguration
@ConditionalOnClass(KoreaHolidayClient.class)
@EnableConfigurationProperties(KoreaHolidayProperties.class)
public class KoreaHolidayAutoConfiguration {

    /**
     * 설정된 API 키와 캐시 TTL로 {@link KoreaHolidayClient} 빈을 생성한다.
     *
     * @param properties {@code korea-holiday.*} 프로퍼티
     * @return 기본 {@link KoreaHolidayClient} 빈
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "korea-holiday", name = "api-key")
    public KoreaHolidayClient koreaHolidayClient(final KoreaHolidayProperties properties) {
        return new KoreaHolidayClient(properties.getApiKey(), properties.getCacheTtl());
    }
}
