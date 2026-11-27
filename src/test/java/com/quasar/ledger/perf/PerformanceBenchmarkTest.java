package com.quasar.ledger.perf;

import static org.assertj.core.api.Assertions.assertThat;

import com.quasar.ledger.module.ledger.model.NewTransaction;
import com.quasar.ledger.module.ledger.model.Posting;
import com.quasar.ledger.module.ledger.model.TransactionType;
import com.quasar.ledger.module.ledger.service.LedgerService;
import com.quasar.ledger.support.ApiClient;
import com.quasar.ledger.support.ApiClient.Response;
import com.quasar.ledger.support.ApiTestBase;
import com.quasar.ledger.support.LedgerSteps;
import com.quasar.ledger.support.TransferSteps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * NFR-PERF-01/02/03 benchmark. Excluded from the normal build (tag "perf"); run it with
 * {@code mvn test -Dtest.excludedGroups=none -Dgroups=perf -Djacoco.skip=true}. Optional: -Dperf.sustainSeconds=300.
 * The SRS names JMeter; this is an equivalent Java load generator (10 concurrent HTTP clients) - see the test report.
 * Results are written to target/perf/perf-results.md.
 */
@Tag("perf")
@TestPropertySource(properties = "ledger.test.isolation=perf")
class PerformanceBenchmarkTest extends ApiTestBase {

    private static final int CLIENTS = 10;
    private static final int SEED_TRANSFERS = 50_000; // + funding entries => more than 1,00,000 ledger entries

    @Autowired
    private LedgerService ledger;

    @Autowired
    private TransactionTemplate writeTransaction;

    @Test
    void benchmark() throws Exception {
        String admin = adminToken();
        List<TestUser> users = new ArrayList<>();
        List<String> wallets = new ArrayList<>();
        for (int i = 0; i < CLIENTS * 2; i++) {
            TestUser u = registerUser("Perf " + i);
            users.add(u);
            String w = createWallet(u.token(), "INR");
            wallets.add(w);
            LedgerSteps.fundOk(api, admin, w, 10_000_000);
        }
        long seedStart = System.nanoTime();
        seedLedger(users, wallets);
        long seedMs = (System.nanoTime() - seedStart) / 1_000_000;
        long entries = count("SELECT COUNT(*) FROM ledger_entry");
        assertThat(entries).isGreaterThanOrEqualTo(100_000);

        // NFR-PERF-01: 10 concurrent clients, >= 1,000 transfer requests and >= 1,000 read requests.
        List<Long> writeMs = Collections.synchronizedList(new ArrayList<>());
        List<Long> readMs = Collections.synchronizedList(new ArrayList<>());
        runClients(users, wallets, 100, writeMs, readMs);
        long p95Write = p95(writeMs);
        long p95Read = p95(readMs);

        // NFR-PERF-02: sustained load, then reconciliation must PASS.
        int seconds = Integer.getInteger("perf.sustainSeconds", 300);
        AtomicLong committed = new AtomicLong();
        AtomicLong busy = new AtomicLong();
        AtomicLong total = new AtomicLong();
        long end = System.currentTimeMillis() + seconds * 1000L;
        ExecutorService pool = Executors.newFixedThreadPool(CLIENTS);
        List<Future<?>> workers = new ArrayList<>();
        for (int c = 0; c < CLIENTS; c++) {
            int client = c;
            workers.add(pool.submit(() -> {
                ApiClient http = new ApiClient("http://localhost:" + port, objectMapper);
                while (System.currentTimeMillis() < end) {
                    Response r = TransferSteps.transfer(http, users.get(client).token(), wallets.get(client),
                            wallets.get(client + CLIENTS), 1, newKey());
                    total.incrementAndGet();
                    if (r.status() == 201) {
                        committed.incrementAndGet();
                    } else if (r.status() == 503) {
                        busy.incrementAndGet();
                    }
                }
                return null;
            }));
        }
        for (Future<?> w : workers) {
            w.get();
        }
        pool.shutdown();
        double perSecond = committed.get() / (double) seconds;
        double busyPercent = total.get() == 0 ? 0 : 100.0 * busy.get() / total.get();

        // NFR-PERF-03: reconciliation over >= 1,00,000 entries within 30 s.
        long reconStart = System.nanoTime();
        Response recon = LedgerSteps.reconcile(api, admin);
        long reconMs = (System.nanoTime() - reconStart) / 1_000_000;

        String report = String.format("""
                # Performance benchmark results (NFR-PERF-01..03)

                | Measure | Target | Result |
                |---|---|---|
                | Ledger entries before load | >= 1,00,000 | %d (seeded in %d ms) |
                | p95 latency, transfers (10 clients, %d requests) | <= 1000 ms | %d ms |
                | p95 latency, reads (10 clients, %d requests) | <= 500 ms | %d ms |
                | Sustained committed transfers/s over %d s | >= 10 | %.1f (%d committed of %d requests) |
                | HTTP 503 rate during sustained load | < 1%% | %.3f%% (%d) |
                | Reconciliation after load | PASS | %s |
                | Reconciliation time (%d entries) | <= 30 s | %d ms |
                """, entries, seedMs, writeMs.size(), p95Write, readMs.size(), p95Read, seconds, perSecond, committed.get(),
                total.get(), busyPercent, busy.get(), recon.text("result"), recon.body().get("checked").get("entries").asLong(), reconMs);
        Path out = Files.createDirectories(Path.of("target", "perf")).resolve("perf-results.md");
        Files.writeString(out, report);
        System.out.println(report);

        assertThat(p95Write).isLessThanOrEqualTo(1000);
        assertThat(p95Read).isLessThanOrEqualTo(500);
        assertThat(perSecond).isGreaterThanOrEqualTo(10.0);
        assertThat(busyPercent).isLessThan(1.0);
        assertThat(recon.text("result")).isEqualTo("PASS");
        assertThat(reconMs).isLessThanOrEqualTo(30_000);
    }

    /** Seeds history straight through the ledger engine in large batches (one write transaction per batch). */
    private void seedLedger(List<TestUser> users, List<String> wallets) {
        int batch = 5_000;
        for (int done = 0; done < SEED_TRANSFERS; done += batch) {
            int start = done;
            writeTransaction.executeWithoutResult(s -> {
                for (int i = start; i < start + batch; i++) {
                    int from = i % wallets.size();
                    int to = (i + 1) % wallets.size();
                    ledger.postCompleted(new NewTransaction(TransactionType.TRANSFER, 1, "INR", users.get(from).accountId(),
                            wallets.get(from), wallets.get(to), null, null),
                            List.of(Posting.debit(wallets.get(from), 1), Posting.credit(wallets.get(to), 1)));
                }
            });
        }
    }

    private void runClients(List<TestUser> users, List<String> wallets, int perClient, List<Long> writeMs, List<Long> readMs)
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(CLIENTS);
        List<Future<?>> futures = new ArrayList<>();
        for (int c = 0; c < CLIENTS; c++) {
            int client = c;
            futures.add(pool.submit(() -> {
                ApiClient http = new ApiClient("http://localhost:" + port, objectMapper);
                for (int i = 0; i < perClient; i++) {
                    long t0 = System.nanoTime();
                    Response w = TransferSteps.transfer(http, users.get(client).token(), wallets.get(client),
                            wallets.get(client + CLIENTS), 1, newKey());
                    writeMs.add((System.nanoTime() - t0) / 1_000_000);
                    assertThat(w.status()).isIn(201, 503);
                    long t1 = System.nanoTime();
                    Response r = http.get("/api/v1/transactions?size=20", users.get(client).token());
                    readMs.add((System.nanoTime() - t1) / 1_000_000);
                    assertThat(r.status()).isEqualTo(200);
                }
                return null;
            }));
        }
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();
    }

    private static long p95(List<Long> samples) {
        List<Long> sorted = new ArrayList<>(samples);
        Collections.sort(sorted);
        return sorted.get((int) Math.ceil(sorted.size() * 0.95) - 1);
    }
}
