package com.adam.agri.planner.symbolic.ontology.computer.code.language.java;

import com.adam.agri.planner.symbolic.ontology.computer.code.AbstractType;
import com.adam.agri.planner.symbolic.ontology.computer.code.Package;
import com.adam.agri.planner.symbolic.ontology.computer.code.Module;

import java.util.*;
import java.util.Optional;

/**
 * Java-specific implementation of the generic Package interface.
 */
public class JavaPackageImpl implements Package {
    private final String qualifiedName;
    private final String simpleName;
    private final JavaPackageImpl parent;
    private final List<Package> subPackages;
    private final List<AbstractType> types;
    private final char separator;
    private final String fileExtension;
    private final String language;
    private Module module;

    public JavaPackageImpl(String qualifiedName, JavaPackageImpl parent, char separator, String language) {
        this.qualifiedName = qualifiedName;
        this.simpleName = qualifiedName.contains(".") ?
            qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1) : qualifiedName;
        this.parent = parent;
        this.separator = separator;
        this.fileExtension = ".java";
        this.language = language;
        this.subPackages = new ArrayList<>();
        this.types = new ArrayList<>();
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
    public Optional<Package> getParent() {
        return Optional.ofNullable(parent);
    }

    @Override
    public List<Package> getSubpackages() {
        return new ArrayList<>(subPackages);
    }

    @Override
    public List<AbstractType> getTypes() {
        return new ArrayList<>(types);
    }

    @Override
    public char getSeparator() {
        return separator;
    }

    @Override
    public String getFileExtension() {
        return fileExtension;
    }

    @Override
    public String getFilePath() {
        return qualifiedName.replace(separator, '/') + '/';
    }

    @Override
    public boolean isRoot() {
        return parent == null;
    }

    @Override
    public Package createSubpackage(String name) {
        String childName = qualifiedName.isEmpty() ? name : qualifiedName + separator + name;
        JavaPackageImpl child = new JavaPackageImpl(childName, this, separator, language);
        subPackages.add(child);
        return child;
    }

    @Override
    public boolean contains(Package subpackage) {
        return subPackages.contains(subpackage);
    }

    @Override
    public Optional<Module> getModule() {
        return Optional.ofNullable(module);
    }

    public void setModule(Module module) {
        this.module = module;
    }

    @Override
    public String getLanguage() {
        return language;
    }

    @Override
    public Optional<String> getVersion() {
        return Optional.empty();
    }

    @Override
    public List<String> getImports() {
        return new ArrayList<>();
    }

    @Override
    public AbstractType createType(String typeName, com.adam.agri.planner.symbolic.ontology.computer.code.TypeKind kind) {
        // This would create a Java-specific type
        return null;
    }

    @Override
    public String getDocumentation() {
        return "";
    }

    @Override
    public boolean packageEquals(Package other) {
        if (this == other) return true;
        if (!(other instanceof JavaPackageImpl)) return false;
        JavaPackageImpl that = (JavaPackageImpl) other;
        return qualifiedName.equals(that.qualifiedName) && language.equals(that.language);
    }

    @Override
    public String toString() {
        return "JavaPackage[" + qualifiedName + "]";
    }
}