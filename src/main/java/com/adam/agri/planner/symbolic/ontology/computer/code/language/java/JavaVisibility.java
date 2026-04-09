package com.adam.agri.planner.symbolic.ontology.computer.code.language.java;

/**
 * Java visibility/access modifiers.
 *
 * Ordered from most restrictive to least restrictive.
 */
public enum JavaVisibility {
    PRIVATE("private", 0),
    PACKAGE_PRIVATE("package", 1),
    PROTECTED("protected", 2),
    PUBLIC("public", 3);

    private final String keyword;
    private final int level;

    JavaVisibility(String keyword, int level) {
        this.keyword = keyword;
        this.level = level;
    }

    public String keyword() {
        return keyword;
    }

    public int level() {
        return level;
    }

    /**
     * Check if this visibility is at least as permissive as other.
     * PUBLIC.isAtLeast(PROTECTED) == true
     */
    public boolean isAtLeast(JavaVisibility other) {
        return this.level >= other.level;
    }

    /**
     * Check if this visibility is more restrictive than other.
     * PRIVATE.isMoreRestrictive(PUBLIC) == true
     */
    public boolean isMoreRestrictive(JavaVisibility other) {
        return this.level < other.level;
    }

    /**
     * Get the most permissive of two visibilities.
     */
    public static JavaVisibility max(JavaVisibility a, JavaVisibility b) {
        return a.level >= b.level ? a : b;
    }

    /**
     * Get the most restrictive of two visibilities.
     */
    public static JavaVisibility min(JavaVisibility a, JavaVisibility b) {
        return a.level <= b.level ? a : b;
    }

    @Override
    public String toString() {
        return keyword;
    }
}
