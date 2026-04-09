package com.adam.agri.planner.symbolic.ontology.computer.code.language.java;

import com.adam.agri.planner.symbolic.ontology.computer.LanguageProvider;
import com.adam.agri.planner.symbolic.ontology.computer.code.AbstractType;
import com.adam.agri.planner.symbolic.ontology.computer.code.Package;
import com.adam.agri.planner.symbolic.ontology.computer.code.Module;
import com.adam.agri.planner.symbolic.ontology.computer.code.TypeKind;
import com.adam.agri.planner.symbolic.ontology.computer.code.Visibility;
import com.adam.agri.planner.symbolic.ontology.computer.code.Modifier;

import java.util.*;

/**
 * Java-specific implementation of LanguageProvider.
 */
public class JavaLanguageProvider implements LanguageProvider {
    private static final String LANGUAGE_NAME = "java";
    private static final List<String> FILE_EXTENSIONS = Arrays.asList("java", "class", "jar");

    @Override
    public String getLanguageName() {
        return LANGUAGE_NAME;
    }

    @Override
    public List<String> getFileExtensions() {
        return FILE_EXTENSIONS;
    }

    @Override
    public boolean handlesExtension(String extension) {
        return FILE_EXTENSIONS.contains(extension.toLowerCase());
    }

    @Override
    public boolean handlesLanguage(String language) {
        return LANGUAGE_NAME.equalsIgnoreCase(language);
    }

    @Override
    public Optional<AbstractType> createType(String name, String kind) {
        // Implementation
        return Optional.of(new JavaTypeImpl(name, name, TypeKind.OBJECT, null, null,
            new ArrayList<>(), new ArrayList<>(), false, null, null,
            Visibility.PUBLIC, EnumSet.noneOf(Modifier.class)));
    }

    @Override
    public Optional<Package> createPackage(String name) {
        return Optional.of(new JavaPackageImpl(name, null, '.', "java"));
    }

    @Override
    public LanguageConfiguration getConfiguration() {
        return new LanguageConfiguration(LANGUAGE_NAME, "17", "java.lang",
            true, true, true, true);
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public List<String> getDependencies() {
        return Arrays.asList("openjdk-17");
    }

    @Override
    public Optional<com.adam.agri.planner.symbolic.ontology.computer.code.Module> createModule(String name) {
        return Optional.empty();
    }

    @Override
    public List<String> getSupportedTypeKinds() {
        return Arrays.asList("class", "interface", "enum", "record", "primitive", "array");
    }
}