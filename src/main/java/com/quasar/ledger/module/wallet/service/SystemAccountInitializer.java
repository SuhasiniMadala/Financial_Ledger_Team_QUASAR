package com.quasar.ledger.module.wallet.service;

import com.quasar.ledger.common.util.Timestamps;
import com.quasar.ledger.config.LedgerProperties;
import com.quasar.ledger.module.wallet.repository.AccountRepository;
import jakarta.annotation.PostConstruct;
import java.time.Clock;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.stereotype.Component;

/** ACC-01: one SYSTEM_FUNDING account per supported currency, created at start-up and owned by no user. */
@Component
@DependsOnDatabaseInitialization
public class SystemAccountInitializer {

    private final AccountRepository accounts;
    private final LedgerProperties properties;
    private final Clock clock;

    public SystemAccountInitializer(AccountRepository accounts, LedgerProperties properties, Clock clock) {
        this.accounts = accounts;
        this.properties = properties;
        this.clock = clock;
    }

    @PostConstruct
    public void createSystemAccounts() {
        for (String currency : properties.currencies()) {
            accounts.ensureSystemFunding(currency, Timestamps.format(clock.instant()));
        }
    }
}
