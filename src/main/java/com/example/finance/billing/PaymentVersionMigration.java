package com.example.finance.billing;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Initialize the new optimistic-lock field on pre-subscription payment rows. */
@Component
@DependsOn("entityManagerFactory")
public class PaymentVersionMigration {
    private final JdbcTemplate jdbc;
    public PaymentVersionMigration(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @PostConstruct
    public void initializeLegacyVersions() {
        jdbc.update("UPDATE stripe_payments SET version = 0 WHERE version IS NULL");
        jdbc.update("UPDATE stripe_payments SET cancel_at_period_end = false WHERE cancel_at_period_end IS NULL");
    }
}
