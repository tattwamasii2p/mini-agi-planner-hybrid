package com.adam.agri.planner.jls.reader;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Manages JLS and JVMS documentation files.
 * Indexes multiple chapters/areas for unified access.
 */
public class JlsRepository {

    private final Path jlsDirectory;
    private final Path jvmsDirectory;
    private final JlsReader jlsReader;
    private final JvmsReader jvmsReader;

    // Indexed sections
    private final Map<String, JlsSection> jlsIndex;
    private final Map<String, JvmsReader.JvmsSection> jvmsIndex;
    private final List<JlsSection> allJlsSections;
    private final List<JvmsReader.JvmsSection> allJvmsSections;

    public JlsRepository(Path jlsDirectory, Path jvmsDirectory) throws IOException {
        this.jlsDirectory = jlsDirectory;
        this.jvmsDirectory = jvmsDirectory;
        this.jlsReader = new JlsReader();
        this.jvmsReader = new JvmsReader();
        this.jlsIndex = new HashMap<>();
        this.jvmsIndex = new HashMap<>();
        this.allJlsSections = new ArrayList<>();
        this.allJvmsSections = new ArrayList<>();

        loadAll();
    }

    public JlsRepository(Path jlsDirectory) throws IOException {
        this(jlsDirectory, null);
    }

    private void loadAll() throws IOException {
        // Load JLS chapters
        if (jlsDirectory != null && Files.exists(jlsDirectory)) {
            try (Stream<Path> files = Files.list(jlsDirectory)) {
                files.filter(f -> f.toString().endsWith(".html"))
                     .sorted()
                     .forEach(this::loadJlsFile);
            }
        }

        // Load JVMS chapters
        if (jvmsDirectory != null && Files.exists(jvmsDirectory)) {
            try (Stream<Path> files = Files.list(jvmsDirectory)) {
                files.filter(f -> f.toString().endsWith(".html"))
                     .sorted()
                     .forEach(this::loadJvmsFile);
            }
        }
    }

    private void loadJlsFile(Path file) {
        try {
            List<JlsSection> sections = jlsReader.readFromHtml(file);
            allJlsSections.addAll(sections);

            for (JlsSection section : sections) {
                // Index by full path
                jlsIndex.put(section.getFullPath(), section);
                // Index by HTML ID
                if (section.getHtmlId() != null && !section.getHtmlId().isEmpty()) {
                    jlsIndex.put("#" + section.getHtmlId(), section);
                }
                // Index children
                for (JlsSection child : section.flatten()) {
                    jlsIndex.put(child.getFullPath(), child);
                    if (child.getHtmlId() != null && !child.getHtmlId().isEmpty()) {
                        jlsIndex.put("#" + child.getHtmlId(), child);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load " + file + ": " + e.getMessage());
        }
    }

    private void loadJvmsFile(Path file) {
        try {
            List<JvmsReader.JvmsSection> sections = jvmsReader.readFromHtml(file);
            allJvmsSections.addAll(sections);

            for (JvmsReader.JvmsSection section : sections) {
                jvmsIndex.put(section.getArea(), section);
                if (section.getHtmlId() != null && !section.getHtmlId().isEmpty()) {
                    jvmsIndex.put("#" + section.getHtmlId(), section);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load " + file + ": " + e.getMessage());
        }
    }

    /**
     * Find JLS section by path (e.g., "4.1", "15.21.1").
     */
    public Optional<JlsSection> findJlsSection(String path) {
        return Optional.ofNullable(jlsIndex.get(path));
    }

    /**
     * Find JVMS section by area (e.g., "2.11", "4.9.2").
     */
    public Optional<JvmsReader.JvmsSection> findJvmsSection(String area) {
        return Optional.ofNullable(jvmsIndex.get(area));
    }

    /**
     * Search JLS sections by title keyword.
     */
    public List<JlsSection> searchJlsByTitle(String keyword) {
        List<JlsSection> results = new ArrayList<>();
        for (JlsSection section : allJlsSections) {
            results.addAll(section.searchByTitle(keyword));
        }
        return results.stream()
            .distinct()
            .sorted(Comparator.comparing(JlsSection::getFullPath))
            .toList();
    }

    /**
     * Search JVMS sections by title keyword.
     */
    public List<JvmsReader.JvmsSection> searchJvmsByTitle(String keyword) {
        return allJvmsSections.stream()
            .filter(s -> s.getTitle().toLowerCase().contains(keyword.toLowerCase()))
            .sorted(Comparator.comparing(JvmsReader.JvmsSection::getArea))
            .toList();
    }

    /**
     * Get all sections for a specific JLS chapter.
     */
    public List<JlsSection> getJlsChapter(String chapterNumber) {
        return allJlsSections.stream()
            .filter(s -> s.getChapter().equals(chapterNumber))
            .toList();
    }

    /**
     * Get all sections for a specific JVMS chapter.
     */
    public List<JvmsReader.JvmsSection> getJvmsChapter(String chapterNumber) {
        return allJvmsSections.stream()
            .filter(s -> s.getChapter().equals(chapterNumber))
            .toList();
    }

    /**
     * Search across both specifications.
     */
    public SearchResults searchBoth(String keyword) {
        return new SearchResults(
            searchJlsByTitle(keyword),
            searchJvmsByTitle(keyword)
        );
    }

    /**
     * Get all indexed JLS section paths.
     */
    public Set<String> getJlsPaths() {
        return Collections.unmodifiableSet(jlsIndex.keySet());
    }

    /**
     * Get all indexed JVMS areas.
     */
    public Set<String> getJvmsAreas() {
        return Collections.unmodifiableSet(jvmsIndex.keySet());
    }

    /**
     * Navigation: get supertypes (for inheritance sections).
     */
    public List<JlsSection> getInheritanceSections() {
        return searchJlsByTitle("inheritance")
            .stream()
            .filter(s -> s.getTitle().toLowerCase().contains("inheritance") ||
                        s.getTitle().toLowerCase().contains("superclass") ||
                        s.getTitle().toLowerCase().contains("subclass"))
            .toList();
    }

    /**
     * Navigation: get type conversion sections.
     */
    public List<JlsSection> getTypeConversionSections() {
        return searchJlsByTitle("conversion");
    }

    /**
     * Navigation: get expression evaluation sections.
     */
    public List<JlsSection> getExpressionSections() {
        return allJlsSections.stream()
            .filter(s -> s.getChapter().equals("15"))
            .toList();
    }

    /**
     * Get byte code sections from JVMS.
     */
    public List<JvmsReader.JvmsSection> getBytecodeSections() {
        return allJvmsSections.stream()
            .filter(s -> s.getSectionType() == JvmsReader.SectionType.BYTECODE)
            .toList();
    }

    /**
     * Find class file format section.
     */
    public Optional<JvmsReader.JvmsSection> findClassFileFormat() {
        return allJvmsSections.stream()
            .filter(s -> s.getSectionType() == JvmsReader.SectionType.CLASS_FILE_FORMAT)
            .findFirst();
    }

    // Record for search results
    public record SearchResults(List<JlsSection> jlsResults, List<JvmsReader.JvmsSection> jvmsResults) {
        public boolean isEmpty() {
            return jlsResults.isEmpty() && jvmsResults.isEmpty();
        }

        public int totalCount() {
            return jlsResults.size() + jvmsResults.size();
        }
    }

    /**
     * Builder for creating repositories.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Path jlsDir;
        private Path jvmsDir;

        public Builder jlsDirectory(Path p) {
            this.jlsDir = p;
            return this;
        }

        public Builder jvmsDirectory(Path p) {
            this.jvmsDir = p;
            return this;
        }

        public JlsRepository build() throws IOException {
            return new JlsRepository(jlsDir, jvmsDir);
        }
    }
}
