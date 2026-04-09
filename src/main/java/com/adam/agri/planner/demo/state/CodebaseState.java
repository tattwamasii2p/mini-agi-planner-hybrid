package com.adam.agri.planner.demo.state;

import com.adam.agri.planner.core.state.*;

import java.nio.file.Path;
import java.util.*;

/**
 * State representing a codebase structure for code generation planning.
 * Tracks packages, classes, files, and their relationships.
 * Extends SymbolicState to be compatible with Action.apply() methods.
 */
public class CodebaseState extends SymbolicState {
    private final Path rootPath;
    private final Set<String> existingPackages;
    private final Set<String> existingClasses;
    private final Set<String> existingFiles;
    private final Map<String, String> fileContents;
    private final Map<String, Set<String>> packageToClasses;
    private final Map<String, String> classToPackage;

    public CodebaseState(StateId id, Path rootPath) {
        super(id);
        this.rootPath = rootPath;
        this.existingPackages = new HashSet<>();
        this.existingClasses = new HashSet<>();
        this.existingFiles = new HashSet<>();
        this.fileContents = new HashMap<>();
        this.packageToClasses = new HashMap<>();
        this.classToPackage = new HashMap<>();
    }

    private CodebaseState(StateId id, Path rootPath,
                          Set<Predicate> predicates,
                          Map<String, Object> bindings,
                          Set<String> existingPackages,
                          Set<String> existingClasses,
                          Set<String> existingFiles,
                          Map<String, String> fileContents,
                          Map<String, Set<String>> packageToClasses,
                          Map<String, String> classToPackage) {
        super(id, predicates, bindings);
        this.rootPath = rootPath;
        this.existingPackages = new HashSet<>(existingPackages);
        this.existingClasses = new HashSet<>(existingClasses);
        this.existingFiles = new HashSet<>(existingFiles);
        this.fileContents = new HashMap<>(fileContents);
        this.packageToClasses = new HashMap<>(packageToClasses);
        this.classToPackage = new HashMap<>(classToPackage);
    }

    /**
     * Check if a package exists.
     */
    public boolean hasPackage(String packageName) {
        return existingPackages.contains(packageName);
    }

    /**
     * Check if a class exists.
     */
    public boolean hasClass(String className) {
        return existingClasses.contains(className);
    }

    /**
     * Check if a file exists.
     */
    public boolean hasFile(String filePath) {
        return existingFiles.contains(filePath);
    }

    /**
     * Add a package to the codebase.
     */
    public CodebaseState withPackage(String packageName) {
        CodebaseState copy = copyInternal();
        copy.existingPackages.add(packageName);
        copy.packageToClasses.putIfAbsent(packageName, new HashSet<>());
        return copy;
    }

    /**
     * Add a class to the codebase.
     */
    public CodebaseState withClass(String packageName, String className) {
        CodebaseState copy = copyInternal();
        copy.existingClasses.add(className);
        copy.existingPackages.add(packageName);
        copy.packageToClasses.computeIfAbsent(packageName, k -> new HashSet<>()).add(className);
        copy.classToPackage.put(className, packageName);
        return copy;
    }

    /**
     * Add a file to the codebase.
     */
    public CodebaseState withFile(String filePath, String content) {
        CodebaseState copy = copyInternal();
        copy.existingFiles.add(filePath);
        copy.fileContents.put(filePath, content);
        return copy;
    }

    /**
     * Get the content of a file.
     */
    public Optional<String> getFileContent(String filePath) {
        return Optional.ofNullable(fileContents.get(filePath));
    }

    /**
     * Get all classes in a package.
     */
    public Set<String> getClassesInPackage(String packageName) {
        return packageToClasses.getOrDefault(packageName, Collections.emptySet());
    }

    /**
     * Get the package of a class.
     */
    public Optional<String> getPackageOfClass(String className) {
        return Optional.ofNullable(classToPackage.get(className));
    }

    /**
     * Get all packages.
     */
    public Set<String> getPackages() {
        return Collections.unmodifiableSet(existingPackages);
    }

    /**
     * Get all classes.
     */
    public Set<String> getClasses() {
        return Collections.unmodifiableSet(existingClasses);
    }

    /**
     * Get all files.
     */
    public Set<String> getFiles() {
        return Collections.unmodifiableSet(existingFiles);
    }

    /**
     * Get the root path of the codebase.
     */
    public Path getRootPath() {
        return rootPath;
    }

    /**
     * Internal copy method that preserves CodebaseState type.
     */
    private CodebaseState copyInternal() {
        return new CodebaseState(
            StateId.generate(),
            rootPath,
            getPredicates(),
            Map.of(),
            existingPackages,
            existingClasses,
            existingFiles,
            fileContents,
            packageToClasses,
            classToPackage
        );
    }

    /**
     * Check if this codebase satisfies a goal.
     * Goal: all required classes exist in appropriate packages.
     */
    public boolean satisfiesGoal(Set<String> requiredClasses, Set<String> requiredPackages) {
        return existingClasses.containsAll(requiredClasses) &&
               existingPackages.containsAll(requiredPackages);
    }

    @Override
    public String toString() {
        return "CodebaseState{" +
               "root=" + rootPath +
               ", packages=" + existingPackages.size() +
               ", classes=" + existingClasses.size() +
               ", files=" + existingFiles.size() +
               '}';
    }
}
