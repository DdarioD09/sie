package com.sie.core.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Turns on the {@code @CreatedDate}/{@code @LastModifiedDate} auditing used by {@link BaseEntity}. */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {}
