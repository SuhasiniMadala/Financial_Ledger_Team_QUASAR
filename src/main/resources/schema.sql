-- Financial Ledger schema (Team QUASAR). Implements docs/architecture/database-schema.md.
-- Statements are separated by '@@' (spring.sql.init.separator) because trigger bodies contain ';'.
-- Additions to the Stage 2 design are marked [S3] and explained in docs/architecture/database-schema.md.

CREATE TABLE IF NOT EXISTS user_account (
    id            TEXT PRIMARY KEY,
    email         TEXT NOT NULL COLLATE NOCASE,
    password_hash TEXT NOT NULL,
    display_name  TEXT NOT NULL,
    role          TEXT NOT NULL CHECK (role IN ('USER', 'MERCHANT', 'ADMIN')),
    created_at    TEXT NOT NULL,
    CONSTRAINT uk_user_email UNIQUE (email)
)@@

CREATE TABLE IF NOT EXISTS merchant_profile (
    id              TEXT PRIMARY KEY,
    user_account_id TEXT NOT NULL,
    business_name   TEXT NOT NULL,
    merchant_code   TEXT NOT NULL,
    CONSTRAINT uk_merchant_user UNIQUE (user_account_id),
    CONSTRAINT uk_merchant_code UNIQUE (merchant_code),
    FOREIGN KEY (user_account_id) REFERENCES user_account(id) ON DELETE RESTRICT
)@@

CREATE TABLE IF NOT EXISTS account (
    id            TEXT PRIMARY KEY,
    owner_id      TEXT,
    account_type  TEXT NOT NULL CHECK (account_type IN ('USER_WALLET', 'MERCHANT_WALLET', 'SYSTEM_FUNDING')),
    currency      TEXT NOT NULL CHECK (length(currency) = 3),
    balance_minor INTEGER NOT NULL DEFAULT 0 CHECK (typeof(balance_minor) = 'integer' AND balance_minor >= 0),
    status        TEXT NOT NULL CHECK (status IN ('ACTIVE', 'FROZEN', 'DEACTIVATED')),
    created_at    TEXT NOT NULL, -- [S3]
    CONSTRAINT uk_owner_currency UNIQUE (owner_id, currency),
    FOREIGN KEY (owner_id) REFERENCES user_account(id) ON DELETE RESTRICT,
    CONSTRAINT chk_account_ownership CHECK (
        (account_type = 'SYSTEM_FUNDING' AND owner_id IS NULL) OR
        (account_type IN ('USER_WALLET', 'MERCHANT_WALLET') AND owner_id IS NOT NULL)
    )
)@@

CREATE TABLE IF NOT EXISTS transaction_record (
    id                     TEXT PRIMARY KEY,
    type                   TEXT NOT NULL CHECK (type IN ('TEST_FUNDING', 'TRANSFER', 'MERCHANT_PAYMENT', 'REVERSAL')),
    status                 TEXT NOT NULL CHECK (status IN ('COMPLETED', 'FAILED')),
    amount_minor           INTEGER NOT NULL CHECK (typeof(amount_minor) = 'integer' AND amount_minor > 0),
    currency               TEXT NOT NULL,
    initiated_by           TEXT NOT NULL,
    source_account_id      TEXT, -- [S3] needed to list FAILED attempts, which have no entries
    destination_account_id TEXT, -- [S3]
    reversal_of_id         TEXT,
    failure_code           TEXT,
    description            TEXT, -- [S3] transfer description / payment reference / reversal reason
    created_at             TEXT NOT NULL,
    FOREIGN KEY (initiated_by) REFERENCES user_account(id) ON DELETE RESTRICT,
    FOREIGN KEY (source_account_id) REFERENCES account(id) ON DELETE RESTRICT,
    FOREIGN KEY (destination_account_id) REFERENCES account(id) ON DELETE RESTRICT,
    FOREIGN KEY (reversal_of_id) REFERENCES transaction_record(id) ON DELETE RESTRICT,
    CHECK ((status = 'FAILED') = (failure_code IS NOT NULL)),
    CHECK ((type = 'REVERSAL') = (reversal_of_id IS NOT NULL))
)@@

-- [S3] BR-10 enforced by the database: a transaction can be reversed at most once
CREATE UNIQUE INDEX IF NOT EXISTS uq_txn_reversal_of ON transaction_record(reversal_of_id) WHERE reversal_of_id IS NOT NULL@@

CREATE TABLE IF NOT EXISTS ledger_entry (
    sequence_no         INTEGER PRIMARY KEY AUTOINCREMENT,
    id                  TEXT NOT NULL,
    transaction_id      TEXT NOT NULL,
    account_id          TEXT NOT NULL,
    direction           TEXT NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
    amount_minor        INTEGER NOT NULL CHECK (typeof(amount_minor) = 'integer' AND amount_minor > 0),
    balance_after_minor INTEGER NOT NULL CHECK (typeof(balance_after_minor) = 'integer' AND balance_after_minor >= 0),
    created_at          TEXT NOT NULL,
    CONSTRAINT uk_ledger_entry_id UNIQUE (id),
    FOREIGN KEY (transaction_id) REFERENCES transaction_record(id) ON DELETE RESTRICT,
    FOREIGN KEY (account_id) REFERENCES account(id) ON DELETE RESTRICT
)@@

CREATE TABLE IF NOT EXISTS idempotency_record (
    principal_id    TEXT NOT NULL,
    idem_key        TEXT NOT NULL,
    request_hash    TEXT NOT NULL,
    response_status INTEGER NOT NULL CHECK (response_status IN (201, 422)),
    response_body   TEXT NOT NULL,
    transaction_id  TEXT,
    expires_at      TEXT NOT NULL,
    created_at      TEXT NOT NULL,
    PRIMARY KEY (principal_id, idem_key),
    FOREIGN KEY (principal_id) REFERENCES user_account(id) ON DELETE CASCADE,
    FOREIGN KEY (transaction_id) REFERENCES transaction_record(id) ON DELETE SET NULL
)@@

CREATE TABLE IF NOT EXISTS wallet_status_change (
    id         TEXT PRIMARY KEY,
    account_id TEXT NOT NULL,
    old_status TEXT NOT NULL,
    new_status TEXT NOT NULL,
    changed_by TEXT NOT NULL,
    reason     TEXT NOT NULL,
    created_at TEXT NOT NULL,
    FOREIGN KEY (account_id) REFERENCES account(id) ON DELETE RESTRICT,
    FOREIGN KEY (changed_by) REFERENCES user_account(id) ON DELETE RESTRICT
)@@

-- [S3] Log of reconciliation runs (not financial data; feeds FR-ADM-07 "latest reconciliation result")
CREATE TABLE IF NOT EXISTS reconciliation_run (
    id                TEXT PRIMARY KEY,
    run_at            TEXT NOT NULL,
    result            TEXT NOT NULL CHECK (result IN ('PASS', 'FAIL')),
    discrepancy_count INTEGER NOT NULL,
    report_json       TEXT NOT NULL
)@@

CREATE INDEX IF NOT EXISTS idx_ledger_account_seq ON ledger_entry(account_id, sequence_no)@@
CREATE INDEX IF NOT EXISTS idx_ledger_txn ON ledger_entry(transaction_id)@@
CREATE INDEX IF NOT EXISTS idx_txn_source ON transaction_record(source_account_id, created_at)@@
CREATE INDEX IF NOT EXISTS idx_txn_destination ON transaction_record(destination_account_id, created_at)@@
CREATE INDEX IF NOT EXISTS idx_txn_initiated ON transaction_record(initiated_by, created_at)@@
CREATE INDEX IF NOT EXISTS idx_txn_type_status ON transaction_record(type, status, created_at)@@
CREATE INDEX IF NOT EXISTS idx_idem_expires ON idempotency_record(expires_at)@@

-- FR-LED-05 / NFR-SAFE-06: committed ledger data is append-only, enforced by the database itself
CREATE TRIGGER IF NOT EXISTS prevent_ledger_entry_update BEFORE UPDATE ON ledger_entry
BEGIN
    SELECT RAISE(ABORT, 'ILLEGAL_OPERATION: ledger entries are immutable');
END@@

CREATE TRIGGER IF NOT EXISTS prevent_ledger_entry_delete BEFORE DELETE ON ledger_entry
BEGIN
    SELECT RAISE(ABORT, 'ILLEGAL_OPERATION: ledger entries cannot be deleted');
END@@

CREATE TRIGGER IF NOT EXISTS prevent_transaction_update BEFORE UPDATE ON transaction_record
BEGIN
    SELECT RAISE(ABORT, 'ILLEGAL_OPERATION: transactions are immutable');
END@@

CREATE TRIGGER IF NOT EXISTS prevent_transaction_delete BEFORE DELETE ON transaction_record
BEGIN
    SELECT RAISE(ABORT, 'ILLEGAL_OPERATION: transactions cannot be deleted');
END@@

CREATE TRIGGER IF NOT EXISTS prevent_status_change_update BEFORE UPDATE ON wallet_status_change
BEGIN
    SELECT RAISE(ABORT, 'ILLEGAL_OPERATION: wallet status changes are immutable');
END@@

CREATE TRIGGER IF NOT EXISTS prevent_status_change_delete BEFORE DELETE ON wallet_status_change
BEGIN
    SELECT RAISE(ABORT, 'ILLEGAL_OPERATION: wallet status changes cannot be deleted');
END@@

-- FR-LED-03: a FAILED transaction can never receive ledger entries
CREATE TRIGGER IF NOT EXISTS prevent_failed_transaction_entries BEFORE INSERT ON ledger_entry
FOR EACH ROW WHEN (SELECT status FROM transaction_record WHERE id = NEW.transaction_id) = 'FAILED'
BEGIN
    SELECT RAISE(ABORT, 'ILLEGAL_OPERATION: FAILED transactions must have zero ledger entries');
END@@
