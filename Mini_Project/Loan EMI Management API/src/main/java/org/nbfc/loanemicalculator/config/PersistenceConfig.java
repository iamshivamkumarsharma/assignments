package org.nbfc.loanemicalculator.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Turns on JPA auditing so {@code @CreatedDate} / {@code @LastModifiedDate}
 * fields on entities are populated automatically.
 */
@Configuration
@EnableJpaAuditing
public class PersistenceConfig {
}
