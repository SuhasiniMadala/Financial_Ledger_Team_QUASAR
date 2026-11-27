package com.quasar.ledger.module.ledger.repository;

import com.quasar.ledger.common.api.PageQuery;
import com.quasar.ledger.common.util.DateRange;
import com.quasar.ledger.module.ledger.model.TransactionRecord;
import com.quasar.ledger.module.ledger.model.TransactionStatus;
import com.quasar.ledger.module.ledger.model.TransactionType;
import com.quasar.ledger.module.ledger.model.TypeCurrencyTotal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTransactionRepository implements TransactionRepository {

    static final RowMapper<TransactionRecord> MAPPER = (rs, i) -> new TransactionRecord(
            rs.getString("id"), TransactionType.valueOf(rs.getString("type")), TransactionStatus.valueOf(rs.getString("status")),
            rs.getLong("amount_minor"), rs.getString("currency"), rs.getString("initiated_by"), rs.getString("source_account_id"),
            rs.getString("destination_account_id"), rs.getString("reversal_of_id"), rs.getString("failure_code"),
            rs.getString("description"), rs.getString("created_at"));

    private final JdbcTemplate jdbc;

    public JdbcTransactionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(TransactionRecord t) {
        jdbc.update("INSERT INTO transaction_record (id, type, status, amount_minor, currency, initiated_by, source_account_id, "
                        + "destination_account_id, reversal_of_id, failure_code, description, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                t.id(), t.type().name(), t.status().name(), t.amountMinor(), t.currency(), t.initiatedBy(), t.sourceAccountId(),
                t.destinationAccountId(), t.reversalOfId(), t.failureCode(), t.description(), t.createdAt());
    }

    @Override
    public Optional<TransactionRecord> findById(String id) {
        return jdbc.query("SELECT * FROM transaction_record WHERE id = ?", MAPPER, id).stream().findFirst();
    }

    @Override
    public Optional<TransactionRecord> findReversalOf(String originalId) {
        return jdbc.query("SELECT * FROM transaction_record WHERE reversal_of_id = ?", MAPPER, originalId).stream().findFirst();
    }

    @Override
    public Map<String, String> findReversalIds(Collection<String> originalIds) {
        if (originalIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new HashMap<>();
        String in = String.join(",", Collections.nCopies(originalIds.size(), "?"));
        jdbc.query("SELECT reversal_of_id, id FROM transaction_record WHERE reversal_of_id IN (" + in + ")",
                rs -> {
                    result.put(rs.getString(1), rs.getString(2));
                }, originalIds.toArray());
        return result;
    }

    @Override
    public List<TransactionRecord> search(TransactionSearch search, PageQuery page) {
        List<Object> args = new ArrayList<>();
        String where = where(search, args);
        args.add(page.size());
        args.add(page.offset());
        return jdbc.query("SELECT * FROM transaction_record t" + where + " ORDER BY t.created_at DESC, t.rowid DESC LIMIT ? OFFSET ?",
                MAPPER, args.toArray());
    }

    @Override
    public long count(TransactionSearch search) {
        List<Object> args = new ArrayList<>();
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM transaction_record t" + where(search, args), Long.class, args.toArray());
        return n == null ? 0 : n;
    }

    /** Builds a parameterised WHERE clause (NFR-SEC-05): values are always bound, never concatenated. */
    private static String where(TransactionSearch s, List<Object> args) {
        List<String> clauses = new ArrayList<>();
        if (s.accountId() != null) {
            clauses.add("(t.source_account_id = ? OR t.destination_account_id = ?)");
            args.add(s.accountId());
            args.add(s.accountId());
        }
        if (s.type() != null) {
            clauses.add("t.type = ?");
            args.add(s.type().name());
        }
        if (s.status() != null) {
            clauses.add("t.status = ?");
            args.add(s.status().name());
        }
        if (s.range() != null && s.range().fromInclusive() != null) {
            clauses.add("t.created_at >= ?");
            args.add(s.range().fromInclusive());
        }
        if (s.range() != null && s.range().toExclusive() != null) {
            clauses.add("t.created_at < ?");
            args.add(s.range().toExclusive());
        }
        if (s.visibleAccounts() != null) {
            // FR-LED-11: COMPLETED transactions on the caller's wallets, plus the caller's own FAILED attempts.
            List<String> parts = new ArrayList<>();
            if (!s.visibleAccounts().isEmpty()) {
                String in = String.join(",", Collections.nCopies(s.visibleAccounts().size(), "?"));
                parts.add("(t.status = 'COMPLETED' AND (t.source_account_id IN (" + in + ") OR t.destination_account_id IN (" + in + ")))");
                args.addAll(s.visibleAccounts());
                args.addAll(s.visibleAccounts());
            }
            parts.add("(t.status = 'FAILED' AND t.initiated_by = ?)");
            args.add(s.failedVisibleTo());
            clauses.add("(" + String.join(" OR ", parts) + ")");
        }
        return clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);
    }

    @Override
    public Map<TransactionStatus, Long> countByStatus() {
        Map<TransactionStatus, Long> counts = new EnumMap<>(TransactionStatus.class);
        for (TransactionStatus s : TransactionStatus.values()) {
            counts.put(s, 0L);
        }
        jdbc.query("SELECT status, COUNT(*) FROM transaction_record GROUP BY status", rs -> {
            counts.put(TransactionStatus.valueOf(rs.getString(1)), rs.getLong(2));
        });
        return counts;
    }

    @Override
    public List<TypeCurrencyTotal> completedTotals() {
        return jdbc.query("SELECT type, currency, COUNT(*), SUM(amount_minor) FROM transaction_record WHERE status = 'COMPLETED' "
                        + "GROUP BY type, currency ORDER BY type, currency",
                (rs, i) -> new TypeCurrencyTotal(TransactionType.valueOf(rs.getString(1)), rs.getString(2), rs.getLong(3),
                        rs.getLong(4)));
    }

    @Override
    public long[] merchantPaymentCountAndTotal(String merchantWalletId, DateRange range) {
        return jdbc.queryForObject("SELECT COUNT(*), COALESCE(SUM(amount_minor), 0) FROM transaction_record "
                        + "WHERE type = 'MERCHANT_PAYMENT' AND status = 'COMPLETED' AND destination_account_id = ? "
                        + "AND created_at >= ? AND created_at < ?",
                (rs, i) -> new long[] {rs.getLong(1), rs.getLong(2)},
                merchantWalletId, range.fromInclusive(), range.toExclusive());
    }
}
