package com.quasar.ledger.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

/**
 * SQLite configuration (DB-01, DB-02, ADR-002): one database file in WAL mode with foreign keys on,
 * synchronous=FULL (durability, NFR-SAFE-04) and a configurable busy timeout (FR-CON-06).
 * Every pooled connection gets these settings when it is opened.
 */
@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

    @Bean
    public DataSource dataSource(LedgerProperties properties) {
        Path file = Path.of(properties.db().path()).toAbsolutePath();
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create database directory for " + file, e);
        }
        SQLiteConfig sqlite = new SQLiteConfig();
        sqlite.setJournalMode(SQLiteConfig.JournalMode.WAL);
        sqlite.setSynchronous(SQLiteConfig.SynchronousMode.FULL);
        sqlite.enforceForeignKeys(true);
        sqlite.setBusyTimeout(properties.db().busyTimeoutMs());

        SQLiteDataSource sqliteDataSource = new SQLiteDataSource(sqlite);
        sqliteDataSource.setUrl("jdbc:sqlite:" + file);

        HikariConfig pool = new HikariConfig();
        pool.setDataSource(sqliteDataSource);
        pool.setPoolName("ledger-sqlite");
        pool.setMaximumPoolSize(properties.db().poolSize());
        pool.setConnectionTimeout(30_000);
        return new HikariDataSource(pool);
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new SqliteImmediateTransactionManager(dataSource);
    }

    /** Read-write transaction (BEGIN IMMEDIATE). Used where the whole unit of work must hold the write lock. */
    @Bean
    public TransactionTemplate writeTransaction(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
