package com.quasar.ledger.module.wallet.repository;

import com.quasar.ledger.common.api.PageQuery;
import com.quasar.ledger.common.id.Ids;
import com.quasar.ledger.module.wallet.model.Account;
import com.quasar.ledger.module.wallet.model.AccountStatus;
import com.quasar.ledger.module.wallet.model.AccountSummary;
import com.quasar.ledger.module.wallet.model.AccountType;
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
public class JdbcAccountRepository implements AccountRepository {

    private static final RowMapper<Account> MAPPER = (rs, i) -> new Account(
            rs.getString("id"), rs.getString("owner_id"), AccountType.valueOf(rs.getString("account_type")),
            rs.getString("currency"), rs.getLong("balance_minor"), AccountStatus.valueOf(rs.getString("status")),
            rs.getString("created_at"));

    private static final String NAME_SQL = """
            CASE WHEN a.account_type = 'SYSTEM_FUNDING' THEN 'System Funding'
                 WHEN a.account_type = 'MERCHANT_WALLET' THEN COALESCE(m.business_name, u.display_name)
                 ELSE u.display_name END""";

    private final JdbcTemplate jdbc;

    public JdbcAccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(Account a) {
        jdbc.update("INSERT INTO account (id, owner_id, account_type, currency, balance_minor, status, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                a.id(), a.ownerId(), a.type().name(), a.currency(), a.balanceMinor(), a.status().name(), a.createdAt());
    }

    @Override
    public void ensureSystemFunding(String currency, String createdAt) {
        jdbc.update("INSERT OR IGNORE INTO account (id, owner_id, account_type, currency, balance_minor, status, created_at) "
                + "VALUES (?, NULL, 'SYSTEM_FUNDING', ?, 0, 'ACTIVE', ?)", Ids.systemFundingId(currency), currency, createdAt);
    }

    @Override
    public Optional<Account> findById(String id) {
        return jdbc.query("SELECT * FROM account WHERE id = ?", MAPPER, id).stream().findFirst();
    }

    @Override
    public List<Account> findByOwner(String ownerId) {
        return jdbc.query("SELECT * FROM account WHERE owner_id = ? ORDER BY created_at, id", MAPPER, ownerId);
    }

    @Override
    public Optional<Account> findByOwnerAndCurrency(String ownerId, String currency) {
        return jdbc.query("SELECT * FROM account WHERE owner_id = ? AND currency = ?", MAPPER, ownerId, currency)
                .stream().findFirst();
    }

    @Override
    public void updateStatus(String id, AccountStatus status) {
        jdbc.update("UPDATE account SET status = ? WHERE id = ?", status.name(), id);
    }

    @Override
    public void updateBalance(String id, long balanceMinor) {
        int rows = jdbc.update("UPDATE account SET balance_minor = ? WHERE id = ?", balanceMinor, id);
        if (rows != 1) {
            throw new IllegalStateException("Balance update affected " + rows + " rows for account " + id);
        }
    }

    @Override
    public List<AccountSummary> findAll(PageQuery page) {
        return jdbc.query("SELECT a.*, " + NAME_SQL + " AS owner_name FROM account a "
                        + "LEFT JOIN user_account u ON u.id = a.owner_id LEFT JOIN merchant_profile m ON m.user_account_id = a.owner_id "
                        + "ORDER BY a.created_at, a.id LIMIT ? OFFSET ?",
                (rs, i) -> new AccountSummary(rs.getString("id"), rs.getString("owner_id"), rs.getString("owner_name"),
                        AccountType.valueOf(rs.getString("account_type")), rs.getString("currency"), rs.getLong("balance_minor"),
                        AccountStatus.valueOf(rs.getString("status")), rs.getString("created_at")),
                page.size(), page.offset());
    }

    @Override
    public long countAll() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM account", Long.class);
        return n == null ? 0 : n;
    }

    @Override
    public Map<AccountStatus, Long> countWalletsByStatus() {
        Map<AccountStatus, Long> counts = new EnumMap<>(AccountStatus.class);
        for (AccountStatus s : AccountStatus.values()) {
            counts.put(s, 0L);
        }
        jdbc.query("SELECT status, COUNT(*) FROM account WHERE account_type <> 'SYSTEM_FUNDING' GROUP BY status",
                rs -> {
                    counts.put(AccountStatus.valueOf(rs.getString(1)), rs.getLong(2));
                });
        return counts;
    }

    @Override
    public Map<String, String> displayNames(Collection<String> accountIds) {
        if (accountIds.isEmpty()) {
            return Collections.emptyMap();
        }
        String placeholders = String.join(",", Collections.nCopies(accountIds.size(), "?"));
        Map<String, String> names = new HashMap<>();
        jdbc.query("SELECT a.id, " + NAME_SQL + " AS name FROM account a "
                        + "LEFT JOIN user_account u ON u.id = a.owner_id LEFT JOIN merchant_profile m ON m.user_account_id = a.owner_id "
                        + "WHERE a.id IN (" + placeholders + ")",
                rs -> {
                    names.put(rs.getString(1), rs.getString(2));
                }, accountIds.toArray());
        return names;
    }
}
