package com.quasar.ledger.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CI "SKILL.md validation" (SDLC section 4) against the Agent Skills format (agentskills.io):
 * each skill is a folder containing SKILL.md; frontmatter 'name' (1-64 chars, lower-case letters, digits and
 * single hyphens, equal to the folder name) and 'description' (1-1024 chars) are required; only the documented
 * optional fields are allowed. SB-3: every skill cites at least one distilled brain note.
 * Run alone with: {@code mvn test -Dtest=SkillDefinitionsTest -Djacoco.skip=true}
 */
class SkillDefinitionsTest {

    private static final Path SKILLS = MarkdownFiles.BRAIN.resolve("skills");
    private static final Set<String> REQUIRED_SKILLS = Set.of("brain-ingest", "brain-refine", "brain-query", "local-check",
            "simulate-transfer");
    private static final Set<String> ALLOWED_FIELDS = Set.of("name", "description", "license", "compatibility", "metadata",
            "allowed-tools");
    private static final String NAME_PATTERN = "^[a-z0-9]+(-[a-z0-9]+)*$";

    @Test
    @DisplayName("All required skills exist, each as <skill-name>/SKILL.md")
    void requiredSkillsExist() throws IOException {
        for (String skill : REQUIRED_SKILLS) {
            assertThat(SKILLS.resolve(skill).resolve("SKILL.md")).as("skill %s", skill).isRegularFile();
        }
        List<Path> misplaced = MarkdownFiles.markdownUnder(MarkdownFiles.ROOT.resolve("brain")).stream()
                .filter(p -> p.getFileName().toString().equalsIgnoreCase("skill.md"))
                .filter(p -> !p.getParent().getParent().equals(SKILLS) || !p.getFileName().toString().equals("SKILL.md"))
                .toList();
        assertThat(MarkdownFiles.relative(misplaced)).as("SKILL.md files outside brain/skills/<name>/").isEmpty();
    }

    @Test
    @DisplayName("Frontmatter and naming follow the Agent Skills format")
    void frontmatterIsValid() throws IOException {
        List<String> problems = new ArrayList<>();
        for (Path file : skillFiles()) {
            String folder = file.getParent().getFileName().toString();
            String where = "brain/skills/" + folder + "/SKILL.md";
            String text = Files.readString(file);
            Map<String, Object> fm = MarkdownFiles.frontmatter(text);
            if (fm == null) {
                problems.add(where + ": missing YAML frontmatter");
                continue;
            }
            if (!(fm.get("name") instanceof String name)) {
                problems.add(where + ": 'name' is required");
            } else {
                if (name.isEmpty() || name.length() > 64 || !name.matches(NAME_PATTERN)) {
                    problems.add(where + ": 'name' must be 1-64 lower-case letters, digits and single hyphens");
                }
                if (!name.equals(folder)) {
                    problems.add(where + ": 'name' (" + name + ") must equal the folder name (" + folder + ")");
                }
            }
            if (!(fm.get("description") instanceof String d) || d.isBlank() || d.length() > 1024) {
                problems.add(where + ": 'description' is required and must be 1-1024 characters");
            }
            if (fm.get("compatibility") instanceof String c && c.length() > 500) {
                problems.add(where + ": 'compatibility' must be at most 500 characters");
            }
            for (String key : fm.keySet()) {
                if (!ALLOWED_FIELDS.contains(key)) {
                    problems.add(where + ": unknown frontmatter field '" + key + "'");
                }
            }
            String body = text.replace("\r\n", "\n");
            body = body.substring(body.indexOf("\n---", 4) + 4).trim();
            if (body.length() < 200) {
                problems.add(where + ": the instructions body is empty or too short");
            }
        }
        assertThat(problems).as("SKILL.md problems").isEmpty();
    }

    @Test
    @DisplayName("SB-3: every skill cites at least one distilled note in brain/resources/")
    void everySkillCitesADistilledNote() throws IOException {
        Set<String> distilled = MarkdownFiles.distilledNoteNames();
        List<String> missing = new ArrayList<>();
        for (Path file : skillFiles()) {
            if (MarkdownFiles.wikilinks(Files.readString(file)).stream().noneMatch(distilled::contains)) {
                missing.add(file.getParent().getFileName().toString());
            }
        }
        assertThat(missing).as("skills without a distilled-note citation").isEmpty();
    }

    private static List<Path> skillFiles() throws IOException {
        List<Path> files = new ArrayList<>();
        for (Path p : MarkdownFiles.markdownUnder(SKILLS)) {
            if (p.getFileName().toString().equals("SKILL.md")) {
                files.add(p);
            }
        }
        assertThat(files).isNotEmpty();
        return files;
    }
}
