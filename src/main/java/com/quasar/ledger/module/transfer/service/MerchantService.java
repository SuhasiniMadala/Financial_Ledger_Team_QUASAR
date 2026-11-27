package com.quasar.ledger.module.transfer.service;

import com.quasar.ledger.common.api.PageQuery;
import com.quasar.ledger.common.api.PageResponse;
import com.quasar.ledger.common.exception.ApiException;
import com.quasar.ledger.common.util.DateRange;
import com.quasar.ledger.common.util.MoneyFormatter;
import com.quasar.ledger.module.auth.model.MerchantProfile;
import com.quasar.ledger.module.auth.repository.MerchantProfileRepository;
import com.quasar.ledger.module.ledger.model.TransactionRecord;
import com.quasar.ledger.module.ledger.model.TransactionStatus;
import com.quasar.ledger.module.ledger.model.TransactionType;
import com.quasar.ledger.module.ledger.repository.TransactionRepository;
import com.quasar.ledger.module.ledger.repository.TransactionSearch;
import com.quasar.ledger.module.transfer.dto.MerchantAccountResponse;
import com.quasar.ledger.module.transfer.dto.MerchantStatisticsResponse;
import com.quasar.ledger.module.transfer.dto.MerchantTransactionView;
import com.quasar.ledger.module.wallet.model.Account;
import com.quasar.ledger.module.wallet.model.AccountType;
import com.quasar.ledger.module.wallet.repository.AccountRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** SRS 4.4 merchant views (FR-PAY-04..07). Merchants can only view; they never send money (BR-04). */
@Service
public class MerchantService {

    static final int DEFAULT_STATISTICS_DAYS = 30;

    private final MerchantProfileRepository merchants;
    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final Clock clock;

    public MerchantService(MerchantProfileRepository merchants, AccountRepository accounts, TransactionRepository transactions,
                           Clock clock) {
        this.merchants = merchants;
        this.accounts = accounts;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MerchantAccountResponse account(String merchantId) {
        MerchantProfile profile = profile(merchantId);
        Account wallet = wallet(merchantId);
        return new MerchantAccountResponse(profile.businessName(), profile.merchantCode(), wallet.id(), wallet.currency(),
                wallet.balanceMinor(), MoneyFormatter.format(wallet.balanceMinor(), wallet.currency()), wallet.status().name());
    }

    @Transactional(readOnly = true)
    public PageResponse<MerchantTransactionView> transactions(String merchantId, DateRange range, PageQuery page) {
        Account wallet = wallet(merchantId);
        TransactionSearch search = new TransactionSearch(wallet.id(), null, TransactionStatus.COMPLETED, range, null, null);
        List<TransactionRecord> rows = transactions.search(search, page);
        Map<String, String> payerNames = accounts.displayNames(rows.stream()
                .map(t -> wallet.id().equals(t.destinationAccountId()) ? t.sourceAccountId() : t.destinationAccountId())
                .collect(Collectors.toSet()));
        List<MerchantTransactionView> items = rows.stream().map(t -> {
            boolean incoming = wallet.id().equals(t.destinationAccountId());
            String payer = incoming ? t.sourceAccountId() : t.destinationAccountId();
            String reference = t.type() == TransactionType.MERCHANT_PAYMENT ? t.description() : null;
            return new MerchantTransactionView(t.id(), t.type().name(), incoming ? "INCOMING" : "OUTGOING", t.amountMinor(),
                    MoneyFormatter.format(t.amountMinor(), t.currency()), t.currency(), payerNames.get(payer), reference,
                    t.reversalOfId(), t.createdAt());
        }).toList();
        return PageResponse.of(items, page, transactions.count(search));
    }

    @Transactional(readOnly = true)
    public MerchantStatisticsResponse statistics(String merchantId, String from, String to) {
        Account wallet = wallet(merchantId);
        DateRange range = DateRange.parseWithDefault(from, to, DEFAULT_STATISTICS_DAYS, clock);
        long[] countAndTotal = transactions.merchantPaymentCountAndTotal(wallet.id(), range);
        long count = countAndTotal[0];
        long total = countAndTotal[1];
        long average = averageHalfEven(total, count);
        return new MerchantStatisticsResponse(range.fromInclusive(), range.toExclusive(), wallet.currency(), count, total,
                MoneyFormatter.format(total, wallet.currency()), average, MoneyFormatter.format(average, wallet.currency()));
    }

    /** FR-PAY-07 / TQ-6: exact decimal division, rounded half-even to a whole paisa. No floating point. */
    static long averageHalfEven(long totalMinor, long count) {
        if (count == 0) {
            return 0;
        }
        return BigDecimal.valueOf(totalMinor).divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_EVEN).longValueExact();
    }

    private MerchantProfile profile(String merchantId) {
        return merchants.findByUserAccountId(merchantId).orElseThrow(() -> ApiException.notFound("Merchant profile"));
    }

    private Account wallet(String merchantId) {
        return accounts.findByOwner(merchantId).stream().filter(a -> a.type() == AccountType.MERCHANT_WALLET).findFirst()
                .orElseThrow(() -> ApiException.notFound("Merchant wallet"));
    }
}
