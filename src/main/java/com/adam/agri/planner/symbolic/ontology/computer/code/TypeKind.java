package com.adam.agri.planner.symbolic.ontology.computer.code;

/**
 * Language-agnostic type kinds for the generic type system.
 * This replaces the Java-specific JavaTypeKind with a more generic concept.
 */
public enum TypeKind {
    // Basic types
    PRIMITIVE("primitive", "A primitive built-in type"),
    OBJECT("object", "An object type that can have fields and methods"),
    CLASS("class", "A class type"),
    ARRAY("array", "A homogeneous collection type"),
    COLLECTION("collection", "A generic collection type"),

    // Language-specific constructions
    INTERFACE("interface", "A contract or protocol definition"),
    ENUM("enum", "An enumerated type"),
    ANNOTATION("annotation", "A metadata annotation type"),
    RECORD("record", "A data carrier type"),

    // Generics
    GENERIC_TYPE("generic", "A parameterized type"),
    TYPE_PARAMETER("type_param", "A generic type parameter"),
    WILDCARD_TYPE("wildcard", "A wildcard type parameter"),

    // Advanced types
    DELEGATE("delegate", "A reference type for methods (C# style)"),
    TRAIT("trait", "A common interface with implementations"),
    PROTOCOL("protocol", "A type definition for protocols"),
    STRUCT("struct", "A value type (C-style)"),
    UNION("union", "A sum type"),

    // Special types
    FUNCTIONAL("functional", "A function or lambda type"),
    VOID("void", "A void or unit type"),
    OPTIONAL("optional", "A nullable or optional type"),

    // Unknown/type system
    UNKNOWN("unknown", "An unknown or inferred type"),
    ERROR("error", "A type representing an error state");

    private final String simpleName;
    private final String description;

    TypeKind(String simpleName, String description) {
        this.simpleName = simpleName;
        this.description = description;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPrimitive() {
        return this == PRIMITIVE;
    }

    public boolean isComposite() {
        return this == OBJECT || this == COLLECTION || this == INTERFACE ||
               this == RECORD || this == STRUCT || this == CLASS;
    }

    public boolean isGeneric() {
        return this == GENERIC_TYPE || this == TYPE_PARAMETER || this == WILDCARD_TYPE;
    }

    public boolean isFunctional() {
        return this == FUNCTIONAL || this == DELEGATE;
    }
}