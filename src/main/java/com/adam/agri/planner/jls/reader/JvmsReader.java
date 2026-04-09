package com.adam.agri.planner.jls.reader;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and parses Java Virtual Machine Specification documents.
 * JVMS describes bytecode, class file format, and runtime behavior.
 */
public class JvmsReader {

    private static final Pattern AREA_PATTERN = Pattern.compile("^(\\d+)\\.([\\d.]*)(?:\\s+(.*))?$");
    private static final Pattern CHAPTER_PATTERN = Pattern.compile("^Chapter (\\d+)");

    public JvmsReader() {
    }

    /**
     * Parse JVMS HTML file.
     */
    public List<JvmsSection> readFromHtml(Path htmlFile) throws Exception {
        String html = Files.readString(htmlFile);
        return parseHtml(html);
    }

    /**
     * Parse JVMS HTML from string.
     */
    public List<JvmsSection> parseHtml(String html) throws Exception {
        String cleaned = cleanHtmlForParsing(html);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        DocumentBuilder builder = factory.newDocumentBuilder();

        InputSource source = new InputSource(new StringReader(cleaned));
        Document doc = builder.parse(source);

        return extractSections(doc);
    }

    private List<JvmsSection> extractSections(Document doc) {
        List<JvmsSection> sections = new ArrayList<>();

        // Similar structure to JLS but focuses on runtime/bytecode areas
        NodeList allNodes = doc.getElementsByTagName("*");

        JvmsSection.Builder currentChapter = null;
        JvmsSection.Builder currentArea = null;
        Stack<JvmsSection.Builder> stack = new Stack<>();

        for (int i = 0; i < allNodes.getLength(); i++) {
            Element elem = (Element) allNodes.item(i);
            String tagName = elem.getTagName().toLowerCase();

            if (isHeading(tagName)) {
                String text = getTextContent(elem).trim();
                String id = elem.getAttribute("id");

                Matcher chapterMatch = CHAPTER_PATTERN.matcher(text);
                Matcher areaMatch = AREA_PATTERN.matcher(text);

                if (chapterMatch.find()) {
                    String chapterNum = chapterMatch.group(1);
                    String title = text.substring(chapterMatch.end()).trim();

                    if (currentChapter != null) {
                        sections.add(currentChapter.build());
                    }

                    currentChapter = JvmsSection.builder()
                        .chapter(chapterNum)
                        .area(chapterNum)
                        .title(title)
                        .htmlId(id)
                        .sectionType(detectSectionType(title));

                    stack.clear();

                } else if (areaMatch.find() && currentChapter != null) {
                    String chap = areaMatch.group(1);
                    String area = areaMatch.group(2);
                    String title = areaMatch.group(3);

                    if (area == null || area.isEmpty()) continue;

                    String[] parts = area.split("\\.");

                    // Pop to correct level
                    while (stack.size() >= parts.length) {
                        stack.pop();
                    }

                    currentArea = JvmsSection.builder()
                        .chapter(chap)
                        .area(chap + "." + area)
                        .title(title)
                        .htmlId(id)
                        .sectionType(detectSectionType(title));

                    if (!stack.isEmpty()) {
                        JvmsSection parent = stack.peek().build();
                        currentArea = currentArea.parent(parent);
                    }

                    stack.push(currentArea);

                    if (stack.size() == 1) {
                        if (currentChapter != null) {
                            currentChapter.addChild(currentArea.build());
                        }
                    }
                }
            }
        }

        if (currentChapter != null) {
            sections.add(currentChapter.build());
        }

        return sections;
    }

    /**
     * Detect section type from title for semantic categorization.
     */
    private SectionType detectSectionType(String title) {
        String lower = title.toLowerCase();
        if (lower.contains("bytecode") || lower.contains("instruction")) {
            return SectionType.BYTECODE;
        }
        if (lower.contains("class file") || lower.contains("format")) {
            return SectionType.CLASS_FILE_FORMAT;
        }
        if (lower.contains("runtime") || lower.contains("execution")) {
            return SectionType.RUNTIME;
        }
        if (lower.contains("type") || lower.contains("checking")) {
            return SectionType.TYPE_CHECKING;
        }
        if (lower.contains("constant pool") || lower.contains("attribute")) {
            return SectionType.CLASS_FILE_COMPONENT;
        }
        return SectionType.GENERAL;
    }

    private boolean isHeading(String tagName) {
        return tagName.matches("h[1-6]");
    }

    private String getTextContent(Element elem) {
        StringBuilder text = new StringBuilder();
        NodeList children = elem.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                text.append(child.getTextContent());
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                text.append(getTextContent((Element) child));
            }
        }
        return text.toString();
    }

    private String cleanHtmlForParsing(String html) {
        String result = html.trim();
        result = result.replaceAll("(?i)<!DOCTYPE[^>]*>", "");
        result = result.replaceAll("(?is)<script[^>]*>.*?</script>", "");
        result = result.replaceAll("(?is)<style[^>]*>.*?</style>", "");

        // Escape entities
        result = result.replace("&nbsp;", "&#160;");
        result = result.replace("&copy;", "&#169;");
        result = result.replace("&mdash;", "&#8212;");

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + result;
    }

    public enum SectionType {
        GENERAL,
        BYTECODE,
        CLASS_FILE_FORMAT,
        CLASS_FILE_COMPONENT,
        RUNTIME,
        TYPE_CHECKING
    }

    /**
     * JVMS Section representation.
     */
    public static final class JvmsSection {
        private final String chapter;
        private final String area;
        private final String title;
        private final String content;
        private final String htmlId;
        private final SectionType sectionType;
        private final List<JvmsSection> children;
        private final JvmsSection parent;

        public JvmsSection(String chapter, String area, String title, String content,
                          String htmlId, SectionType sectionType,
                          List<JvmsSection> children, JvmsSection parent) {
            this.chapter = chapter;
            this.area = area;
            this.title = title;
            this.content = content;
            this.htmlId = htmlId;
            this.sectionType = sectionType;
            this.children = children != null ? List.copyOf(children) : List.of();
            this.parent = parent;
        }

        public String getChapter() { return chapter; }
        public String getArea() { return area; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
        public String getHtmlId() { return htmlId; }
        public SectionType getSectionType() { return sectionType; }
        public List<JvmsSection> getChildren() { return children; }
        public Optional<JvmsSection> getParent() { return Optional.ofNullable(parent); }

        @Override
        public String toString() {
            return "JvmsSection[" + area + " " + title + "]";
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String chapter;
            private String area;
            private String title;
            private String content;
            private String htmlId;
            private SectionType sectionType;
            private List<JvmsSection> children = new ArrayList<>();
            private JvmsSection parent;

            public Builder chapter(String c) { this.chapter = c; return this; }
            public Builder area(String a) { this.area = a; return this; }
            public Builder title(String t) { this.title = t; return this; }
            public Builder content(String c) { this.content = c; return this; }
            public Builder htmlId(String id) { this.htmlId = id; return this; }
            public Builder sectionType(SectionType t) { this.sectionType = t; return this; }
            public Builder addChild(JvmsSection c) { this.children.add(c); return this; }
            public Builder parent(JvmsSection p) { this.parent = p; return this; }

            public JvmsSection build() {
                return new JvmsSection(chapter, area, title, content, htmlId, sectionType, children, parent);
            }
        }
    }
}
