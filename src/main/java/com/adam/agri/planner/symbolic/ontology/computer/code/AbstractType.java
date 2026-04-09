package com.adam.agri.planner.symbolic.ontology.computer.code;

/**
 * Language-agnostic abstract type system interface.
 * This interface provides a generic foundation for type systems across different programming languages.
 * It replaces the Java-specific JavaType interface with a more flexible abstraction.
 */
public interface AbstractType {

    /**
     * Get the fully qualified name of this type including any namespace or package information.
     */
    String getQualifiedName();

    /**
     * Get the simple/short name of this type without any namespace or package qualifiers.
     */
    String getSimpleName();

    /**
     * Get the kind of type this represents (class, interface, primitive, etc.).
     */
    TypeKind getTypeKind();

    /**
     * Get the package or namespace this type belongs to.
     */
    Package getPackage();

    /**
     * Check if this type is assignable from the given type.
     * This is the language-agnostic equivalent of Java's isAssignableFrom.
     */
    boolean isAssignableFrom(AbstractType other);

    /**
     * Check if this type is a subtype or implements the given type, depending on the language's inheritance model.
     */
    boolean isSubtypeOf(AbstractType other);

    /**
     * Get the supertype for this type, or null if this is a root type.
     */
    AbstractType getSupertype();

    /**
     * Get the list of interfaces or protocols (depending on language) this type implements.
     */
    java.util.List<AbstractType> getInterfaces();

    /**
     * Check if this type is parameterized (generic).
     */
    boolean isParameterized();

    /**
     * Get the type parameters if this is a generic type.
     */
    java.util.List<AbstractType> getTypeParameters();

    /**
     * Get the generic type (raw type) if this is a parameterized type.
     */
    AbstractType getGenericType();

    /**
     * Get the generic type's type arguments if this is a parameterized type.
     */
    java.util.List<AbstractType> getTypeArguments();

    /**
     * Check if this type represents an optional or nullable type.
     */
    boolean isOptional();

    /**
     * Get the actual type if this is an optional/optional type.
     */
    AbstractType getActualType();

    /**
     * Check if this type is an array type.
     */
    boolean isArrayType();

    /**
     * Get the component type if this is an array type.
     */
    AbstractType getComponentType();

    /**
     * Check if this type has wildcards or generic bounds.
     */
    boolean hasBounds();

    /**
     * Get the bounds for this type if it has any.
     */
    java.util.List<AbstractType> getBounds();

    /**
     * Get the visibility/qualifier for this type (public, private, etc.)
     */
    Visibility getVisibility();

    /**
     * Get the modifiers for this type (static, final, etc.)
     */
    java.util.Set<Modifier> getModifiers();

    /**
     * Check if this type is primitive in the source language.
     */
    boolean isPrimitive();

    /**
     * Get the wrapper type if this is a primitive type.
     */
    AbstractType getWrapperType();

    /**
     * Check if this type is an enum/union type.
     */
    boolean isEnumeration();

    /**
     * Get the qualified name of the type's enclosing type or null.
     */
    String getEnclosingType();

    /**
     * Create a new type instance representing a subtype relationship.
     */
    AbstractType subtype(AbstractType subType);

    /**
     * Create a new parameterized type instance.
     */
    AbstractType withTypeArguments(java.util.List<AbstractType> typeArguments);

    /**
     * Create an array type for this type.
     */
    AbstractType toArray();

    /**
     * Create a qualified version of this type within the specified package.
     */
    AbstractType withPackage(Package pkg);

    /**
     * Check equality based on type contents rather than object identity.
     */
    boolean typeEquals(AbstractType other);

    /**
     * Get the language associated with this type system.
     */
    String getLanguage();
}