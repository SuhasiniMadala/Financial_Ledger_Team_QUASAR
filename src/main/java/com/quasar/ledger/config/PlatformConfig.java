package com.quasar.ledger.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Infrastructure beans that tests may replace (NFR-MAIN-03: clock behind an interface). */
@Configuration
public class PlatformConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** NFR-SEC-01: passwords are stored only as BCrypt hashes with work factor >= 10. */
    @Bean
    public PasswordEncoder passwordEncoder(LedgerProperties properties) {
        return new BCryptPasswordEncoder(properties.bcryptStrength());
    }
}
