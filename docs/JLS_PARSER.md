# Java Language Specification Parser

Documentation for the `com.adam.agri.planner.jls.reader` package - a comprehensive parser for JLS and JVMS documents.

## Overview

This parser reads Java Language Specification (JLS) and Java Virtual Machine Specification (JVMS) HTML documents and converts them into structured, navigable Java objects.

## Quick Start

```java
import com.adam.agri.planner.jls.reader.*;
import java.nio.file.Path;
import java.nio.file.Paths;

// Create repository from JLS/JVMS HTML files
JlsRepository repo = JlsRepository.builder()
    .jlsDirectory(Paths.get("/docs/jls"))
    .jvmsDirectory(Paths.get("/docs/jvms"))
    .build();

// Look up a specific section
Optional<JlsSection> sec4_1 = repo.findJlsSection("4.1");
sec4_1.ifPresent(s -> System.out.println(s.getTitle()));
```

## Obtaining JLS/JVMS Documents

Download from Oracle:
- JLS: https://docs.oracle.com/javase/specs/jls/se17/html/index.html
- JVMS: https://docs.oracle.com/javase/specs/jvms/se17/html/index.html

Save each chapter as separate HTML files (e.g., `jls-1.html`, `jls-2.html`, etc.)

## Core Classes

### JlsReader

Parses JLS HTML documents into structured sections.

```java
JlsReader reader = new JlsReader();

// Parse from file
List<JlsSection> chapters = reader.readFromHtml(Paths.get("jls-4.html"));

// Parse from string
String html = "..."; // JLS HTML content
List<JlsSection> sections = reader.parseHtml(html);

// Build search index
Map<String, JlsSection> index = reader.buildIndex(chapters);
```

### JlsSection

Represents a JLS section with hierarchical structure.

```java
JlsSection section = ...;

// Navigation
String path = section.getFullPath();      // "4.1", "15.21.1"
String chapter = section.getChapter();    // "4"
String title = section.getTitle();        // "The Kinds of Types and Values"
Optional<JlsSection> parent = section.getParent();
List<JlsSection> children = section.getChildren();

// Search within section tree
Optional<JlsSection> subsec = section.navigate("4.1.2");
List<JlsSection> found = section.searchByTitle("primitive");
List<JlsSection> contentMatch = section.searchByContent("widening conversion");

// Flatten all descendants
List<JlsSection> all = section.flatten();
```

### JvmsReader

Parses JVMS documents focusing on bytecode and runtime.

```java
JvmsReader jvmsReader = new JvmsReader();
List<JvmsReader.JvmsSection> jvmsSections = jvmsReader.readFromHtml(
    Paths.get("jvms-2.html")
);

for (JvmsReader.JvmsSection sec : jvmsSections) {
    System.out.println(sec.getArea() + ": " + sec.getTitle());
    System.out.println("Type: " + sec.getSectionType()); // BYTECODE, CLASS_FILE_FORMAT, etc.
}
```

### JlsRepository

Unified access to both JLS and JVMS with full-text indexing.

```java
JlsRepository repo = new JlsRepository(
    Paths.get("/docs/jls"),
    Paths.get("/docs/jvms")
);

// Direct lookups
Optional<JlsSection> types = repo.findJlsSection("4");
Optional<JlsSection> conversions = repo.findJlsSection("5.1");
Optional<JvmsReader.JvmsSection> bytecode = repo.findJvmsSection("2.11");

// Search across both specifications
JlsRepository.SearchResults results = repo.searchBoth("type conversion");
for (JlsSection s : results.jlsResults()) {
    System.out.println("JLS " + s.getFullPath() + ": " + s.getTitle());
}
for (JvmsReader.JvmsSection s : results.jvmsResults()) {
    System.out.println("JVMS " + s.getArea() + ": " + s.getTitle());
}

// Get chapters
List<JlsSection> chapter4 = repo.getJlsChapter("4");        // Types
List<JlsSection> chapter5 = repo.getJlsChapter("5");        // Conversions
List<JlsSection> chapter15 = repo.getJlsChapter("15");      // Expressions

// JVMS bytecode sections
List<JvmsReader.JvmsSection> bytecodeSections = repo.getBytecodeSections();

// Class file format
Optional<JvmsReader.JvmsSection> classFile = repo.findClassFileFormat();
```

### JlsClassifier

Extracts structured rules from section content.

```java
JlsSection section = repo.findJlsSection("4").orElseThrow();
String content = section.getContent();

// Extract grammar rules
List<JlsClassifier.GrammarRule> grammar = JlsClassifier.extractGrammarRules(content);
for (JlsClassifier.GrammarRule rule : grammar) {
    System.out.println(rule);  // Type ::= PrimitiveType | ReferenceType
}

// Extract type conversion rules
List<JlsClassifier.TypeRule> typeRules = JlsClassifier.extractTypeRules(content);
for (JlsClassifier.TypeRule rule : typeRules) {
    System.out.println(rule.kind() + ": " + rule.pattern());
    System.out.println("  " + rule.description());
}

// Extract execution semantics
List<JlsClassifier.ExecutionRule> execRules = JlsClassifier.extractExecutionRules(content);

// Check section category
if (JlsClassifier.isTypeSystemSection(section)) {
    // Process type system rules
}

if (JlsClassifier.isBinaryCompatibilitySection(section)) {
    // Process binary compatibility info
}
```

## Section Categories

JLS chapters are organized as:

| Chapter | Category | Description |
|---------|----------|-------------|
| 1 | INTRODUCTION | Overview |
| 2 | GRAMMAR | Grammar notation |
| 3 | LEXICAL | Lexical structure |
| 4 | TYPES | Types, values, variables |
| 5 | CONVERSIONS | Type conversions |
| 6 | NAMES | Names and identifiers |
| 7 | CLASSES | Packages and modules |
| 8 | CLASSES | Classes |
| 9 | INTERFACES | Interfaces |
| 10 | ARRAYS | Arrays |
| 11 | EXCEPTIONS | Exceptions |
| 12 | EXECUTION | Execution model |
| 13 | BINARY | Binary compatibility |
| 14 | COMPILATION | Blocks and statements |
| 15 | EXPRESSIONS | Expressions |
| 16 | DEFINITE_ASSIGNMENT | Definite assignment |
| 17 | THREADS | Threads and locks |
| 18 | TYPE_INFERENCE | Type inference |

## Common Use Cases

### Find Type Conversion Rules

```java
List<JlsSection> conversions = repo.searchJlsByTitle("conversion");
for (JlsSection sec : conversions) {
    List<TypeRule> rules = JlsClassifier.extractTypeRules(sec.getContent());
    // Process rules...
}
```

### Build Grammar from JLS Chapter 2

```java
JlsSection grammarChapter = repo.findJlsSection("2").orElseThrow();
List<GrammarRule> rules = JlsClassifier.extractGrammarRules(
    grammarChapter.getContent()
);
// rules contains productions like:
// CompilationUnit ::= PackageDeclaration? ImportDeclaration* TypeDeclaration*
```

### Find All Bytecode Instructions

```java
List<JvmsReader.JvmsSection> bytecode = repo.getBytecodeSections();
for (JvmsReader.JvmsSection sec : bytecode) {
    if (sec.getTitle().contains("aaload") ||
        sec.getTitle().contains("invokevirtual")) {
        // Found instruction documentation
    }
}
```

### Validate Section Relationships

```java
JlsSection sec4 = repo.findJlsSection("4").orElseThrow();
JlsSection sec4_1 = repo.findJlsSection("4.1").orElseThrow();

// Check if sec4_1 is child of sec4
assert sec4_1.getParent().get().equals(sec4);

// Flatten all subsections
List<JlsSection> allSubsections = sec4.flatten();
```

## Error Handling

```java
try {
    JlsReader reader = new JlsReader();
    List<JlsSection> sections = reader.readFromHtml(path);
} catch (Exception e) {
    // Handle parsing errors
    // Common issues: malformed HTML, missing entities
}

// Optional-based lookups
Optional<JlsSection> sec = repo.findJlsSection("999.999");
if (sec.isPresent()) {
    // Process section
} else {
    // Section not found
}
```

## Thread Safety

- `JlsReader`, `JvmsReader` - stateless, thread-safe
- `JlsSection` - immutable, thread-safe
- `JlsRepository` - immutable after construction, thread-safe for reading

## Performance

- Parsing: ~50-100ms per HTML file
- Indexing: O(n) for n sections
- Lookup: O(1) via HashMap
- Search: O(n) for content search, reduce via filtering first

## Tips

1. **Pre-build index**: Create repository once, reuse for multiple lookups
2. **Filter before content search**: Use `searchByTitle()` before `searchByContent()`
3. **Cache results**: `JlsSection` objects are immutable and safe to cache
4. **Use navigation**: `section.navigate("4.1.2")` faster than building new paths
