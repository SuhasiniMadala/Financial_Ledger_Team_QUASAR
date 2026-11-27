package com.quasar.ledger.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;

/**
 * Transaction manager that makes every read-write transaction start with {@code BEGIN IMMEDIATE}
 * (FR-CON-02, ADR-002, ADR-009).
 *
 * <p>Why: SQLite allows one writer. BEGIN IMMEDIATE takes that write lock <em>before</em> the
 * transaction reads any balance or idempotency record, so two transfers can never both read the
 * same old balance. If the lock is not obtained within {@code busy_timeout}, SQLite returns
 * SQLITE_BUSY and the request fails with HTTP 503 having changed nothing (FR-CON-06).
 *
 * <p>How: sqlite-jdbc starts a DEFERRED transaction ("begin;") when auto-commit is switched off.
 * We immediately end that empty transaction and start an IMMEDIATE one. The driver is left in its
 * default DEFERRED mode on purpose: its commit() re-opens a transaction straight away, and in
 * IMMEDIATE mode that would grab the write lock while the connection sits idle (see ADR-009).
 * Read-only transactions keep the DEFERRED begin, which gives a consistent WAL snapshot (FR-REC-05).
 */
public class SqliteImmediateTransactionManager extends DataSourceTransactionManager {

    public SqliteImmediateTransactionManager(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected void prepareTransactionalConnection(Connection con, TransactionDefinition definition) throws SQLException {
        super.prepareTransactionalConnection(con, definition);
        if (definition.isReadOnly()) {
            return;
        }
        try (Statement statement = con.createStatement()) {
            statement.execute("COMMIT");
            try {
                statement.execute("BEGIN IMMEDIATE");
            } catch (SQLException busy) {
                // Restore the driver's expected state (an open deferred transaction) before failing.
                statement.execute("BEGIN");
                throw busy;
            }
        }
    }
}
