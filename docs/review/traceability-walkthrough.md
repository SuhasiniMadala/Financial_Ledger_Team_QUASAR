# Traceability Walkthrough (SDLC 7 Stage 4, SE-5)

Owner: Geethika M. Presented live; time about 4 minutes. Every link below is a real file, ID, commit or PR and can be opened by anyone with the repository.

## Requirement chosen

**FR-CON-08 / FR-TRF-05: a retried or concurrent request with the same Idempotency-Key executes at most once and returns the stored response** (parent requirement FR-2 "Idempotent transfers", NFR-SAFE-05). It is the guarantee that a network retry never double-charges, which is the core promise of the project (SRS 1.4).

## The chain

| # | Link | Where | What to show |
|---|---|---|---|
| 1 | **Brain note (distilled)** | `brain/resources/api-idempotency-and-retry-semantics.md` - [[api-idempotency-and-retry-semantics]]. Created by Akshaya A (commit `1c9cdd8`, 2026-11-26), refined by Geethika M (`be87709`, PR #24) | why a client cannot know whether a timed-out request committed, and why the key + fingerprint + stored response must be saved in the same transaction |
| 2 | **SRS requirement** | `Financial_Wallet_Ledger_SRS (2) (1).docx` v1.2, section 4.3.3 (FR-TRF-04..08) and 4.5.3 (FR-CON-08); scenarios 4.3.2 #2/#3 and 4.5.2 #3 | "For a reused key with the same fingerprint, the system shall return the stored status and body with Idempotent-Replayed: true and create no new transaction" |
| 3 | **GitHub issues** | #9 "Ledger engine, idempotency, test funding and reversals (... FR-TRF-02..08 ...)" (closed by PR #24); #14 "Concurrency, busy-timeout, durability and performance tests" (closed by PR #25) | issue text and the PR that closed it |
| 4 | **ADR** | `docs/adr/ADR-005-idempotency-key-pattern.md` (cites [[api-idempotency-and-retry-semantics]]); refined by `docs/adr/ADR-009-explicit-begin-immediate-transaction-manager.md` (lock before reading the key) | the decision: key scoped to the caller, SHA-256 fingerprint, stored response committed atomically with the outcome |
| 5 | **Code** | `src/main/java/com/quasar/ledger/module/idempotency/service/IdempotencyService.java`, method `execute` (lines 55-78): inside ONE write transaction, `repository.find(principal, key)` -> same hash: return stored response with `replayed = true`; different hash: `IDEMPOTENCY_KEY_REUSED`; otherwise run the operation and `repository.insert(...)` the record before commit. Introduced in commit `f5a8770` (Geethika M, PR #24). The write lock is taken first by `config/SqliteImmediateTransactionManager.java` (ADR-009) | the four branches in 24 lines |
| 6 | **Automated tests** | `src/test/java/com/quasar/ledger/module/transfer/ConcurrencyApiTest.java` - `sameKeyConcurrentlyExecutesOnce` ("FR-CON-08 / NFR-SAFE-05: 10 concurrent and 10 sequential repeats of one key -> one transaction, identical bodies"), commit `4d6206d` (Suhasini M, PR #25). Also `TransferApiTest.replay` (SRS 4.3.2 #2), `TransferApiTest.reusedKey` (#3), `IdempotencyRulesTest` (key format, fingerprint) | run it live: `mvn test -Dtest=ConcurrencyApiTest#sameKeyConcurrentlyExecutesOnce -Djacoco.skip=true` -> `Tests run: 1, Failures: 0` |
| 7 | **PRs / commits** | PR #24 (Geethika2611, merge `3c5c491`), PR #25 (SuhasiniMadala, merge `594e58f`) | review and CI checks on each PR |
| 8 | **Skill** | `brain/skills/simulate-transfer/SKILL.md` step 3 (send a transfer, resend identically: same transaction ID, replay header, one transaction) | the step-3 line of a recorded run in `docs/testing/stage-4-skill-evaluation.md` |
| 9 | **Stage 4 client (UI-04)** | `frontend/src/hooks/useFinancialSubmit.ts` (same key and body on retry after network error, timeout or 503); tests `frontend/src/hooks/useFinancialSubmit.test.ts` and the real-backend test `frontend/tests/integration/client-contract.int.test.ts` ("test funding is idempotent..."); ADR-011, [[react-client-money-and-idempotency]] | the client half of the same guarantee |

## How to present it (in this order)

1. Open the brain note (Obsidian graph view shows its links to ADR-005 and ADR-009).
2. Open the SRS at FR-TRF-05 and FR-CON-08.
3. Open issue #9 on GitHub and follow "closed by" to PR #24.
4. Open ADR-005, point at the "Second Brain Reference" line.
5. Open `IdempotencyService.execute` and read the four branches.
6. Run the test command in row 6 and show it pass.
7. Finish in the client: Transfer screen, explain that Retry re-sends the same key (row 9).

## Check before presenting

- [ ] `mvn test -Dtest=ConcurrencyApiTest#sameKeyConcurrentlyExecutesOnce -Djacoco.skip=true` passes on the final `main`.
- [ ] Line numbers in row 5 still match after the final merge (`IdempotencyService.java`).
- [ ] Issue #9 / #14 and PR #24 / #25 links open on GitHub.
