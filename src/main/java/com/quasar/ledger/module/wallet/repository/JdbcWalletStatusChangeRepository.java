package com.quasar.ledger.module.wallet.repository;

import com.quasar.ledger.module.wallet.model.AccountStatus;
import com.quasar.ledger.module.wallet.model.WalletStatusChange;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcWalletStatusChangeRepository implements WalletStatusChangeRepository {

    private final JdbcTemplate jdbc;

    public JdbcWalletStatusChangeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(WalletStatusChange c) {
        jdbc.update("INSERT INTO wallet_status_change (id, account_id, old_status, new_status, changed_by, reason, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                c.id(), c.accountId(), c.oldStatus().name(), c.newStatus().name(), c.changedBy(), c.reason(), c.createdAt());
    }

    @Override
    public List<WalletStatusChange> findByAccount(String accountId) {
        return jdbc.query("SELECT * FROM wallet_status_change WHERE account_id = ? ORDER BY created_at, rowid",
                (rs, i) -> new WalletStatusChange(rs.getString("id"), rs.getString("account_id"),
                        AccountStatus.valueOf(rs.getString("old_status")), AccountStatus.valueOf(rs.getString("new_status")),
                        rs.getString("changed_by"), rs.getString("reason"), rs.getString("created_at")),
                accountId);
    }
}
