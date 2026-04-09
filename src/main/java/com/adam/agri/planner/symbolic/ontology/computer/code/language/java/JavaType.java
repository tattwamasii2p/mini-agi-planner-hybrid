package com.adam.agri.planner.symbolic.ontology.computer.code.language.java;

import com.adam.agri.planner.symbolic.ontology.upper.*;

import java.util.*;

/**
 * Java Type - represents Java type system constructs.
 *
 * Layer 5-6: Types as abstract entities in the type system.
 * Includes: classes, interfaces, enums, records, primitives, arrays, generics.
 */
public abstract class JavaType extends AbstractJavaEntity {

    protected final Optional<JavaPackage> pkg;
    protected final JavaTypeKind kind;
    protected final Optional<JavaType> superType;
    protected final Set<JavaType> interfaces;
    protected final Set<JavaType> typeParameters;
    protected final boolean isGeneric;

    public enum JavaTypeKind {
        CLASS, INTERFACE, ENUM, RECORD, ANNOTATION,
        PRIMITIVE, ARRAY, GENERIC_TYPE_PARAMETER,
        WILDCARD, UNION, INTERSECTION
    }

    public JavaType(EntityId id, Set<Property> properties,
                    String qualifiedName, JavaVisibility visibility,
                    Set<JavaAnnotation> annotations, Set<JavaModifier> modifiers,
                    Optional<JavaDoc> documentation,
                    Optional<JavaPackage> pkg, JavaTypeKind kind,
                    Optional<JavaType> superType, Set<JavaType> interfaces,
                    Set<JavaType> typeParameters, boolean isGeneric) {
        super(id, properties, qualifiedName, visibility, annotations, modifiers, documentation);
        this.pkg = pkg;
        this.kind = kind;
        this.superType = superType;
        this.interfaces = Set.copyOf(interfaces);
        this.typeParameters = Set.copyOf(typeParameters);
        this.isGeneric = isGeneric;
    }

    public Optional<JavaPackage> getPackage() {
        return pkg;
    }

    public JavaTypeKind getKind() {
        return kind;
    }

    public Optional<JavaType> getSuperType() {
        return superType;
    }

    public Set<JavaType> getInterfaces() {
        return interfaces;
    }

    public Set<JavaType> getTypeParameters() {
        return typeParameters;
    }

    public boolean isGeneric() {
        return isGeneric;
    }

    public boolean isAbstract() {
        return hasModifier(AbstractJavaEntity.JavaModifier.ABSTRACT);
    }

    public boolean isFinal() {
        return hasModifier(AbstractJavaEntity.JavaModifier.FINAL);
    }

    public boolean isInterface() {
        return kind == JavaTypeKind.INTERFACE;
    }

    public boolean isEnum() {
        return kind == JavaTypeKind.ENUM;
    }

    public boolean isRecord() {
        return kind == JavaTypeKind.RECORD;
    }

    public boolean isPrimitive() {
        return kind == JavaTypeKind.PRIMITIVE;
    }

    public boolean isArray() {
        return kind == JavaTypeKind.ARRAY;
    }

    /**
     * Check if this type is assignable from other type.
     */
    public boolean isAssignableFrom(JavaType other) {
        if (this.equals(other)) return true;
        if (this.isPrimitive() || other.isPrimitive()) return false;

        // Check supertype chain
        Optional<JavaType> sup = other.getSuperType();
        while (sup.isPresent()) {
            if (sup.get().equals(this)) return true;
            sup = sup.get().getSuperType();
        }

        // Check interfaces
        return other.getInterfaces().contains(this);
    }

    /**
     * Check if this type implements given interface.
     */
    public boolean implementsInterface(JavaType iface) {
        if (!iface.isInterface()) return false;
        return interfaces.contains(iface);
    }

    @Override
    protected boolean isInSamePackage(JavaPackage other) {
        return pkg.map(p -> p.equals(other)).orElse(false);
    }

    @Override
    public String toString() {
        return "JavaType[" + qualifiedName + "]";
    }

    // ==================== PRIMITIVE TYPES ====================

    public static final JavaPrimitiveType VOID = new JavaPrimitiveType("void", 0);
    public static final JavaPrimitiveType BOOLEAN = new JavaPrimitiveType("boolean", 1);
    public static final JavaPrimitiveType BYTE = new JavaPrimitiveType("byte", 8);
    public static final JavaPrimitiveType CHAR = new JavaPrimitiveType("char", 16);
    public static final JavaPrimitiveType SHORT = new JavaPrimitiveType("short", 16);
    public static final JavaPrimitiveType INT = new JavaPrimitiveType("int", 32);
    public static final JavaPrimitiveType LONG = new JavaPrimitiveType("long", 64);
    public static final JavaPrimitiveType FLOAT = new JavaPrimitiveType("float", 32);
    public static final JavaPrimitiveType DOUBLE = new JavaPrimitiveType("double", 64);

    // ==================== BOXED TYPES ====================

    public static final JavaBoxedType BOXED_BOOLEAN = new JavaBoxedType("java.lang.Boolean", BOOLEAN);
    public static final JavaBoxedType BOXED_BYTE = new JavaBoxedType("java.lang.Byte", BYTE);
    public static final JavaBoxedType BOXED_CHAR = new JavaBoxedType("java.lang.Character", CHAR);
    public static final JavaBoxedType BOXED_SHORT = new JavaBoxedType("java.lang.Short", SHORT);
    public static final JavaBoxedType BOXED_INT = new JavaBoxedType("java.lang.Integer", INT);
    public static final JavaBoxedType BOXED_LONG = new JavaBoxedType("java.lang.Long", LONG);
    public static final JavaBoxedType BOXED_FLOAT = new JavaBoxedType("java.lang.Float", FLOAT);
    public static final JavaBoxedType BOXED_DOUBLE = new JavaBoxedType("java.lang.Double", DOUBLE);

    // ==================== COMMON TYPES ====================

    public static final JavaReferenceType OBJECT = new JavaReferenceType(
        EntityId.of("java.lang.Object"),
        Set.of(),
        "java.lang.Object",
        JavaVisibility.PUBLIC,
        Set.of(), Set.of(), Optional.empty(),
        null, Optional.empty(), Set.of(),
        Optional.empty(), Set.of(), Set.of(), Set.of(), false
    );

    public static final JavaReferenceType STRING = new JavaReferenceType(
        EntityId.of("java.lang.String"),
        Set.of(),
        "java.lang.String",
        JavaVisibility.PUBLIC,
        Set.of(), Set.of(new JavaModifier[] {JavaModifier.FINAL}), Optional.empty(),
        null, Optional.of(OBJECT), Set.of(),
        Optional.empty(), Set.of(), Set.of(), Set.of(), false
    );
}

/**
 * Primitive type (int, boolean, etc.).
 */
class JavaPrimitiveType extends JavaType {
    private final int bitWidth;

    JavaPrimitiveType(String name, int bitWidth) {
        super(EntityId.of(name), Set.of(), name, JavaVisibility.PUBLIC,
            Set.of(), Set.of(), Optional.empty(),
            Optional.empty(), JavaTypeKind.PRIMITIVE,
            Optional.empty(), Set.of(), Set.of(), false);
        this.bitWidth = bitWidth;
    }

    public int getBitWidth() {
        return bitWidth;
    }

    @Override
    public String toString() {
        return "primitive[" + qualifiedName + "]";
    }
}

/**
 * Boxed primitive type (Integer, Boolean, etc.).
 */
class JavaBoxedType extends JavaType {
    private final JavaPrimitiveType primitive;

    JavaBoxedType(String name, JavaPrimitiveType primitive) {
        super(EntityId.of(name), Set.of(), name, JavaVisibility.PUBLIC,
            Set.of(), Set.of(new JavaModifier[]{JavaModifier.FINAL}), Optional.empty(),
            Optional.empty(), JavaTypeKind.CLASS, Optional.empty(), Set.of(), Set.of(), false);
        this.primitive = primitive;
    }

    public JavaPrimitiveType getPrimitive() {
        return primitive;
    }

    @Override
    public String toString() {
        return "boxed[" + qualifiedName + "=" + primitive + "]";
    }
}

/**
 * Reference type (class, interface, enum, record).
 */
class JavaReferenceType extends JavaType {

    private final Set<JavaField> fields;
    private final Set<JavaMethod> methods;
    private final Set<JavaConstructor> constructors;

    public JavaReferenceType(EntityId id, Set<Property> properties,
                              String qualifiedName, JavaVisibility visibility,
                              Set<JavaAnnotation> annotations,
                              Set<AbstractJavaEntity.JavaModifier> modifiers,
                              Optional<JavaDoc> documentation,
                              JavaPackage pkg, Optional<JavaType> superType,
                              Set<JavaType> interfaces,
                              Optional<JavaPackage> optPkg, Set<JavaType> typeParameters,
                              Set<JavaField> fields, Set<JavaMethod> methods,
                              boolean isGeneric) {
        super(id, properties, qualifiedName, visibility, annotations, modifiers,
            documentation, optPkg, JavaTypeKind.CLASS, superType, interfaces,
            typeParameters, isGeneric);
        this.fields = Set.copyOf(fields);
        this.methods = Set.copyOf(methods);
        this.constructors = new HashSet<>();
    }

    public Set<JavaField> getFields() {
        return fields;
    }

    public Set<JavaMethod> getMethods() {
        return methods;
    }

    public Set<JavaConstructor> getConstructors() {
        return constructors;
    }

    public Optional<JavaMethod> findMethod(String name, JavaType... paramTypes) {
        return methods.stream()
            .filter(m -> m.getName().equals(name))
            .filter(m -> m.matchesSignature(paramTypes))
            .findFirst();
    }

    public Optional<JavaField> findField(String name) {
        return fields.stream()
            .filter(f -> f.getName().equals(name))
            .findFirst();
    }

    @Override
    public String toString() {
        return "JavaClass[" + qualifiedName + "]";
    }
}

/**
 * Array type.
 */
class JavaArrayType extends JavaType {
    private final JavaType componentType;
    private final int dimensions;

    JavaArrayType(JavaType componentType, int dimensions) {
        super(EntityId.of(componentType.qualifiedName + "[]".repeat(dimensions)),
            Set.of(),
            componentType.qualifiedName + "[]".repeat(dimensions),
            JavaVisibility.PUBLIC, Set.of(), Set.of(), Optional.empty(),
            componentType.pkg, JavaTypeKind.ARRAY,
            Optional.empty(), Set.of(), Set.of(), false);
        this.componentType = componentType;
        this.dimensions = dimensions;
    }

    public JavaType getComponentType() {
        return componentType;
    }

    public int getDimensions() {
        return dimensions;
    }

    @Override
    public String toString() {
        return componentType + "[]".repeat(dimensions);
    }
}

/**
 * Generic type parameter.
 */
class JavaTypeParameter extends JavaType {
    private final Set<JavaType> bounds;

    JavaTypeParameter(String name, Set<JavaType> bounds) {
        super(EntityId.of(name), Set.of(), name, JavaVisibility.PUBLIC,
            Set.of(), Set.of(), Optional.empty(),
            Optional.empty(), JavaTypeKind.GENERIC_TYPE_PARAMETER,
            Optional.empty(), Set.of(), Set.of(), true);
        this.bounds = Set.copyOf(bounds);
    }

    public Set<JavaType> getBounds() {
        return bounds;
    }

    @Override
    public String toString() {
        return "T[" + qualifiedName + "]";
    }
}

/**
 * Wildcard type (?, ? extends T, ? super T).
 */
class JavaWildcardType extends JavaType {
    private final Optional<JavaType> upperBound;
    private final Optional<JavaType> lowerBound;

    JavaWildcardType(Optional<JavaType> upperBound, Optional<JavaType> lowerBound) {
        super(EntityId.of("?"), Set.of(), "?", JavaVisibility.PUBLIC,
            Set.of(), Set.of(), Optional.empty(),
            Optional.empty(), JavaTypeKind.WILDCARD,
            Optional.empty(), Set.of(), Set.of(), false);
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
    }

    public Optional<JavaType> getUpperBound() {
        return upperBound;
    }

    public Optional<JavaType> getLowerBound() {
        return lowerBound;
    }

    public boolean isUnbounded() {
        return upperBound.isEmpty() && lowerBound.isEmpty();
    }

    @Override
    public String toString() {
        if (lowerBound.isPresent()) {
            return "? super " + lowerBound.get();
        }
        if (upperBound.isPresent() && !upperBound.get().equals(JavaType.OBJECT)) {
            return "? extends " + upperBound.get();
        }
        return "?";
    }
}
