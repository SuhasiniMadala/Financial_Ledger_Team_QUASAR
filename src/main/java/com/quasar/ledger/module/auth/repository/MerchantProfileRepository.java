package com.quasar.ledger.module.auth.repository;

import com.quasar.ledger.common.api.PageQuery;
import com.quasar.ledger.module.auth.model.MerchantProfile;
import com.quasar.ledger.module.auth.model.MerchantSummary;
import java.util.List;
import java.util.Optional;

/** Persistence port for merchant profiles (NFR-MAIN-03). */
public interface MerchantProfileRepository {

    void insert(MerchantProfile profile);

    Optional<MerchantProfile> findByMerchantCode(String merchantCode);

    Optional<MerchantProfile> findByUserAccountId(String userAccountId);

    boolean existsByMerchantCode(String merchantCode);

    List<MerchantSummary> findSummaries(PageQuery page);
}
