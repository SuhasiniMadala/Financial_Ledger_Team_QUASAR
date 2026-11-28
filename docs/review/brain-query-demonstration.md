# brain-query - Live Demonstration (SDLC 1 workflow skills, SDLC 7 Stage 4)

Owner: Geethika M (refined the vault and wrote the brain validator in Stage 3, issue #11). Time: about 4 minutes.

`brain-query` is an agent skill (`brain/skills/brain-query/SKILL.md`): an AI assistant answers a question using **only** the vault, the SRS and the repository, and cites every note it used. There is no script; the skill is run by asking the assistant in this repository.

## Exact question

> Explain how a wallet transfer moves through the system, which architectural decision governs transaction handling, and which tests verify its correctness.

## How to run it live

1. Open the repository in VS Code with Claude Code (or another assistant that reads `AGENTS.md`).
2. Type: `Use the brain-query skill to answer: Explain how a wallet transfer moves through the system, which architectural decision governs transaction handling, and which tests verify its correctness.`
3. Show the answer on screen. Then open two or three of the cited notes in Obsidian (or VS Code) and show that they say what the answer claims.
4. Paste the full answer below under "Recorded runs".

## How to judge the answer (check live, out loud)

A correct answer uses the skill's output format (`Question / Answer / Evidence / Gaps`) and is supported by these files, which all exist in the repository:

| Claim the answer should contain | Source that proves it |
|---|---|
| A transfer is one SQLite transaction that takes the write lock first (`BEGIN IMMEDIATE`), then checks the idempotency key, then validates and posts | [[sqlite-write-lock-and-driver-behaviour]], [[sqlite-concurrency-and-acid-transactions]]; ADR-002, ADR-009; `config/SqliteImmediateTransactionManager.java`; FR-CON-01/02 |
| Same key + same body replays the stored response; different body is 409 | [[api-idempotency-and-retry-semantics]]; ADR-005; `module/idempotency/service/IdempotencyService.java`; FR-TRF-04..08 |
| Debit source and credit destination with equal amounts; only the Ledger module writes entries and balances; checked before commit | [[double-entry-accounting-principles]]; ADR-003; `module/ledger/service/LedgerService.java`; FR-TRF-14, FR-LED-02, NFR-MAIN-02 |
| Rejections are committed as FAILED transactions with no entries | [[immutable-ledger-and-audit-trails]]; FR-TRF-13, FR-LED-03 |
| Tests | `TransferApiTest` (SRS 4.3.2 scenarios), `ConcurrencyApiTest` (overdraft race, same key 10x), `TransferRollbackTest`, `RollbackFailureInjectionTest`, `LedgerServiceTest`; [[testing-concurrency-and-failure-injection]] |
| Stage 4 client side | [[react-client-money-and-idempotency]]; ADR-011; `frontend/src/hooks/useFinancialSubmit.ts` |

Mark the run **FAIL** if the answer cites a note or file that does not exist, states something the cited note does not say, or invents a requirement ID. "Gaps" must be honest (e.g. if the vault has no note on something).

## Recorded runs

Paste the real output. Do not edit it afterwards except to remove anything sensitive.

### Rehearsal (date, presenter)

```
(paste here)
```

Accuracy check: notes opened and confirmed: ... ; result: PASS / FAIL

### Live review (date, presenter)

```
(paste here)
```

Accuracy check: ... ; result: PASS / FAIL
