package com.adam.agri.planner.jls.reader;

import java.util.*;

/**
 * Represents a section of the Java Language Specification.
 * JLS is organized hierarchically: Chapters -> Sections -> Subsections.
 */
public final class JlsSection {
    private final String chapter;
    private final String section;
    private final String subsection;
    private final String title;
    private final String content;
    private final String htmlId;
    private final List<JlsSection> children;
    private final JlsSection parent;

    public JlsSection(String chapter, String section, String subsection,
                      String title, String content, String htmlId,
                      List<JlsSection> children, JlsSection parent) {
        this.chapter = chapter;
        this.section = section;
        this.subsection = subsection;
        this.title = title;
        this.content = content;
        this.htmlId = htmlId;
        this.children = children != null ? List.copyOf(children) : List.of();
        this.parent = parent;
    }

    public String getChapter() { return chapter; }
    public String getSection() { return section; }
    public String getSubsection() { return subsection; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getHtmlId() { return htmlId; }
    public List<JlsSection> getChildren() { return children; }
    public Optional<JlsSection> getParent() { return Optional.ofNullable(parent); }

    /**
     * Full path like "4.1" or "15.21.1"
     */
    public String getFullPath() {
        if (subsection != null && !subsection.isEmpty()) {
            return section + "." + subsection;
        }
        return section;
    }

    /**
     * Navigate to a subsection by number.
     */
    public Optional<JlsSection> navigate(String path) {
        if (getFullPath().equals(path)) {
            return Optional.of(this);
        }
        for (JlsSection child : children) {
            Optional<JlsSection> found = child.navigate(path);
            if (found.isPresent()) return found;
        }
        return Optional.empty();
    }

    /**
     * Search for sections containing keyword in title.
     */
    public List<JlsSection> searchByTitle(String keyword) {
        List<JlsSection> results = new ArrayList<>();
        if (title.toLowerCase().contains(keyword.toLowerCase())) {
            results.add(this);
        }
        for (JlsSection child : children) {
            results.addAll(child.searchByTitle(keyword));
        }
        return results;
    }

    /**
     * Search for sections containing content pattern.
     */
    public List<JlsSection> searchByContent(String pattern) {
        List<JlsSection> results = new ArrayList<>();
        if (content != null && content.toLowerCase().contains(pattern.toLowerCase())) {
            results.add(this);
        }
        for (JlsSection child : children) {
            results.addAll(child.searchByContent(pattern));
        }
        return results;
    }

    /**
     * Get all sections including this and descendants.
     */
    public List<JlsSection> flatten() {
        List<JlsSection> all = new ArrayList<>();
        all.add(this);
        for (JlsSection child : children) {
            all.addAll(child.flatten());
        }
        return all;
    }

    @Override
    public String toString() {
        return "JlsSection[" + getFullPath() + " " + title + "]";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String chapter;
        private String section;
        private String subsection;
        private String title;
        private String content;
        private String htmlId;
        private List<JlsSection> children = new ArrayList<>();
        private JlsSection parent;

        public Builder chapter(String c) { this.chapter = c; return this; }
        public Builder section(String s) { this.section = s; return this; }
        public Builder subsection(String s) { this.subsection = s; return this; }
        public Builder title(String t) { this.title = t; return this; }
        public Builder content(String c) { this.content = c; return this; }
        public Builder htmlId(String id) { this.htmlId = id; return this; }
        public Builder addChild(JlsSection c) { this.children.add(c); return this; }
        public Builder parent(JlsSection p) { this.parent = p; return this; }

        public JlsSection build() {
            return new JlsSection(chapter, section, subsection, title, content, htmlId, children, parent);
        }
    }
}
