package com.adam.agri.planner.symbolic.ontology.computer.code;

import com.adam.agri.planner.symbolic.ontology.computer.code.Module;

import java.util.List;
import java.util.Optional;

/**
 * Language-agnostic package/container interface.
 * This replaces Java-specific package concepts with generic container structures.
 */
public interface Package {

    /**
     * Get the fully qualified name of this package.
     */
    String getQualifiedName();

    /**
     * Get the simple name of this package.
     */
    String getSimpleName();

    /**
     * Get the parent package, if any.
     */
    Optional<Package> getParent();

    /**
     * Get the child packages/packages.
     */
    List<Package> getSubpackages();

    /**
     * Get the types defined in this package.
     */
    List<AbstractType> getTypes();

    /**
     * Get the file/path separator used by the language.
     * For Java-like languages this would be '.', for others it might be '::', '/', or '\'.
     */
    char getSeparator();

    /**
     * Get the filename extension for source files in this package's language.
     */
    String getFileExtension();

    /**
     * Get the file/directory path representation.
     * Converts the package name to the corresponding file system structure.
     */
    String getFilePath();

    /**
     * Check if this package is a root package (has no parent).
     */
    boolean isRoot();

    /**
     * Parse a qualified name relative to this package.
     * Returns the package structure for the given qualified name.
     */
    Package createSubpackage(String name);

    /**
     * Check if this package contains the given subpackage.
     */
    boolean contains(Package subpackage);

    /**
     * Get the module that contains this package, if any.
     */
    Optional<Module> getModule();

    /**
     * Get the language this package belongs to.
     */
    String getLanguage();

    /**
     * Get the version information for this package, if available.
     */
    Optional<String> getVersion();

    /**
     * Get any imports or dependencies for this package.
     */
    List<String> getImports();

    /**
     * Create a type within this package with the given name.
     */
    AbstractType createType(String typeName, TypeKind kind);

    /**
     * Get the documentation/description for this package.
     */
    String getDocumentation();

    /**
     * Check equality based on package structure rather than object identity.
     */
    boolean packageEquals(Package other);
}