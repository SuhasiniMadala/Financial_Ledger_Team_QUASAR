package com.quasar.ledger.module.auth.repository;

import com.quasar.ledger.common.api.PageQuery;
import com.quasar.ledger.common.security.Role;
import com.quasar.ledger.module.auth.model.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** All SQL is parameterised (NFR-SEC-05). */
@Repository
public class JdbcUserAccountRepository implements UserAccountRepository {

    private static final RowMapper<UserAccount> MAPPER = (rs, i) -> new UserAccount(
            rs.getString("id"), rs.getString("email"), rs.getString("password_hash"),
            rs.getString("display_name"), Role.valueOf(rs.getString("role")), rs.getString("created_at"));

    private final JdbcTemplate jdbc;

    public JdbcUserAccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(UserAccount a) {
        jdbc.update("INSERT INTO user_account (id, email, password_hash, display_name, role, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                a.id(), a.email(), a.passwordHash(), a.displayName(), a.role().name(), a.createdAt());
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return jdbc.query("SELECT * FROM user_account WHERE email = ?", MAPPER, email).stream().findFirst();
    }

    @Override
    public Optional<UserAccount> findById(String id) {
        return jdbc.query("SELECT * FROM user_account WHERE id = ?", MAPPER, id).stream().findFirst();
    }

    @Override
    public List<UserAccount> findByRole(Role role, PageQuery page) {
        return jdbc.query("SELECT * FROM user_account WHERE role = ? ORDER BY created_at, id LIMIT ? OFFSET ?",
                MAPPER, role.name(), page.size(), page.offset());
    }

    @Override
    public long countByRole(Role role) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM user_account WHERE role = ?", Long.class, role.name());
        return n == null ? 0 : n;
    }
}
