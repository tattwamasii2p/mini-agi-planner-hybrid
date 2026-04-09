package com.adam.agri.planner.symbolic.ontology.computer.code;

import java.util.List;
import java.util.Optional;

/**
 * Language-agnostic module system interface.
 * Represents compilation units or module definitions across different languages.
 */
public interface Module {

    /**
     * Get the name of this module.
     */
    String getName();

    /**
     * Get the qualified name of this module.
     */
    String getQualifiedName();

    /**
     * Get the version of this module.
     */
    Optional<String> getVersion();

    /**
     * Get packages contained in this module.
     */
    List<Package> getPackages();

    /**
     * Add a package to this module.
     */
    void addPackage(Package pkg);

    /**
     * Get dependencies required by this module.
     */
    List<String> getRequiredModules();

    /**
     * Check if this module exports the given package.
     */
    boolean exportsPackage(Package pkg);

    /**
     * Get exported packages.
     */
    List<Package> getExportedPackages();

    /**
     * Add a parameter to this module.
     */
    void addParameter(ModuleParameter parameter);

    /**
     * Get module parameters.
     */
    List<ModuleParameter> getParameters();

    /**
     * Check if this module is executable.
     */
    boolean isExecutable();

    /**
     * Get the main entry point if this module is executable.
     */
    Optional<String> getMainEntryPoint();

    /**
     * Get the language this module belongs to.
     */
    String getLanguage();

    /**
     * Module parameter definition.
     */
    class ModuleParameter {
        private final String name;
        private final String value;
        private final boolean optional;

        public ModuleParameter(String name, String value, boolean optional) {
            this.name = name;
            this.value = value;
            this.optional = optional;
        }

        public String getName() { return name; }
        public String getValue() { return value; }
        public boolean isOptional() { return optional; }
    }
}