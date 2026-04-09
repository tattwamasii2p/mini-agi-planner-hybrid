package com.adam.agri.planner.symbolic.ontology.computer;

import com.adam.agri.planner.symbolic.ontology.computer.code.AbstractType;
import com.adam.agri.planner.symbolic.ontology.computer.code.Module;
import com.adam.agri.planner.symbolic.ontology.computer.code.Package;

import java.util.List;
import java.util.Optional;

/**
 * Service Provider Interface (SPI) for language-specific implementations.
 * Enables multilanguage support by providing language-specific type systems, parsing, and code structure representations.
 */
public interface LanguageProvider {
    String getLanguageName();
    List<String> getFileExtensions();
    boolean handlesExtension(String extension);
    boolean handlesLanguage(String language);
    Optional<AbstractType> createType(String name, String kind);
    Optional<Package> createPackage(String name);
    Optional<Module> createModule(String name);
    List<String> getSupportedTypeKinds();
    LanguageConfiguration getConfiguration();
    String getVersion();
    List<String> getDependencies();

    /**
     * Configuration for a language provider.
     */
    class LanguageConfiguration {
        private final String languageName;
        private final String version;
        private final String basePackage;
        private final boolean supportsGenerics;
        private final boolean supportsInterfaces;
        private final boolean supportsModules;
        private final boolean supportsAsync;

        public LanguageConfiguration(String languageName, String version, String basePackage,
                                     boolean supportsGenerics, boolean supportsInterfaces,
                                     boolean supportsModules, boolean supportsAsync) {
            this.languageName = languageName;
            this.version = version;
            this.basePackage = basePackage;
            this.supportsGenerics = supportsGenerics;
            this.supportsInterfaces = supportsInterfaces;
            this.supportsModules = supportsModules;
            this.supportsAsync = supportsAsync;
        }

        public String getLanguageName() { return languageName; }
        public String getVersion() { return version; }
        public String getBasePackage() { return basePackage; }
        public boolean supportsGenerics() { return supportsGenerics; }
        public boolean supportsInterfaces() { return supportsInterfaces; }
        public boolean supportsModules() { return supportsModules; }
        public boolean supportsAsync() { return supportsAsync; }
    }

    /**
     * Source type representation for mapping.
     */
    interface SourceType {
        String getName();
        String getQualifiedName();
        String getKind();
        String getLanguage();
    }
}