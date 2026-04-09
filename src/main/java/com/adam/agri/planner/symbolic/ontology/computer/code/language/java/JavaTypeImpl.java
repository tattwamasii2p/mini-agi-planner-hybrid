package com.adam.agri.planner.symbolic.ontology.computer.code.language.java;

import com.adam.agri.planner.symbolic.ontology.computer.code.AbstractType;
import com.adam.agri.planner.symbolic.ontology.computer.code.TypeKind;
import com.adam.agri.planner.symbolic.ontology.computer.code.Visibility;
import com.adam.agri.planner.symbolic.ontology.computer.code.Modifier;
import com.adam.agri.planner.symbolic.ontology.computer.code.Package;

import java.util.*;

/**
 * Java-specific implementation of the generic AbstractType interface.
 * This class provides Java-specific type handling while conforming to the
 * language-agnostic interface.
 */
public class JavaTypeImpl implements AbstractType {
    private final String qualifiedName;
    private final String simpleName;
    private final TypeKind typeKind;
    private final Package packageContext;
    private final Visibility visibility;
    private final Set<Modifier> modifiers;
    private final JavaTypeImpl supertype;
    private final List<JavaTypeImpl> interfaces;
    private final List<JavaTypeImpl> typeParameters;
    private final List<JavaTypeImpl> typeArguments;
    private final boolean isPrimitive;
    private final boolean isParameterized;
    private final JavaTypeImpl componentType;
    private final JavaTypeImpl wrapperType;

    public JavaTypeImpl(String qualifiedName, String simpleName, TypeKind typeKind,
                       Package packageContext, JavaTypeImpl supertype,
                       List<JavaTypeImpl> interfaces, List<JavaTypeImpl> typeParameters,
                       boolean isPrimitive, JavaTypeImpl componentType,
                       JavaTypeImpl wrapperType, Visibility visibility,
                       Set<Modifier> modifiers) {
        this.qualifiedName = qualifiedName;
        this.simpleName = simpleName;
        this.typeKind = typeKind;
        this.packageContext = packageContext;
        this.supertype = supertype;
        this.interfaces = interfaces != null ? interfaces : new ArrayList<>();
        this.typeParameters = typeParameters != null ? typeParameters : new ArrayList<>();
        this.typeArguments = new ArrayList<>();
        this.isPrimitive = isPrimitive;
        this.isParameterized = false;
        this.componentType = componentType;
        this.wrapperType = wrapperType;
        this.visibility = visibility;
        this.modifiers = modifiers != null ? modifiers : new HashSet<>();
    }

    @Override
    public String getQualifiedName() {
        return qualifiedName;
    }

    @Override
    public String getSimpleName() {
        return simpleName;
    }

    @Override
    public TypeKind getTypeKind() {
        return typeKind;
    }

    @Override
    public Package getPackage() {
        return packageContext;
    }

    @Override
    public boolean isAssignableFrom(AbstractType other) {
        if (!(other instanceof JavaTypeImpl)) {
            return false;
        }

        JavaTypeImpl that = (JavaTypeImpl) other;
        return this.qualifiedName.equals(that.qualifiedName) ||
               (supertype != null && supertype.isAssignableFrom(that)) ||
               interfaces.stream().anyMatch(iface -> iface.isAssignableFrom(that));
    }

    @Override
    public boolean isSubtypeOf(AbstractType other) {
        if (!(other instanceof JavaTypeImpl)) {
            return false;
        }

        JavaTypeImpl that = (JavaTypeImpl) other;
        return supertype != null && (supertype.qualifiedName.equals(that.qualifiedName) ||
               supertype.isSubtypeOf(that));
    }

    @Override
    public JavaTypeImpl getSupertype() {
        return supertype;
    }

    @Override
    public List<AbstractType> getInterfaces() {
        return new ArrayList<>(interfaces);
    }

    @Override
    public boolean isParameterized() {
        return isParameterized || !typeParameters.isEmpty();
    }

    @Override
    public List<AbstractType> getTypeParameters() {
        return new ArrayList<>(typeParameters);
    }

    @Override
    public AbstractType getGenericType() {
        if (isParameterized) {
            return this;
        }
        return null;
    }

    @Override
    public List<AbstractType> getTypeArguments() {
        return new ArrayList<>(typeArguments);
    }

    @Override
    public boolean isOptional() {
        return false; // Java doesn't have native optional types
    }

    @Override
    public AbstractType getActualType() {
        return this;
    }

    @Override
    public boolean isArrayType() {
        return componentType != null;
    }

    @Override
    public AbstractType getComponentType() {
        return componentType;
    }

    @Override
    public boolean hasBounds() {
        return !typeParameters.isEmpty();
    }

    @Override
    public List<AbstractType> getBounds() {
        return new ArrayList<>(typeParameters);
    }

    @Override
    public Visibility getVisibility() {
        return visibility;
    }

    @Override
    public Set<Modifier> getModifiers() {
        return new HashSet<>(modifiers);
    }

    @Override
    public boolean isPrimitive() {
        return isPrimitive;
    }

    @Override
    public AbstractType getWrapperType() {
        return wrapperType;
    }

    @Override
    public boolean isEnumeration() {
        return typeKind == TypeKind.ENUM;
    }

    @Override
    public String getEnclosingType() {
        if (packageContext != null) {
            return packageContext.getQualifiedName();
        }
        return null;
    }

    @Override
    public AbstractType subtype(AbstractType subType) {
        if (subType instanceof JavaTypeImpl) {
            JavaTypeImpl javaSubType = (JavaTypeImpl) subType;
            return new JavaTypeImpl(
                javaSubType.qualifiedName,
                javaSubType.simpleName,
                javaSubType.typeKind,
                javaSubType.packageContext,
                this,
                javaSubType.interfaces,
                javaSubType.typeParameters,
                javaSubType.isPrimitive,
                javaSubType.componentType,
                javaSubType.wrapperType,
                javaSubType.visibility,
                javaSubType.modifiers
            );
        }
        return subType;
    }

    @Override
    public AbstractType withTypeArguments(List<AbstractType> typeArguments) {
        List<JavaTypeImpl> javaTypeArgs = new ArrayList<>();
        for (AbstractType arg : typeArguments) {
            if (arg instanceof JavaTypeImpl) {
                javaTypeArgs.add((JavaTypeImpl) arg);
            }
        }

        return new JavaTypeImpl(
            this.qualifiedName,
            this.simpleName,
            this.typeKind,
            this.packageContext,
            this.supertype,
            this.interfaces,
            javaTypeArgs,
            this.isPrimitive,
            this.componentType,
            this.wrapperType,
            this.visibility,
            this.modifiers
        );
    }

    @Override
    public AbstractType toArray() {
        return new JavaTypeImpl(
            this.qualifiedName + "[]",
            this.simpleName + "[]",
            TypeKind.ARRAY,
            this.packageContext,
            null, // Arrays don't have supertypes in the same way
            new ArrayList<>(),
            new ArrayList<>(),
            false,
            this,
            null,
            this.visibility,
            this.modifiers
        );
    }

    @Override
    public AbstractType withPackage(Package pkg) {
        return new JavaTypeImpl(
            pkg != null ? pkg.getQualifiedName() + "." + this.simpleName : this.simpleName,
            this.simpleName,
            this.typeKind,
            pkg,
            this.supertype,
            this.interfaces,
            this.typeParameters,
            this.isPrimitive,
            this.componentType,
            this.wrapperType,
            this.visibility,
            this.modifiers
        );
    }

    @Override
    public boolean typeEquals(AbstractType other) {
        if (this == other) return true;
        if (!(other instanceof JavaTypeImpl)) return false;
        JavaTypeImpl that = (JavaTypeImpl) other;
        return qualifiedName.equals(that.qualifiedName) &&
               typeKind == that.typeKind;
    }

    @Override
    public String getLanguage() {
        return "java";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JavaTypeImpl that = (JavaTypeImpl) o;
        return qualifiedName.equals(that.qualifiedName);
    }

    @Override
    public int hashCode() {
        return qualifiedName.hashCode();
    }

    @Override
    public String toString() {
        return "JavaType{" + qualifiedName + ", " + typeKind + "}";
    }
}