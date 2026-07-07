package org.nbfc.loanemicalculator.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring's declarative caching. The {@code CacheManager} itself is provided by
 * Spring Boot auto-configuration: a simple in-memory {@code ConcurrentMapCacheManager}
 * in production, and a no-op manager during tests (via {@code spring.cache.type=none})
 * so cached reads never mask fresh data. Swap in Caffeine/Redis for production scale.
 */
@Configuration
@EnableCaching
public class CachingConfig {

    public static final String DASHBOARD_CACHE = "dashboard";
    public static final String CUSTOMER_SUMMARY_CACHE = "customerSummary";
}
