package com.quasar.ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FinancialLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinancialLedgerApplication.class, args);
    }
}
