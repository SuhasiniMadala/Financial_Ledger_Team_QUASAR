package com.quasar.ledger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArchitectureInvariantsTest {

    // --- Invariant 1: COMPLETED vs FAILED Ledger Entries ---

    record MockEntry(String direction, long amountMinor) {}
    record MockTransaction(String id, String status, List<MockEntry> entries) {}

    private void validateTransactionLedgerEntries(MockTransaction txn) {
        if ("FAILED".equals(txn.status())) {
            if (!txn.entries().isEmpty()) {
                throw new IllegalStateException("FAILED transactions must have zero ledger entries (FR-LED-01)");
            }
            return;
        }

        if ("COMPLETED".equals(txn.status())) {
            if (txn.entries().size() < 2) {
                throw new IllegalStateException("COMPLETED transactions must have at least 2 entries (FR-LED-01)");
            }
            long totalDebits = txn.entries().stream()
                    .filter(e -> "DEBIT".equals(e.direction()))
                    .mapToLong(MockEntry::amountMinor)
                    .sum();
            long totalCredits = txn.entries().stream()
                    .filter(e -> "CREDIT".equals(e.direction()))
                    .mapToLong(MockEntry::amountMinor)
                    .sum();
            if (totalDebits != totalCredits) {
                throw new IllegalStateException("Debits (" + totalDebits + ") must equal Credits (" + totalCredits + ") (FR-LED-02)");
            }
        }
    }

    @Test
    @DisplayName("Invariant 1: COMPLETED transaction with balanced debits and credits passes")
    void testCompletedBalancedTransactionPasses() {
        MockTransaction txn = new MockTransaction("T-501", "COMPLETED", List.of(
                new MockEntry("DEBIT", 1000L),
                new MockEntry("CREDIT", 1000L)
        ));
        assertDoesNotThrow(() -> validateTransactionLedgerEntries(txn));
    }

    @Test
    @DisplayName("Invariant 1: COMPLETED transaction with unbalanced debits/credits throws error")
    void testCompletedUnbalancedTransactionFails() {
        MockTransaction txn = new MockTransaction("T-502", "COMPLETED", List.of(
                new MockEntry("DEBIT", 1000L),
                new MockEntry("CREDIT", 999L)
        ));
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validateTransactionLedgerEntries(txn));
        assertTrue(ex.getMessage().contains("must equal Credits"));
    }

    @Test
    @DisplayName("Invariant 1: FAILED transaction with zero entries passes")
    void testFailedTransactionWithZeroEntriesPasses() {
        MockTransaction txn = new MockTransaction("T-503", "FAILED", List.of());
        assertDoesNotThrow(() -> validateTransactionLedgerEntries(txn));
    }

    @Test
    @DisplayName("Invariant 1: FAILED transaction with ledger entries throws error")
    void testFailedTransactionWithEntriesFails() {
        MockTransaction txn = new MockTransaction("T-504", "FAILED", List.of(
                new MockEntry("DEBIT", 1000L)
        ));
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validateTransactionLedgerEntries(txn));
        assertTrue(ex.getMessage().contains("FAILED transactions must have zero ledger entries"));
    }

    // --- Invariant 2: User Role vs Wallet Type Enforcement ---

    enum Role { USER, MERCHANT, ADMIN }
    enum AccountType { USER_WALLET, MERCHANT_WALLET, SYSTEM_FUNDING }

    private void validateWalletOwnership(Role ownerRole, AccountType requestedType) {
        if (requestedType == AccountType.USER_WALLET && ownerRole != Role.USER) {
            throw new IllegalArgumentException("Only USER accounts may own a USER_WALLET (FR-WAL-01)");
        }
        if (requestedType == AccountType.MERCHANT_WALLET && ownerRole != Role.MERCHANT) {
            throw new IllegalArgumentException("Only MERCHANT accounts may own a MERCHANT_WALLET (FR-WAL-02)");
        }
        if (requestedType == AccountType.SYSTEM_FUNDING) {
            throw new IllegalArgumentException("SYSTEM_FUNDING accounts cannot be owned by user accounts (ACC-01)");
        }
    }

    @Test
    @DisplayName("Invariant 2: USER creating USER_WALLET and MERCHANT creating MERCHANT_WALLET succeed")
    void testValidWalletRoleOwnership() {
        assertDoesNotThrow(() -> validateWalletOwnership(Role.USER, AccountType.USER_WALLET));
        assertDoesNotThrow(() -> validateWalletOwnership(Role.MERCHANT, AccountType.MERCHANT_WALLET));
    }

    @Test
    @DisplayName("Invariant 2: USER creating MERCHANT_WALLET fails")
    void testUserCannotCreateMerchantWallet() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validateWalletOwnership(Role.USER, AccountType.MERCHANT_WALLET));
        assertTrue(ex.getMessage().contains("Only MERCHANT accounts may own a MERCHANT_WALLET"));
    }

    @Test
    @DisplayName("Invariant 2: MERCHANT creating USER_WALLET fails")
    void testMerchantCannotCreateUserWallet() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validateWalletOwnership(Role.MERCHANT, AccountType.USER_WALLET));
        assertTrue(ex.getMessage().contains("Only USER accounts may own a USER_WALLET"));
    }

    @Test
    @DisplayName("Invariant 2: No user or merchant can own SYSTEM_FUNDING account")
    void testNoOwnerForSystemFunding() {
        assertThrows(IllegalArgumentException.class, () -> validateWalletOwnership(Role.USER, AccountType.SYSTEM_FUNDING));
        assertThrows(IllegalArgumentException.class, () -> validateWalletOwnership(Role.MERCHANT, AccountType.SYSTEM_FUNDING));
        assertThrows(IllegalArgumentException.class, () -> validateWalletOwnership(Role.ADMIN, AccountType.SYSTEM_FUNDING));
    }
}
