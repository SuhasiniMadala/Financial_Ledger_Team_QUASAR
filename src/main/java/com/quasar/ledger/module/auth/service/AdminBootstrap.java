package com.quasar.ledger.module.auth.service;

import com.quasar.ledger.common.security.Role;
import com.quasar.ledger.config.LedgerProperties;
import com.quasar.ledger.module.auth.repository.UserAccountRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.stereotype.Component;

/**
 * FR-AUTH-05: ADMIN accounts come only from deployment configuration
 * (LEDGER_ADMIN_EMAIL / LEDGER_ADMIN_PASSWORD), never from the API.
 */
@Component
@DependsOnDatabaseInitialization
public class AdminBootstrap {

    private static final Logger LOG = LoggerFactory.getLogger(AdminBootstrap.class);

    private final LedgerProperties properties;
    private final UserAccountRepository users;
    private final AuthService auth;

    public AdminBootstrap(LedgerProperties properties, UserAccountRepository users, AuthService auth) {
        this.properties = properties;
        this.users = users;
        this.auth = auth;
    }

    @PostConstruct
    public void ensureConfiguredAdmin() {
        String email = properties.admin().email();
        String password = properties.admin().password();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            LOG.warn("No admin configured (LEDGER_ADMIN_EMAIL / LEDGER_ADMIN_PASSWORD not set); admin endpoints are unusable.");
            return;
        }
        users.findByEmail(AuthService.normalizeEmail(email)).ifPresentOrElse(existing -> {
            if (existing.role() != Role.ADMIN) {
                LOG.error("Configured admin email belongs to a non-admin account; it will NOT be promoted.");
            }
        }, () -> {
            auth.createConfiguredAdmin(email, password, properties.admin().displayName());
            LOG.info("Created the configured admin account.");
        });
    }
}
