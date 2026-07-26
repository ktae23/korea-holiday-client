package com.ktae23.spring;

import com.ktae23.KoreaHolidayClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KoreaHolidayAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KoreaHolidayAutoConfiguration.class));

    @Test
    void createsClientBean_whenApiKeyPresent() {
        runner.withPropertyValues("korea-holiday.api-key=dummy-key")
                .run(context -> assertTrue(context.getBeansOfType(KoreaHolidayClient.class).size() == 1));
    }

    @Test
    void doesNotCreateClientBean_whenApiKeyMissing() {
        runner.run(context -> assertTrue(context.getBeansOfType(KoreaHolidayClient.class).isEmpty()));
    }

    @Test
    void bindsCustomCacheTtl() {
        runner.withPropertyValues("korea-holiday.api-key=dummy-key", "korea-holiday.cache-ttl=1h")
                .run(context -> {
                    KoreaHolidayProperties props = context.getBean(KoreaHolidayProperties.class);
                    assertEquals(java.time.Duration.ofHours(1), props.getCacheTtl());
                });
    }
}
