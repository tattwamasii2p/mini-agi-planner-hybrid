package com.adam.agri.planner.symbolic.ontology.computer.code.language.java;

import com.adam.agri.planner.symbolic.ontology.upper.*;

import java.util.*;

/**
 * Java Package - namespace container for Java types.
 *
 * Layer 5-6: Package structure as abstract namespace.
 */
public class JavaPackage extends AbstractJavaEntity {

    private final List<JavaPackage> subPackages;
    private final Set<JavaType> containedTypes;
    private final Optional<JavaPackage> parent;

    public JavaPackage(EntityId id, Set<Property> properties,
                       String qualifiedName, Optional<JavaPackage> parent) {
        super(id, properties, qualifiedName, JavaVisibility.PUBLIC,
            Set.of(), Set.of(), Optional.empty());
        this.parent = parent;
        this.subPackages = new ArrayList<>();
        this.containedTypes = new HashSet<>();
    }

    public List<JavaPackage> getSubPackages() {
        return List.copyOf(subPackages);
    }

    public void addSubPackage(JavaPackage pkg) {
        subPackages.add(pkg);
    }

    public Set<JavaType> getContainedTypes() {
        return Set.copyOf(containedTypes);
    }

    public void addType(JavaType type) {
        containedTypes.add(type);
    }

    public Optional<JavaPackage> getParent() {
        return parent;
    }

    /**
     * Check if this package contains the given type.
     */
    public boolean containsType(String typeName) {
        return containedTypes.stream()
            .anyMatch(t -> t.getSimpleName().equals(typeName));
    }

    /**
     * Find type by simple name in this package.
     */
    public Optional<JavaType> findType(String simpleName) {
        return containedTypes.stream()
            .filter(t -> t.getSimpleName().equals(simpleName))
            .findFirst();
    }

    /**
     * Get package info file path.
     */
    public String getPackageInfoPath() {
        return qualifiedName.replace('.', '/') + "/package-info.java";
    }

    @Override
    protected boolean isInSamePackage(JavaPackage other) {
        return this.qualifiedName.equals(other.qualifiedName);
    }

    @Override
    public String toString() {
        return "JavaPackage[" + qualifiedName + "]";
    }
}
