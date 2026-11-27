package com.quasar.ledger.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CI "brain validation" (SDLC section 4): PARA structure, the note schema, broken wikilinks, and SB-3 for ADRs.
 * The schema is documented in brain/skills/brain-ingest/SKILL.md. Run alone with:
 * {@code mvn test -Dtest=BrainVaultTest -Djacoco.skip=true}
 */
class BrainVaultTest {

    private static final List<String> PARA = List.of("inbox", "projects", "areas", "resources", "archive");
    private static final Set<String> TYPES = Set.of("research", "resource", "project", "area", "archive", "review",
            "ai-session", "decision", "meeting");
    /** Files that describe the vault itself and therefore carry no note frontmatter. */
    private static final Set<String> STRUCTURAL = Set.of("README.md", "index.md", "log.md", "SKILL.md");

    private static boolean isNote(Path p) {
        return !STRUCTURAL.contains(p.getFileName().toString())
                && !MarkdownFiles.BRAIN.relativize(p).toString().replace('\\', '/').startsWith("skills/");
    }

    @Test
    @DisplayName("PARA folders, brain/index.md and brain/log.md exist (SDLC 1)")
    void paraStructureExists() {
        for (String dir : PARA) {
            assertThat(MarkdownFiles.BRAIN.resolve(dir)).as("brain/%s/", dir).isDirectory();
        }
        assertThat(MarkdownFiles.BRAIN.resolve("index.md")).isRegularFile();
        assertThat(MarkdownFiles.BRAIN.resolve("log.md")).isRegularFile();
    }

    @Test
    @DisplayName("Every note has valid frontmatter: title, type, authors, created (yyyy-MM-dd)")
    void notesFollowTheSchema() throws IOException {
        List<String> problems = new ArrayList<>();
        int notes = 0;
        for (Path p : MarkdownFiles.markdownUnder(MarkdownFiles.BRAIN)) {
            if (!isNote(p)) {
                continue;
            }
            notes++;
            String where = MarkdownFiles.ROOT.relativize(p).toString().replace('\\', '/');
            Map<String, Object> fm = MarkdownFiles.frontmatter(Files.readString(p));
            if (fm == null) {
                problems.add(where + ": missing YAML frontmatter (--- ... ---)");
                continue;
            }
            if (!(fm.get("title") instanceof String t) || t.isBlank()) {
                problems.add(where + ": 'title' is required");
            }
            if (!(fm.get("type") instanceof String type) || !TYPES.contains(type)) {
                problems.add(where + ": 'type' must be one of " + TYPES);
            }
            if (!(fm.get("authors") instanceof List<?> authors) || authors.isEmpty()) {
                problems.add(where + ": 'authors' must be a non-empty list");
            }
            Object created = fm.get("created");
            if (!(created instanceof java.util.Date) && !isIsoDate(created)) {
                problems.add(where + ": 'created' must be a date in yyyy-MM-dd format");
            }
        }
        assertThat(notes).as("number of brain notes").isGreaterThanOrEqualTo(10);
        assertThat(problems).as("brain note schema problems").isEmpty();
    }

    private static boolean isIsoDate(Object value) {
        if (!(value instanceof String s)) {
            return false;
        }
        try {
            LocalDate.parse(s);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    @Test
    @DisplayName("No broken [[wikilinks]] anywhere in brain/, docs/ or AGENTS.md (SB-1)")
    void noBrokenWikilinks() throws IOException {
        Set<String> names = MarkdownFiles.allNoteNames();
        List<Path> files = new ArrayList<>(MarkdownFiles.markdownUnder(MarkdownFiles.BRAIN));
        files.addAll(MarkdownFiles.markdownUnder(MarkdownFiles.ROOT.resolve("docs")));
        files.add(MarkdownFiles.ROOT.resolve("AGENTS.md"));
        List<String> broken = new ArrayList<>();
        for (Path p : files) {
            for (String target : MarkdownFiles.wikilinks(Files.readString(p))) {
                if (!names.contains(target)) {
                    broken.add(MarkdownFiles.ROOT.relativize(p).toString().replace('\\', '/') + " -> [[" + target + "]]");
                }
            }
        }
        assertThat(broken).as("broken wikilinks").isEmpty();
    }

    @Test
    @DisplayName("SB-3: every ADR cites at least one distilled note in brain/resources/")
    void everyAdrCitesADistilledNote() throws IOException {
        Set<String> distilled = MarkdownFiles.distilledNoteNames();
        List<Path> adrs = MarkdownFiles.markdownUnder(MarkdownFiles.ROOT.resolve("docs").resolve("adr")).stream()
                .filter(p -> p.getFileName().toString().startsWith("ADR-")).toList();
        assertThat(adrs).isNotEmpty();
        List<String> missing = new ArrayList<>();
        for (Path adr : adrs) {
            if (MarkdownFiles.wikilinks(Files.readString(adr)).stream().noneMatch(distilled::contains)) {
                missing.add(adr.getFileName().toString());
            }
        }
        assertThat(missing).as("ADRs without a distilled-note citation").isEmpty();
    }
}
