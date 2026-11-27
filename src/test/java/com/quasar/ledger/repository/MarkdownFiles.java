package com.quasar.ledger.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.yaml.snakeyaml.Yaml;

/** Helpers shared by the repository validators (brain notes and Agent Skills). */
final class MarkdownFiles {

    static final Path ROOT = Path.of("").toAbsolutePath();
    static final Path BRAIN = ROOT.resolve("brain");
    private static final Pattern WIKILINK = Pattern.compile("\\[\\[([^\\]|#]+)(?:#[^\\]|]*)?(?:\\|[^\\]]*)?]]");

    private MarkdownFiles() {
    }

    static List<Path> markdownUnder(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(dir)) {
            return files.filter(p -> p.toString().endsWith(".md")).sorted().toList();
        }
    }

    /** Parses the YAML frontmatter between the first two '---' lines; returns null if there is none. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> frontmatter(String text) {
        String normalized = text.replace("\r\n", "\n");
        if (!normalized.startsWith("---\n")) {
            return null;
        }
        int end = normalized.indexOf("\n---", 4);
        if (end < 0) {
            return null;
        }
        Object parsed = new Yaml().load(normalized.substring(4, end));
        return parsed instanceof Map ? (Map<String, Object>) parsed : null;
    }

    /** Wikilink targets in a document, ignoring fenced code blocks and inline code (examples, not links). */
    static Set<String> wikilinks(String text) {
        String withoutCode = text.replace("\r\n", "\n").replaceAll("(?s)```.*?```", "").replaceAll("`[^`\n]*`", "");
        Set<String> targets = new LinkedHashSet<>();
        Matcher m = WIKILINK.matcher(withoutCode);
        while (m.find()) {
            targets.add(m.group(1).trim());
        }
        return targets;
    }

    static String nameOf(Path file) {
        String n = file.getFileName().toString();
        return n.substring(0, n.length() - ".md".length());
    }

    /** Names of every note in the vault (Obsidian resolves [[name]] by file name anywhere in the vault). */
    static Set<String> allNoteNames() throws IOException {
        Set<String> names = new LinkedHashSet<>();
        for (Path p : markdownUnder(BRAIN)) {
            names.add(nameOf(p));
        }
        return names;
    }

    /** Names of distilled notes, i.e. notes in brain/resources/ (SB-3 "distilled note"). */
    static Set<String> distilledNoteNames() throws IOException {
        Set<String> names = new LinkedHashSet<>();
        for (Path p : markdownUnder(BRAIN.resolve("resources"))) {
            if (!p.getFileName().toString().equals("README.md")) {
                names.add(nameOf(p));
            }
        }
        return names;
    }

    static List<String> relative(List<Path> files) {
        List<String> out = new ArrayList<>();
        for (Path f : files) {
            out.add(ROOT.relativize(f).toString().replace('\\', '/'));
        }
        return out;
    }
}
