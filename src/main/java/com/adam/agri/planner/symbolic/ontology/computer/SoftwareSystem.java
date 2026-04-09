package com.adam.agri.planner.symbolic.ontology.computer;

import com.adam.agri.planner.symbolic.ontology.upper.*;
import com.adam.agri.planner.symbolic.ontology.computer.code.AbstractType;
import com.adam.agri.planner.symbolic.ontology.computer.code.Package;
import com.adam.agri.planner.symbolic.ontology.computer.code.Module;
import com.adam.agri.planner.symbolic.ontology.computer.code.language.java.*;
import com.adam.agri.planner.symbolic.ontology.planning.ConcreteRepresentation;

import java.util.*;

/**
 * SoftwareSystem - represents a software system as an abstract, versioned entity.
 *
 * Ontologically, a SoftwareSystem is an Abstract entity (Layer 3-4 in DOLCE)
 * that contains structure (packages, modules, types) but is independent
 * of the Physical ComputerSystems on which it may be deployed.
 *
 * Key insight: SoftwareSystem ≠ ComputerSystem
 * - SoftwareSystem is a structure of types, packages, and dependencies
 * - ComputerSystem is hardware that can execute software
 * - A SoftwareSystem can be deployed on multiple ComputerSystems
 * - Multiple SoftwareSystems can coexist on one ComputerSystem
 *
 * Example: A Calculator app is a SoftwareSystem with:
 * - Structure: packages (com.example.calculator)
 * - Components: classes (Calculator, MathEngine)
 * - Version: 1.2.3
 * - Dependencies: requires(java.base)
 */
public class SoftwareSystem extends Abstract {

    private final String name;
    private final String version;
    private final Package rootPackage;
    private final Set<Package> packages;
    private final Set<Module> modules;
    private final Set<String> dependencies;
    private final Map<String, String> properties;
    private final String language;

    public SoftwareSystem(EntityId id, Set<Property> properties,
                         String name, String version) {
        this(id, properties, name, version, "java");
    }

    public SoftwareSystem(EntityId id, Set<Property> properties,
                         String name, String version, String language) {
        super(id, properties);
        this.name = name;
        this.version = version;
        this.language = language;
        this.packages = new HashSet<>();
        this.modules = new HashSet<>();
        this.dependencies = new HashSet<>();
        this.properties = new HashMap<>();
        this.rootPackage = LanguageRegistry.getInstance()
            .createPackage(language, "")
            .orElseGet(() -> new JavaPackageImpl("", null, '.', language));
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getLanguage() {
        return language;
    }

    /**
     * Add a package to this software system.
     */
    public void addPackage(Package pkg) {
        packages.add(pkg);
    }

    public Set<Package> getPackages() {
        return Collections.unmodifiableSet(packages);
    }

    /** @deprecated Use addPackage(Package) instead */
    @Deprecated
    public void addPackage(JavaPackage pkg) {
        addPackage(new JavaPackageImpl(pkg.getQualifiedName(), null, '.', "java"));
    }

    /** @deprecated Use getPackages() instead */
    @Deprecated
    public Set<JavaPackage> getJavaPackages() {
        Set<JavaPackage> result = new HashSet<>();
        for (Package pkg : packages) {
            if (pkg instanceof JavaPackageImpl) {
                // Would need conversion - legacy support
            }
        }
        return result;
    }

    /**
     * Check if this system contains a package.
     */
    public boolean hasPackage(String packageName) {
        return packages.stream()
            .anyMatch(p -> p.getQualifiedName().equals(packageName));
    }

    /**
     * Add a module to this software system.
     */
    public void addModule(Module module) {
        modules.add(module);
    }

    public Set<Module> getModules() {
        return Collections.unmodifiableSet(modules);
    }

    /** @deprecated Use addModule(Module) instead */
    @Deprecated
    public void addModule(JavaModule module) {
        // Wrap in generic Module
    }

    /** @deprecated Use getModules() instead */
    @Deprecated
    public Set<JavaModule> getJavaModules() {
        return new HashSet<>();
    }

    /**
     * Add a dependency (e.g., "java.base", "com.google.guava").
     */
    public void addDependency(String dependency) {
        dependencies.add(dependency);
    }

    public Set<String> getDependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    /**
     * Check if this system has a type with the given name.
     */
    public boolean hasTypeByName(String typeName) {
        return packages.stream()
            .flatMap(p -> p.getTypes().stream())
            .anyMatch(t -> t.getSimpleName().equals(typeName));
    }

    /**
     * Find a type by simple name across all packages.
     */
    public Optional<AbstractType> findType(String typeName) {
        return packages.stream()
            .flatMap(p -> p.getTypes().stream())
            .filter(t -> t.getSimpleName().equals(typeName))
            .findFirst();
    }

    /** @deprecated Use findType(String) instead */
    @Deprecated
    public Optional<JavaType> findClass(String className) {
        return packages.stream()
            .flatMap(p -> p.getTypes().stream())
            .filter(t -> t instanceof JavaType)
            .map(t -> (JavaType) t)
            .filter(t -> t.getSimpleName().equals(className))
            .findFirst();
    }

    /**
     * Set a property (e.g., "language", "buildTool", "framework").
     */
    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    public Optional<String> getProperty(String key) {
        return Optional.ofNullable(properties.get(key));
    }

    /**
     * Get the artifact identifier (Maven/Gradle style).
     * Format: groupId:artifactId:version
     */
    public String getArtifactId() {
        return "com.agri.generated:" + name + ":" + version;
    }

    /**
     * Check if this software system can be deployed on a given computer system.
     */
    public boolean canBeDeployedOn(ComputerSystem system) {
        return system.isAvailable() && system.getComputeCapacity() > 0;
    }

    /**
     * Create a ConcreteRepresentation (Layer 2) of this software system
     * as a JAR/WAR file at a physical location.
     */
    public ConcreteRepresentation asConcreteRepresentation(EntityId repId,
         Set<Property> repProps,
         String artifactFormat,
         Location deploymentLocation) {
        double now = System.currentTimeMillis();
        TimeInterval interval = createTimeInterval(now, now + 3600000);
        return new ConcreteRepresentation(
            repId, repProps, this, artifactFormat, deploymentLocation, interval
        );
    }

    @Override
    public String toString() {
        return "SoftwareSystem[" + name + ":" + version +
            ", language=" + language +
            ", packages=" + packages.size() +
            ", types=" + packages.stream().mapToInt(p -> p.getTypes().size()).sum() + "]";
    }

    private TimeInterval createTimeInterval(double start, double end) {
        return new TimeInterval() {
            @Override public double getStart() { return start; }
            @Override public double getEnd() { return end; }
            @Override public double getDuration() { return end - start; }
            @Override public boolean overlaps(TimeInterval other) {
                return start < other.getEnd() && end > other.getStart();
            }
            @Override public boolean contains(double timestamp) {
                return timestamp >= start && timestamp <= end;
            }
        };
    }
}