package com.adam.agri.planner.demo.state;

import com.adam.agri.planner.core.state.*;
import com.adam.agri.planner.symbolic.ontology.computer.LanguageRegistry;
import com.adam.agri.planner.symbolic.ontology.computer.SoftwareSystem;
import com.adam.agri.planner.symbolic.ontology.computer.code.AbstractType;
import com.adam.agri.planner.symbolic.ontology.computer.code.Package;
import com.adam.agri.planner.symbolic.ontology.computer.code.language.java.*;
import com.adam.agri.planner.symbolic.ontology.upper.*;

import java.nio.file.Path;
import java.util.*;

/**
 * State representing a software system using the ontology.
 *
 * This state wraps a SoftwareSystem ontology entity and provides
 * planner-compatible operations while maintaining rich semantic structure.
 * SoftwareSystem represents an abstract, versioned software entity (Layer 3-4 DOLCE).
 */
public class SoftwareSystemState extends SymbolicState {

    private final SoftwareSystem softwareSystem;
    private final Path outputRoot;

    /**
     * Create a new software system state.
     */
    public SoftwareSystemState(StateId id, String name, String version, Path outputRoot) {
        this(id, name, version, outputRoot, "java");
    }

    /**
     * Create a new software system state with specified language.
     */
    public SoftwareSystemState(StateId id, String name, String version, Path outputRoot, String language) {
        super(id);
        this.softwareSystem = new SoftwareSystem(
            EntityId.of("ss:" + name),
            Set.of(),
            name,
            version,
            language
        );
        this.outputRoot = outputRoot;
    }

    /**
     * Private constructor for copy operations.
     */
    private SoftwareSystemState(StateId id,
         Set<Predicate> predicates,
         Map<String, Object> bindings,
         SoftwareSystem softwareSystem,
         Path outputRoot) {
        super(id, predicates, bindings);
        this.softwareSystem = softwareSystem;
        this.outputRoot = outputRoot;
    }

    /**
     * Get the underlying SoftwareSystem ontology entity.
     */
    public SoftwareSystem getSoftwareSystem() {
        return softwareSystem;
    }

    /**
     * Get the output root path for file generation.
     */
    public Path getOutputRoot() {
        return outputRoot;
    }

    /**
     * Get the programming language of this system.
     */
    public String getLanguage() {
        return softwareSystem.getLanguage();
    }

    /**
     * Check if the system has a package.
     */
    public boolean hasPackage(String packageName) {
        return softwareSystem.hasPackage(packageName);
    }

    /**
     * Get a package by name (generic interface).
     */
    public Optional<Package> getPackageGeneric(String packageName) {
        return softwareSystem.getPackages().stream()
            .filter(p -> p.getQualifiedName().equals(packageName))
            .findFirst();
    }

    /**
     * Get a package by name (deprecated, use generic version).
     * @deprecated Use getPackageGeneric or adapt to language-specific APIs
     */
    @Deprecated
    public Optional<JavaPackage> getPackage(String packageName) {
        return getPackageGeneric(packageName)
            .map(this::toJavaPackage);
    }

    /**
     * Add a package to the software system (generic).
     */
    public SoftwareSystemState withPackage(Package pkg) {
        SoftwareSystemState copy = copyInternal();
        copy.softwareSystem.addPackage(pkg);
        return copy;
    }

    /**
     * Add a package (deprecated constructor).
     * @deprecated Use withPackage(Package) with language-agnostic packages
     */
    @Deprecated
    public SoftwareSystemState withPackage(JavaPackage pkg) {
        Package genericPkg = LanguageRegistry.getInstance()
            .createPackage(getLanguage(), pkg.getQualifiedName())
            .orElse(new JavaPackageImpl(pkg.getQualifiedName(), null, '.', "java"));
        return withPackage(genericPkg);
    }

    /**
     * Check if the system has a type with the given name.
     */
    public boolean hasType(String typeName) {
        return softwareSystem.hasTypeByName(typeName);
    }

    /**
     * Check if the system has a class with the given name.
     * @deprecated Use hasType instead
     */
    @Deprecated
    public boolean hasClass(String className) {
        return hasType(className);
    }

    /**
     * Get a type by simple name (generic interface).
     */
    public Optional<AbstractType> getTypeGeneric(String typeName) {
        return softwareSystem.findType(typeName);
    }

    /**
     * Get a class by simple name.
     * @deprecated Use getTypeGeneric instead
     */
    @Deprecated
    public Optional<JavaType> getClass(String className) {
        return getTypeGeneric(className)
            .filter(t -> t instanceof JavaType)
            .map(t -> (JavaType) t);
    }

    /**
     * Get all types in a package (generic).
     */
    public Set<AbstractType> getTypesInPackage(String packageName) {
        return getPackageGeneric(packageName)
            .map(Package::getTypes)
            .map(Set::copyOf)
            .orElse(Set.of());
    }

    /**
     * Get all classes in a package.
     * @deprecated Use getTypesInPackage
     */
    @Deprecated
    public Set<JavaType> getClassesInPackage(String packageName) {
        return getPackageGeneric(packageName)
            .map(Package::getTypes)
            .map(types -> {
                Set<JavaType> javaTypes = new HashSet<>();
                for (AbstractType t : types) {
                    if (t instanceof JavaType) {
                        javaTypes.add((JavaType) t);
                    }
                }
                return javaTypes;
            })
            .orElse(Set.of());
    }

    /**
     * Add a type to a package.
     */
    public SoftwareSystemState withType(String packageName, AbstractType type) {
        SoftwareSystemState copy = copyInternal();
        copy.softwareSystem.getPackages().stream()
            .filter(p -> p.getQualifiedName().equals(packageName))
            .findFirst()
            .ifPresent(pkg -> {
                // This would need GenericPackage to support addType
            });
        return copy;
    }

    /**
     * Add a type to a package (deprecated).
     * @deprecated Use withType with AbstractType
     */
    @Deprecated
    public SoftwareSystemState withType(String packageName, JavaType type) {
        return withType(packageName, (AbstractType) type);
    }

    /**
     * Add a type by name to a package.
     * For now this just marks the intent - the actual type will be created via LanguageProvider.
     */
    public SoftwareSystemState withType(String packageName, String typeName) {
        // For now, this is a placeholder - in a full implementation,
        // we would create the type via LanguageProvider and add it to the package
        SoftwareSystemState copy = copyInternal();
        // Mark that we intend to add this type
        return copy;
    }

    /**
     * Get all packages in the system (generic).
     */
    public Set<Package> getPackagesGeneric() {
        return softwareSystem.getPackages();
    }

    /**
     * Get all packages in the system.
     * @deprecated Use getPackagesGeneric
     */
    @Deprecated
    public Set<JavaPackage> getPackages() {
        Set<JavaPackage> result = new HashSet<>();
        for (Package pkg : getPackagesGeneric()) {
            if (pkg instanceof JavaPackageImpl) {
                // Convert - for legacy compatibility
            }
        }
        return result;
    }

    /**
     * Get the system name.
     */
    public String getSystemName() {
        return softwareSystem.getName();
    }

    /**
     * Get the system version.
     */
    public String getSystemVersion() {
        return softwareSystem.getVersion();
    }

    /**
     * Get artifact ID (Maven/Gradle style).
     */
    public String getArtifactId() {
        return softwareSystem.getArtifactId();
    }

    /**
     * Get all dependencies.
     */
    public Set<String> getDependencies() {
        return softwareSystem.getDependencies();
    }

    /**
     * Add a dependency.
     */
    public void addDependency(String dependency) {
        softwareSystem.addDependency(dependency);
    }

    /**
     * Get all types in all packages (generic).
     */
    public Set<AbstractType> getAllTypes() {
        Set<AbstractType> types = new HashSet<>();
        for (Package pkg : softwareSystem.getPackages()) {
            types.addAll(pkg.getTypes());
        }
        return types;
    }

    /**
     * Get all classes in the system.
     * @deprecated Use getAllTypes
     */
    @Deprecated
    public Set<JavaType> getAllClasses() {
        Set<JavaType> types = new HashSet<>();
        for (Package pkg : softwareSystem.getPackages()) {
            for (AbstractType t : pkg.getTypes()) {
                if (t instanceof JavaType) {
                    types.add((JavaType) t);
                }
            }
        }
        return types;
    }

    /**
     * Count total types in the system.
     */
    public int getTypeCount() {
        return softwareSystem.getPackages().stream()
            .mapToInt(p -> p.getTypes().size())
            .sum();
    }

    /**
     * Get a system property.
     */
    public Optional<String> getProperty(String key) {
        return softwareSystem.getProperty(key);
    }

    /**
     * Set a system property.
     */
    public void setProperty(String key, String value) {
        softwareSystem.setProperty(key, value);
    }

    /**
     * Check if system satisfies a goal condition.
     */
    public boolean satisfiesGoal(Set<String> requiredTypes, Set<String> requiredPackages) {
        boolean typesExist = requiredTypes.isEmpty() ||
            requiredTypes.stream().allMatch(this::hasType);
        boolean packagesExist = requiredPackages.isEmpty() ||
            requiredPackages.stream().allMatch(this::hasPackage);
        return typesExist && packagesExist;
    }

    /**
     * Alias for satisfiesGoal using 'Class' terminology.
     * @deprecated Use satisfiesGoal
     */
    @Deprecated
    public boolean satisfiesGoalClasses(Set<String> requiredClasses, Set<String> requiredPackages) {
        return satisfiesGoal(requiredClasses, requiredPackages);
    }

    /**
     * Internal copy method.
     */
    private SoftwareSystemState copyInternal() {
        return new SoftwareSystemState(
            StateId.generate(),
            getPredicates(),
            Map.of(),
            this.softwareSystem,
            this.outputRoot
        );
    }

    /**
     * Helper to convert generic Package to JavaPackage (for compatibility).
     */
    private JavaPackage toJavaPackage(Package pkg) {
        // Create a temporary JavaPackage wrapper for compatibility
        return new JavaPackage(
            EntityId.of("pkg:" + pkg.getQualifiedName()),
            null, // Properties
            pkg.getQualifiedName(),
            Optional.ofNullable(pkg.getParent().orElse(null))
                .map(p -> toJavaPackage(p))
        );
    }

    @Override
    public String toString() {
        return "SoftwareSystemState[" + softwareSystem.getName() +
            ":" + softwareSystem.getVersion() +
            " (" + softwareSystem.getLanguage() + "), packages=" +
            softwareSystem.getPackages().size() + ", types=" + getTypeCount() + "]";
    }
}