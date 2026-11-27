package com.quasar.ledger.module.transfer;

import static org.assertj.core.api.Assertions.assertThat;

import com.quasar.ledger.support.ApiClient;
import com.quasar.ledger.support.ApiClient.Response;
import com.quasar.ledger.support.ApiTestBase;
import com.quasar.ledger.support.LedgerSteps;
import com.quasar.ledger.support.TransferSteps;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SRS 4.5 concurrency (FR-CON-01..08, NFR-SAFE-03, NFR-SAFE-05, TQ-4). Requests are fired from many threads at
 * the same instant against the real server and a real SQLite file in WAL mode. Every test checks the stored
 * balances (not just HTTP codes) and ends with a reconciliation PASS (NFR-SAFE-02).
 */
class ConcurrencyApiTest extends ApiTestBase {

    /** Runs all tasks at once (released together by a latch) and returns their responses. */
    private List<Response> concurrently(List<Callable<Response>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(tasks.size(), 64));
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Response>> futures = new ArrayList<>();
            for (Callable<Response> task : tasks) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<Response> results = new ArrayList<>();
            for (Future<Response> f : futures) {
                results.add(f.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private ApiClient client() {
        return new ApiClient("http://localhost:" + port, objectMapper);
    }

    @Test
    @DisplayName("SRS 4.5.2 #1: Rs 500 balance, two Rs 400 transfers at once -> exactly one succeeds, never negative")
    void twoConcurrentOverdraftsOnlyOneWins() throws Exception {
        String admin = adminToken();
        TestUser asha = registerUser("Asha Rao");
        String a = createWallet(asha.token(), "INR");
        String b = createWallet(registerUser("Ravi Kumar").token(), "INR");
        LedgerSteps.fundOk(api, admin, a, 50_000);
        List<Callable<Response>> tasks = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            tasks.add(() -> TransferSteps.transfer(client(), asha.token(), a, b, 40_000, newKey()));
        }
        List<Response> results = concurrently(tasks);
        long ok = results.stream().filter(r -> r.status() == 201).count();
        assertThat(results).allSatisfy(r -> assertThat(r.status()).isIn(201, 422, 503));
        assertThat(ok).isEqualTo(1);
        assertThat(results.stream().filter(r -> r.status() == 422).map(Response::code)).containsOnly("INSUFFICIENT_FUNDS");
        assertThat(storedBalance(a)).isEqualTo(10_000);
        assertThat(storedBalance(b)).isEqualTo(40_000);
        LedgerSteps.assertReconciles(api, admin);
    }

    @Test
    @DisplayName("NFR-SAFE-03: 100 concurrent Rs 10 debits on Rs 500 -> at most 50 succeed and balance = 50000 - 1000 x successes")
    void hundredConcurrentDebits() throws Exception {
        String admin = adminToken();
        TestUser asha = registerUser("Asha Rao");
        String a = createWallet(asha.token(), "INR");
        String b = createWallet(registerUser("Ravi Kumar").token(), "INR");
        LedgerSteps.fundOk(api, admin, a, 50_000);
        List<Callable<Response>> tasks = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            tasks.add(() -> TransferSteps.transfer(client(), asha.token(), a, b, 1_000, newKey()));
        }
        List<Response> results = concurrently(tasks);
        long successes = results.stream().filter(r -> r.status() == 201).count();
        assertThat(results).allSatisfy(r -> assertThat(r.status()).isIn(201, 422, 503));
        assertThat(successes).isLessThanOrEqualTo(50);
        assertThat(storedBalance(a)).isEqualTo(50_000 - 1_000 * successes).isNotNegative();
        assertThat(storedBalance(b)).isEqualTo(1_000 * successes);
        assertThat(count("SELECT COUNT(*) FROM transaction_record WHERE status = 'COMPLETED' AND type = 'TRANSFER' "
                + "AND source_account_id = ?", a)).isEqualTo(successes);
        LedgerSteps.assertReconciles(api, admin);
    }

    @Test
    @DisplayName("NFR-SAFE-03 / SRS 4.5.2 #2: 50 incoming + 50 outgoing at once give the exact expected balance")
    void incomingAndOutgoingTogether() throws Exception {
        String admin = adminToken();
        TestUser asha = registerUser("Asha Rao");
        TestUser carol = registerUser("Carol");
        String a = createWallet(asha.token(), "INR");
        String b = createWallet(registerUser("Ravi Kumar").token(), "INR");
        String c = createWallet(carol.token(), "INR");
        LedgerSteps.fundOk(api, admin, a, 50_000);
        LedgerSteps.fundOk(api, admin, c, 50_000);
        List<Callable<Response>> tasks = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            tasks.add(() -> TransferSteps.transfer(client(), carol.token(), c, a, 100, "in-" + newKey()));
            tasks.add(() -> TransferSteps.transfer(client(), asha.token(), a, b, 100, "out-" + newKey()));
        }
        List<Response> results = concurrently(tasks);
        long in = results.stream().filter(r -> r.status() == 201 && c.equals(r.text("sourceAccountId"))).count();
        long out = results.stream().filter(r -> r.status() == 201 && a.equals(r.text("sourceAccountId"))).count();
        assertThat(results).allSatisfy(r -> assertThat(r.status()).isIn(201, 503));
        assertThat(storedBalance(a)).isEqualTo(50_000 + 100 * in - 100 * out);
        LedgerSteps.assertReconciles(api, admin);
    }

    @Test
    @DisplayName("FR-CON-08 / NFR-SAFE-05: 10 concurrent and 10 sequential repeats of one key -> one transaction, identical bodies")
    void sameKeyConcurrentlyExecutesOnce() throws Exception {
        String admin = adminToken();
        TestUser asha = registerUser("Asha Rao");
        String a = createWallet(asha.token(), "INR");
        String b = createWallet(registerUser("Ravi Kumar").token(), "INR");
        LedgerSteps.fundOk(api, admin, a, 100_000);
        String key = newKey();
        List<Callable<Response>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tasks.add(() -> TransferSteps.transfer(client(), asha.token(), a, b, 10_000, key));
        }
        List<Response> results = new ArrayList<>(concurrently(tasks));
        for (int i = 0; i < 10; i++) {
            results.add(TransferSteps.transfer(api, asha.token(), a, b, 10_000, key));
        }
        Set<String> bodies = new HashSet<>();
        for (Response r : results) {
            assertThat(r.status()).as("%s", r).isIn(201, 503);
            if (r.status() == 201) {
                bodies.add(r.raw().body());
            }
        }
        assertThat(bodies).hasSize(1);
        assertThat(results.stream().filter(r -> r.status() == 201 && r.header("Idempotent-Replayed") == null).count()).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM transaction_record WHERE type = 'TRANSFER' AND source_account_id = ?", a)).isEqualTo(1);
        assertThat(storedBalance(a)).isEqualTo(90_000);
        assertThat(storedBalance(b)).isEqualTo(10_000);
        LedgerSteps.assertReconciles(api, admin);
    }
}
