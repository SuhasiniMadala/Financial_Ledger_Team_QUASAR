package com.quasar.ledger.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Static checks on the source code itself:
 * TQ-6 / NFR-TEST-06 (no floating point anywhere in production code),
 * NFR-MAIN-02 (only the Ledger module writes entries or balances),
 * NFR-MAIN-01 (no cyclic dependencies between modules; common code does not depend on modules).
 */
class ArchitectureRulesTest {

    private static final Path MAIN = Path.of("src/main/java/com/quasar/ledger");
    private static final Pattern MODULE_IMPORT = Pattern.compile("import com\\.quasar\\.ledger\\.module\\.([a-z]+)\\.");

    private static Map<Path, String> sources() throws IOException {
        Map<Path, String> files = new HashMap<>();
        try (Stream<Path> paths = Files.walk(MAIN)) {
            for (Path p : paths.filter(f -> f.toString().endsWith(".java")).toList()) {
                files.put(MAIN.relativize(p), Files.readString(p));
            }
        }
        assertThat(files).as("source files found").isNotEmpty();
        return files;
    }

    /** Code without comments and string literals, so prose such as "floating-point" is not matched. */
    private static String codeOnly(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "").replaceAll("\"(\\\\.|[^\"\\\\])*\"", "\"\"");
    }

    @Test
    @DisplayName("TQ-6: production code never uses float or double")
    void noFloatingPointTypes() throws IOException {
        List<String> offenders = new ArrayList<>();
        sources().forEach((path, src) -> {
            if (Pattern.compile("\\b(float|double|Float|Double)\\b").matcher(codeOnly(src)).find()) {
                offenders.add(path.toString());
            }
        });
        assertThat(offenders).as("files using float/double").isEmpty();
    }

    @Test
    @DisplayName("NFR-MAIN-02: only the Ledger module inserts ledger entries or updates balances")
    void onlyLedgerWritesEntriesAndBalances() throws IOException {
        List<String> offenders = new ArrayList<>();
        sources().forEach((path, src) -> {
            String p = path.toString().replace('\\', '/');
            boolean ledger = p.startsWith("module/ledger/");
            boolean definesBalanceUpdate = p.startsWith("module/wallet/repository/");
            String code = codeOnly(src);
            if (!ledger && !definesBalanceUpdate && code.contains(".updateBalance(")) {
                offenders.add(p + " calls updateBalance");
            }
            if (!ledger && src.toUpperCase().contains("INSERT INTO LEDGER_ENTRY")) {
                offenders.add(p + " inserts ledger entries");
            }
            if (!ledger && !definesBalanceUpdate && src.toUpperCase().contains("SET BALANCE_MINOR")) {
                offenders.add(p + " updates balance_minor");
            }
        });
        assertThat(offenders).isEmpty();
    }

    @Test
    @DisplayName("NFR-MAIN-01: no cyclic module dependencies; common/ never depends on a module")
    void moduleDependenciesAreAcyclic() throws IOException {
        Map<String, Set<String>> graph = new HashMap<>();
        List<String> commonViolations = new ArrayList<>();
        sources().forEach((path, src) -> {
            String p = path.toString().replace('\\', '/');
            Matcher m = MODULE_IMPORT.matcher(src);
            if (p.startsWith("common/") && m.find()) {
                commonViolations.add(p);
                return;
            }
            if (!p.startsWith("module/")) {
                return;
            }
            String from = p.split("/")[1];
            graph.computeIfAbsent(from, k -> new HashSet<>());
            m.reset();
            while (m.find()) {
                if (!m.group(1).equals(from)) {
                    graph.get(from).add(m.group(1));
                }
            }
        });
        assertThat(commonViolations).as("common/ importing modules").isEmpty();
        for (String start : graph.keySet()) {
            assertThat(reaches(graph, start, start, new HashSet<>())).as("cycle through module '%s': %s", start, graph).isFalse();
        }
    }

    private static boolean reaches(Map<String, Set<String>> graph, String from, String target, Set<String> seen) {
        for (String next : graph.getOrDefault(from, Set.of())) {
            if (next.equals(target)) {
                return true;
            }
            if (seen.add(next) && reaches(graph, next, target, seen)) {
                return true;
            }
        }
        return false;
    }
}
