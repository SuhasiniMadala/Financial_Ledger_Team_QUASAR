package com.quasar.ledger.module.transfer.service;

import com.quasar.ledger.common.api.ErrorCode;
import com.quasar.ledger.module.wallet.model.Account;
import java.util.Optional;

/**
 * The business rules shared by transfers and merchant payments (FR-TRF-11/12, FR-PAY-02), checked in this order:
 * INVALID_DESTINATION (BR-04), SAME_WALLET (BR-06), CURRENCY_MISMATCH (BR-03), WALLET_NOT_ACTIVE (BR-07),
 * INSUFFICIENT_FUNDS (BR-05). They run inside the write-locked transaction, so the balance read here cannot
 * change before the debit (FR-CON-03).
 */
public final class TransferRules {

    private TransferRules() {
    }

    public record Violation(ErrorCode code, String message) {
    }

    /** @param destination the payee account if it is a valid kind of payee for this operation, otherwise null */
    public static Optional<Violation> check(Account source, Account destination, long amountMinor) {
        if (destination == null) {
            return Optional.of(new Violation(ErrorCode.INVALID_DESTINATION, "The destination is not a valid payee for this operation"));
        }
        if (destination.id().equals(source.id())) {
            return Optional.of(new Violation(ErrorCode.SAME_WALLET, "Source and destination must be different wallets"));
        }
        if (!destination.currency().equals(source.currency())) {
            return Optional.of(new Violation(ErrorCode.CURRENCY_MISMATCH,
                    "Source is " + source.currency() + " but destination is " + destination.currency() + "; there is no conversion"));
        }
        if (!source.isActive() || !destination.isActive()) {
            return Optional.of(new Violation(ErrorCode.WALLET_NOT_ACTIVE, "Both wallets must be ACTIVE"));
        }
        if (source.balanceMinor() < amountMinor) {
            return Optional.of(new Violation(ErrorCode.INSUFFICIENT_FUNDS, "The source wallet balance is too low"));
        }
        return Optional.empty();
    }
}
