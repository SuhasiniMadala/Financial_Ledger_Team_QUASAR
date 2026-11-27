package com.quasar.ledger.support;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Starts the real application in a separate JVM process (used for the durability test, NFR-SAFE-04). */
public final class AppProcess implements AutoCloseable {

    public static final String ADMIN_EMAIL = "admin@durability.test";
    public static final String ADMIN_PASSWORD = "test-test-test-test";

    private final Process process;
    private final int port;

    private AppProcess(Process process, int port) {
        this.process = process;
        this.port = port;
    }

    public static AppProcess start(Path databaseFile, Path logFile) throws IOException, InterruptedException {
        int port;
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        List<String> cmd = new ArrayList<>(List.of(java, "-cp", System.getProperty("java.class.path"),
                "com.quasar.ledger.FinancialLedgerApplication",
                "--server.port=" + port,
                "--ledger.db.path=" + databaseFile.toAbsolutePath(),
                "--ledger.jwt.allow-ephemeral-secret=true",
                "--ledger.admin.email=" + ADMIN_EMAIL,
                "--ledger.admin.password=" + ADMIN_PASSWORD));
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).redirectOutput(logFile.toFile()).start();
        AppProcess app = new AppProcess(p, port);
        app.awaitHealthy(Duration.ofSeconds(90));
        return app;
    }

    public String baseUrl() {
        return "http://localhost:" + port;
    }

    /** Simulates a crash: the JVM is terminated immediately, with no shutdown hooks (like kill -9). */
    public void kill() throws InterruptedException {
        process.destroyForcibly();
        process.waitFor();
    }

    private void awaitHealthy(Duration timeout) throws InterruptedException {
        HttpClient http = HttpClient.newHttpClient();
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (!process.isAlive()) {
                throw new IllegalStateException("Application process exited early with code " + process.exitValue());
            }
            try {
                HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(baseUrl() + "/api/v1/health")).build(),
                        HttpResponse.BodyHandlers.ofString());
                if (r.statusCode() == 200) {
                    return;
                }
            } catch (IOException notYet) {
                // server not listening yet
            }
            Thread.sleep(250);
        }
        kill();
        throw new IllegalStateException("Application did not become healthy within " + timeout);
    }

    @Override
    public void close() throws InterruptedException {
        if (process.isAlive()) {
            kill();
        }
    }
}
