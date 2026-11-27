package com.quasar.ledger.module.auth.repository;

import com.quasar.ledger.common.api.PageQuery;
import com.quasar.ledger.module.auth.model.MerchantProfile;
import com.quasar.ledger.module.auth.model.MerchantSummary;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMerchantProfileRepository implements MerchantProfileRepository {

    private static final RowMapper<MerchantProfile> MAPPER = (rs, i) -> new MerchantProfile(
            rs.getString("id"), rs.getString("user_account_id"), rs.getString("business_name"), rs.getString("merchant_code"));

    private final JdbcTemplate jdbc;

    public JdbcMerchantProfileRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(MerchantProfile p) {
        jdbc.update("INSERT INTO merchant_profile (id, user_account_id, business_name, merchant_code) VALUES (?, ?, ?, ?)",
                p.id(), p.userAccountId(), p.businessName(), p.merchantCode());
    }

    @Override
    public Optional<MerchantProfile> findByMerchantCode(String merchantCode) {
        return jdbc.query("SELECT * FROM merchant_profile WHERE merchant_code = ?", MAPPER, merchantCode).stream().findFirst();
    }

    @Override
    public Optional<MerchantProfile> findByUserAccountId(String userAccountId) {
        return jdbc.query("SELECT * FROM merchant_profile WHERE user_account_id = ?", MAPPER, userAccountId).stream().findFirst();
    }

    @Override
    public boolean existsByMerchantCode(String merchantCode) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM merchant_profile WHERE merchant_code = ?", Long.class, merchantCode);
        return n != null && n > 0;
    }

    @Override
    public List<MerchantSummary> findSummaries(PageQuery page) {
        return jdbc.query("""
                SELECT u.id, m.business_name, u.email, m.merchant_code, u.created_at
                FROM merchant_profile m JOIN user_account u ON u.id = m.user_account_id
                ORDER BY u.created_at, u.id LIMIT ? OFFSET ?""",
                (rs, i) -> new MerchantSummary(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)),
                page.size(), page.offset());
    }
}
