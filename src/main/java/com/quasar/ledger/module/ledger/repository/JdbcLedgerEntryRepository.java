package com.quasar.ledger.module.ledger.repository;

import com.quasar.ledger.common.api.PageQuery;
import com.quasar.ledger.module.ledger.model.EntryDirection;
import com.quasar.ledger.module.ledger.model.LedgerEntry;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLedgerEntryRepository implements LedgerEntryRepository {

    private static final RowMapper<LedgerEntry> MAPPER = (rs, i) -> new LedgerEntry(
            rs.getLong("sequence_no"), rs.getString("id"), rs.getString("transaction_id"), rs.getString("account_id"),
            EntryDirection.valueOf(rs.getString("direction")), rs.getLong("amount_minor"), rs.getLong("balance_after_minor"),
            rs.getString("created_at"));

    private final JdbcTemplate jdbc;

    public JdbcLedgerEntryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(LedgerEntry e) {
        jdbc.update("INSERT INTO ledger_entry (id, transaction_id, account_id, direction, amount_minor, balance_after_minor, "
                        + "created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                e.id(), e.transactionId(), e.accountId(), e.direction().name(), e.amountMinor(), e.balanceAfterMinor(), e.createdAt());
    }

    @Override
    public List<LedgerEntry> findByTransaction(String transactionId) {
        return jdbc.query("SELECT * FROM ledger_entry WHERE transaction_id = ? ORDER BY sequence_no", MAPPER, transactionId);
    }

    @Override
    public List<LedgerEntry> findByAccount(String accountId, PageQuery page) {
        return jdbc.query("SELECT * FROM ledger_entry WHERE account_id = ? ORDER BY sequence_no DESC LIMIT ? OFFSET ?",
                MAPPER, accountId, page.size(), page.offset());
    }

    @Override
    public long countByAccount(String accountId) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM ledger_entry WHERE account_id = ?", Long.class, accountId);
        return n == null ? 0 : n;
    }
}
