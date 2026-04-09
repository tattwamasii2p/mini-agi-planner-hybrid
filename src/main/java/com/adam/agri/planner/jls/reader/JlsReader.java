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
 * Reads and parses Java Language Specification HTML documents.
 * JLS is distributed as HTML with structured section headings.
 */
public class JlsReader {

    private static final Pattern SECTION_PATTERN = Pattern.compile("^(\\d+)\\.([\\d.]*)(?:\\s+(.*))?$");
    private static final Pattern CHAPTER_PATTERN = Pattern.compile("^Chapter (\\d+):");

    private final boolean lenientParsing;

    public JlsReader() {
        this(false);
    }

    public JlsReader(boolean lenientParsing) {
        this.lenientParsing = lenientParsing;
    }

    /**
     * Parse JLS HTML file into structured sections.
     */
    public List<JlsSection> readFromHtml(Path htmlFile) throws Exception {
        String html = Files.readString(htmlFile);
        return parseHtml(html);
    }

    /**
     * Parse JLS HTML from string.
     */
    public List<JlsSection> parseHtml(String html) throws Exception {
        String cleaned = cleanHtmlForParsing(html);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        DocumentBuilder builder = factory.newDocumentBuilder();

        InputSource source = new InputSource(new StringReader(cleaned));
        Document doc = builder.parse(source);

        return extractSections(doc);
    }

    /**
     * Extract sections from parsed DOM document.
     */
    private List<JlsSection> extractSections(Document doc) {
        List<JlsSection> sections = new ArrayList<>();

        NodeList allNodes = doc.getElementsByTagName("*");
        JlsSection.Builder currentChapter = null;
        Stack<JlsSection.Builder> sectionStack = new Stack<>();

        for (int i = 0; i < allNodes.getLength(); i++) {
            Element elem = (Element) allNodes.item(i);
            String tagName = elem.getTagName().toLowerCase();

            if (isHeading(tagName)) {
                String text = getTextContent(elem).trim();
                String id = elem.getAttribute("id");

                Matcher chapterMatch = CHAPTER_PATTERN.matcher(text);
                Matcher sectionMatch = SECTION_PATTERN.matcher(text);

                if (chapterMatch.find()) {
                    String chapterNum = chapterMatch.group(1);
                    String title = text.substring(chapterMatch.end()).trim();

                    currentChapter = JlsSection.builder()
                        .chapter(chapterNum)
                        .section(chapterNum)
                        .title(title)
                        .htmlId(id);

                } else if (sectionMatch.find()) {
                    String chap = sectionMatch.group(1);
                    String sec = sectionMatch.group(2);
                    String title = sectionMatch.group(3);

                    if (sec == null || sec.isEmpty()) {
                        if (currentChapter != null) {
                            sections.add(currentChapter.build());
                        }
                        currentChapter = JlsSection.builder()
                            .chapter(chap)
                            .section(chap)
                            .title(title)
                            .htmlId(id);
                        sectionStack.clear();
                    } else {
                        String[] parts = sec.split("\\.");
                        String subSection = parts.length > 1
                            ? String.join(".", Arrays.copyOfRange(parts, 1, parts.length))
                            : "";

                        while (sectionStack.size() >= parts.length) {
                            sectionStack.pop();
                        }

                        JlsSection.Builder newSection = JlsSection.builder()
                            .chapter(chap)
                            .section(chap + "." + parts[0])
                            .subsection(subSection)
                            .title(title)
                            .htmlId(id);

                        if (!sectionStack.isEmpty()) {
                            sectionStack.peek().addChild(newSection.build());
                        }
                        sectionStack.push(newSection);
                    }
                }
            }
        }

        if (currentChapter != null) {
            sections.add(currentChapter.build());
        }

        return sections;
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

    /**
     * Clean HTML to be well-formed XML.
     */
    private String cleanHtmlForParsing(String html) {
        String result = html.trim();
        if (!result.startsWith("<?xml") && !result.startsWith("<!DOCTYPE") && !result.startsWith("<html")) {
            if (!result.startsWith("<")) {
                result = "<div>" + result + "</div>";
            }
        }

        result = result.replaceAll("(?i)<!DOCTYPE[^>]*>", "");
        result = result.replaceAll("(?is)<script[^>]*>.*?</script>", "");
        result = result.replaceAll("(?is)<style[^>]*>.*?</style>", "");

        result = result.replace("&nbsp;", "&#160;");
        result = result.replace("&copy;", "&#169;");
        result = result.replace("&mdash;", "&#8212;");
        result = result.replace("&ndash;", "&#8211;");
        result = result.replace("&ldquo;", "&#8220;");
        result = result.replace("&rdquo;", "&#8221;");
        result = result.replace("&lsquo;", "&#8216;");
        result = result.replace("&rsquo;", "&#8217;");
        result = result.replace("&hellip;", "&#8230;");

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + result;
    }

    /**
     * Index all sections for fast lookup.
     */
    public Map<String, JlsSection> buildIndex(List<JlsSection> sections) {
        Map<String, JlsSection> index = new HashMap<>();
        for (JlsSection section : sections) {
            for (JlsSection s : section.flatten()) {
                index.put(s.getFullPath(), s);
                if (s.getHtmlId() != null && !s.getHtmlId().isEmpty()) {
                    index.put("#" + s.getHtmlId(), s);
                }
            }
        }
        return index;
    }
}
