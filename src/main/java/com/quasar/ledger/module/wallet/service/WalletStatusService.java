package com.quasar.ledger.module.wallet.service;

import com.quasar.ledger.common.api.ErrorCode;
import com.quasar.ledger.common.exception.ApiException;
import com.quasar.ledger.common.id.IdGenerator;
import com.quasar.ledger.common.id.IdKind;
import com.quasar.ledger.common.util.Timestamps;
import com.quasar.ledger.module.wallet.model.Account;
import com.quasar.ledger.module.wallet.model.AccountStatus;
import com.quasar.ledger.module.wallet.model.AccountType;
import com.quasar.ledger.module.wallet.model.WalletStatusChange;
import com.quasar.ledger.module.wallet.repository.AccountRepository;
import com.quasar.ledger.module.wallet.repository.WalletStatusChangeRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin wallet status changes (FR-ADM-04/05/06). Allowed: ACTIVE->FROZEN (freeze), FROZEN->ACTIVE (unfreeze),
 * ACTIVE/FROZEN->DEACTIVATED (deactivate, final). Status changes never touch balances or the ledger.
 */
@Service
public class WalletStatusService {

    public enum Action { FREEZE, UNFREEZE, DEACTIVATE }

    private final AccountRepository accounts;
    private final WalletStatusChangeRepository changes;
    private final IdGenerator ids;
    private final Clock clock;

    public WalletStatusService(AccountRepository accounts, WalletStatusChangeRepository changes, IdGenerator ids, Clock clock) {
        this.accounts = accounts;
        this.changes = changes;
        this.ids = ids;
        this.clock = clock;
    }

    @Transactional
    public Account change(String walletId, Action action, String reason, String adminId) {
        Account wallet = accounts.findById(walletId)
                .filter(a -> a.type() != AccountType.SYSTEM_FUNDING)
                .orElseThrow(() -> ApiException.notFound("Wallet"));
        AccountStatus from = wallet.status();
        AccountStatus to = switch (action) {
            case FREEZE -> requireFrom(from, AccountStatus.FROZEN, AccountStatus.ACTIVE);
            case UNFREEZE -> requireFrom(from, AccountStatus.ACTIVE, AccountStatus.FROZEN);
            case DEACTIVATE -> requireFrom(from, AccountStatus.DEACTIVATED, AccountStatus.ACTIVE, AccountStatus.FROZEN);
        };
        accounts.updateStatus(walletId, to);
        changes.insert(new WalletStatusChange(ids.generate(IdKind.STATUS_CHANGE), walletId, from, to, adminId,
                reason.trim(), Timestamps.format(clock.instant())));
        return new Account(wallet.id(), wallet.ownerId(), wallet.type(), wallet.currency(), wallet.balanceMinor(), to,
                wallet.createdAt());
    }

    private static AccountStatus requireFrom(AccountStatus current, AccountStatus target, AccountStatus... allowed) {
        for (AccountStatus s : allowed) {
            if (s == current) {
                return target;
            }
        }
        throw new ApiException(ErrorCode.INVALID_STATE_TRANSITION,
                "Cannot change wallet status from " + current + " to " + target);
    }
}
