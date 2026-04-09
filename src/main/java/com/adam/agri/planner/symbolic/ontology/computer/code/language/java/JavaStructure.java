package com.adam.agri.planner.symbolic.ontology.computer.code.language.java;

import com.adam.agri.planner.symbolic.ontology.upper.*;

import java.util.*;

/**
 * Represents the overall structure of software artifacts (modules, packages, subsystems).
 * This concept captures the high-level organization of software in a system,
 * independent of the physical hardware on which it runs.
 */
public class JavaStructure extends Abstract {

    private final String name;
    private final String version;
    private final List<JavaModule> modules;
    private final List<JavaPackage> rootPackages;

    public JavaStructure(EntityId id, Set<Property> properties, String name, String version) {
        super(id, properties);
        this.name = name;
        this.version = version;
        this.modules = new ArrayList<>();
        this.rootPackages = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public void addModule(JavaModule module) {
        modules.add(module);
    }

    public List<JavaModule> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public void addRootPackage(JavaPackage pkg) {
        rootPackages.add(pkg);
    }

    public List<JavaPackage> getRootPackages() {
        return Collections.unmodifiableList(rootPackages);
    }

    @Override
    public String toString() {
        return "JavaStructure[" + name + ":" + version + "]";
    }
}
