package com.adam.agri.planner.symbolic.ontology.computer.code;

import java.util.EnumSet;
import java.util.Set;

/**
 * Language-agnostic modifiers for code entities.
 * This replaces Java's specific modifier system with a more generic approach
 * that can be mapped to various programming language modifier concepts.
 */
public enum Modifier {
    // Basic access modifiers
    PUBLIC("public", "Accessible from anywhere"),
    PRIVATE("private", "Accessible only within declaring scope"),
    PROTECTED("protected", "Accessible within inheritance hierarchy"),
    INTERNAL("internal", "Accessible within same assembly/module"),

    // Type modifiers
    STATIC("static", "Belongs to type rather than instance"),
    ABSTRACT("abstract", "Must be implemented by subclasses"),
    FINAL("final", "Cannot be extended or overridden"),
    SEALED("sealed", "Restricted inheritance to specific types"),
    INTERSECT("intersect", "Type intersection (TypeScript style)"),

    // Mutability modifiers
    READONLY("readonly", "Cannot be modified after definition"),
    MUTABLE("mutable", "Can be modified"),
    IMMUTABLE("immutable", "Cannot be modified after creation"),

    // Synchronization and thread safety
    SYNCHRONIZED("synchronized", "Thread-safe access"),
    VOLATILE("volatile", "Variable may change outside of normal flow"),
    ATOMIC("atomic", "Atomic operations"),

    // Generic and template modifiers
    GENERIC("generic", "Parameterized type"),
    TEMPLATE("template", "Template type (C++ style)"),
    CONCEPT("concept", "Concept-based generic (C++20)"),

    // Parameter modifiers
    BY_REFERENCE("ref", "Passed by reference"),
    BY_VALUE("value", "Passed by value"),
    OUT("out", "Output parameter"),
    OPTIONAL("optional", "Parameter may be omitted"),

    // Variable storage modifiers
    LOCAL("local", "Local variable"),
    CONST("const", "Constant value"),
    LAMBDA("lambda", "Anonymous function"),

    // Class membership
    INNER("inner", "Non-static inner class"),
    ANONYMOUS("anonymous", "Anonymous type"),

    // Extension and override
    OVERRIDE("override", "Overrides base method"),
    VIRTUAL("virtual", "Can be overridden"),
    NEW("new", "Hides rather than overrides"),
    DEFAULT("default", "Default implementation"),

    // Function modifiers
    ASYNC("async", "Asynchronous operation"),
    AWAIT("await", "Awaitable operation"),
    YIELD("yield", "Generator function"),
    LAZY("lazy", "Lazy evaluation"),

    // Performance modifiers
    INLINE("inline", "Inline expanded"),
    NO_INLINE("noinline", "Prevent inlining"),
    FORCE_INLINE("forcedinline", "Force inlining"),

    // Memory and lifetime
    TRANSIENT("transient", "Not serialized"),
    GARBAGE_COLLECTED("gc", "Garbage collected"),
    MANUAL("manual", "Manual memory management"),

    // Null safety
    NULLABLE("nullable", "Can be null"),
    NON_NULLABLE("nonnull", "Cannot be null"),

    // Factory and instance creation
    SINGLETON("singleton", "Single instance"),
    PROTOTYPE("prototype", "New instance for each request"),
    FACTORY("factory", "A factory method"),

    // Test and documentation
    TEST("test", "Used for testing"),
    DOCUMENTED("documented", "Has formal documentation"),
    DEPRECATED("deprecated", "Marked for deprecation"),
    EXPERIMENTAL("experimental", "Experimental feature"),

    // Language-specific flags
    EXTERN("extern", "External linkage"),
    EXPORT("export", "Visible to other modules"),
    IMPORT("import", "Imported from another module"),
    KEYWORD("keyword", "Language keyword"),

    // Error and safety
    THROWS("throws", "Declares checked exceptions"),
    SAFE("safe", "Memory safe"),
    UNSAFE("unsafe", "Memory unsafe operations"),

    // Generator and meta modifiers
    GENERATED("generated", "Auto-generated code"),
    HANDWRITTEN("handwritten", "Manually written code"),
    COMPILED("compiled", "Pre-compiled rather than interpreted");

    private final String keyword;
    private final String description;

    Modifier(String keyword, String description) {
        this.keyword = keyword;
        this.description = description;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getDescription() {
        return description;
    }

    public static EnumSet<Modifier> accessModifiers() {
        return EnumSet.of(PUBLIC, PRIVATE, PROTECTED, INTERNAL);
    }

    public static EnumSet<Modifier> inheritanceModifiers() {
        return EnumSet.of(ABSTRACT, FINAL, SEALED, VIRTUAL, OVERRIDE);
    }

    public static EnumSet<Modifier> scopeModifiers() {
        return EnumSet.of(STATIC, LOCAL, INNER, ANONYMOUS);
    }

    public static EnumSet<Modifier> parameterModifiers() {
        return EnumSet.of(BY_REFERENCE, BY_VALUE, OUT, OPTIONAL);
    }

    public static EnumSet<Modifier> modifiability() {
        return EnumSet.of(READONLY, MUTABLE, IMMUTABLE, CONST);
    }

    public static EnumSet<Modifier> threadSafety() {
        return EnumSet.of(SYNCHRONIZED, VOLATILE, ATOMIC);
    }

    public static EnumSet<Modifier> nullSafety() {
        return EnumSet.of(NULLABLE, NON_NULLABLE);
    }

    public static EnumSet<Modifier> genericModifiers() {
        return EnumSet.of(GENERIC, TEMPLATE, CONCEPT);
    }

    public static EnumSet<Modifier> instanceCreation() {
        return EnumSet.of(SINGLETON, PROTOTYPE, FACTORY);
    }
}