package com.adam.agri.planner.symbolic.ontology.computer.code.language.java;

import java.util.HashSet;
import java.util.Set;

import com.adam.agri.planner.symbolic.ontology.upper.Abstract;
import com.adam.agri.planner.symbolic.ontology.upper.EntityId;
import com.adam.agri.planner.symbolic.ontology.upper.Property;

/**
 * Java Module (Java 9+) - higher level container.
 */
public class JavaModule extends Abstract {

    private final String name;
    private final Set<JavaPackage> exportedPackages;
    private final Set<String> requires; // module dependencies
    private final Set<String> provides; // service providers
    private final Set<String> uses;     // service consumers

    public JavaModule(EntityId id, Set<Property> properties, String name) {
        super(id, properties);
        this.name = name;
        this.exportedPackages = new HashSet<>();
        this.requires = new HashSet<>();
        this.provides = new HashSet<>();
        this.uses = new HashSet<>();
    }

    public String getName() {
        return name;
    }

    public Set<JavaPackage> getExportedPackages() {
        return Set.copyOf(exportedPackages);
    }

    public void exportPackage(JavaPackage pkg) {
        exportedPackages.add(pkg);
    }

    public void requireModule(String moduleName) {
        requires.add(moduleName);
    }

    public Set<String> getRequires() {
        return Set.copyOf(requires);
    }

    public Set<String> getProvides() {
        return Set.copyOf(provides);
    }

    public Set<String> getUses() {
        return Set.copyOf(uses);
    }

    /**
     * Check if package is exported (accessible from other modules).
     */
    public boolean isPackageExported(JavaPackage pkg) {
        return exportedPackages.contains(pkg);
    }

    @Override
    public String toString() {
        return "JavaModule[" + name + "]";
    }
}