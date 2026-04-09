package com.adam.agri.planner.symbolic.ontology.computer;

/**
 * Programming language enumeration for software systems.
 * Any specific language can be represented in the ontology while keeping
 * the abstractions language-agnostic.
 */
public enum ProgramLanguage {
    JAVA("Java", ".java", "package"),
    PYTHON("Python", ".py", "…"),
    JAVASCRIPT("JavaScript", ".js", "import"),
    TYPESCRIPT("TypeScript", ".ts", "import"),
    RUST("Rust", ".rs", "use"),
    GO("Go", ".go", "package"),
    C_PLUSPLUS("C++", ".cpp", "#include"),
    C_SHARP("C#", ".cs", "namespace"),
    KOTLIN("Kotlin", ".kt", "package"),
    SCALA("Scala", ".scala", "package");

    private final String displayName;
    private final String fileExtension;
    private final String importKeyword;

    ProgramLanguage(String displayName, String fileExtension, String importKeyword) {
        this.displayName = displayName;
        this.fileExtension = fileExtension;
        this.importKeyword = importKeyword;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public String getImportKeyword() {
        return importKeyword;
    }
}