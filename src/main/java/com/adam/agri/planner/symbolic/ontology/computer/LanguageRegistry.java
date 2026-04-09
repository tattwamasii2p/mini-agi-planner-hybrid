package com.adam.agri.planner.symbolic.ontology.computer;

import com.adam.agri.planner.symbolic.ontology.computer.code.AbstractType;
import com.adam.agri.planner.symbolic.ontology.computer.code.Module;
import com.adam.agri.planner.symbolic.ontology.computer.code.Package;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for managing language providers and their capabilities.
 * Central registry for discovering and managing language-specific implementations
 * through the ServiceLoader mechanism.
 */
public class LanguageRegistry {
    private static LanguageRegistry instance;
    private final Map<String, LanguageProvider> providers = new ConcurrentHashMap<>();
    private final Map<String, LanguageProvider> extensionMap = new ConcurrentHashMap<>();
    private final ServiceLoader<LanguageProvider> serviceLoader = ServiceLoader.load(LanguageProvider.class);

    private LanguageRegistry() {
        reload();
    }

    public static synchronized LanguageRegistry getInstance() {
        if (instance == null) {
            instance = new LanguageRegistry();
        }
        return instance;
    }

    /**
     * Reload all available language providers.
     */
    public synchronized void reload() {
        providers.clear();
        extensionMap.clear();

        // Load providers via ServiceLoader
        for (LanguageProvider provider : serviceLoader) {
            registerProvider(provider);
        }

        // Register default Java provider if not found
        if (!providers.containsKey("java")) {
            registerJavaDefaultProvider();
        }
    }

    private void registerProvider(LanguageProvider provider) {
        providers.put(provider.getLanguageName().toLowerCase(), provider);

        // Map file extensions to providers
        for (String extension : provider.getFileExtensions()) {
            extensionMap.put(extension.toLowerCase(), provider);
        }
    }

    private void registerJavaDefaultProvider() {
        // This will be implemented by the JavaLanguageProvider
        // For now we'll use a stub
    }

    /**
     * Register a custom language provider.
     */
    public void registerProvider(LanguageProvider provider, boolean override) {
        String language = provider.getLanguageName().toLowerCase();

        if (override || !providers.containsKey(language)) {
            registerProvider(provider);
        }
    }

    /**
     * Get a provider for a specific language.
     */
    public Optional<LanguageProvider> getProvider(String language) {
        return Optional.ofNullable(providers.get(language.toLowerCase()));
    }

    /**
     * Get a provider for a specific file extension.
     */
    public Optional<LanguageProvider> getProviderByExtension(String extension) {
        return Optional.ofNullable(extensionMap.get(extension.toLowerCase()));
    }

    /**
     * Get all registered languages.
     */
    public Set<String> getLanguages() {
        return Collections.unmodifiableSet(providers.keySet());
    }

    /**
     * Get all file extensions handled.
     */
    public Set<String> getFileExtensions() {
        return Collections.unmodifiableSet(extensionMap.keySet());
    }

    /**
     * Check if a language is supported.
     */
    public boolean isLanguageSupported(String language) {
        return providers.containsKey(language.toLowerCase());
    }

    /**
     * Check if a file extension is supported.
     */
    public boolean isExtensionSupported(String extension) {
        return extensionMap.containsKey(extension.toLowerCase());
    }

    /**
     * Create a type for the specified language.
     */
    public Optional<AbstractType> createType(String language, String name, String kind) {
        return getProvider(language)
                .flatMap(provider -> provider.createType(name, kind));
    }

    /**
     * Create a package for the specified language.
     */
    public Optional<Package> createPackage(String language, String name) {
        return getProvider(language)
                .flatMap(provider -> provider.createPackage(name));
    }

    /**
     * Create a module for the specified language.
     */
    public Optional<Module> createModule(String language, String name) {
        return getProvider(language)
                .flatMap(provider -> provider.createModule(name));
    }

    /**
     * Get all providers that support a specific capability.
     */
    public List<LanguageProvider> getProvidersByCapability(String capability) {
        return providers.values().stream()
                .filter(provider -> supportsCapability(provider, capability))
                .collect(Collectors.toList());
    }

    private boolean supportsCapability(LanguageProvider provider, String capability) {
        switch (capability.toLowerCase()) {
            case "generics":
                return provider.getConfiguration().supportsGenerics();
            case "interfaces":
                return provider.getConfiguration().supportsInterfaces();
            case "modules":
                return provider.getConfiguration().supportsModules();
            case "async":
                return provider.getConfiguration().supportsAsync();
            default:
                return true;
        }
    }

    /**
     * Get metadata about available languages.
     */
    public Map<String, LanguageInfo> getLanguageInfo() {
        Map<String, LanguageInfo> info = new LinkedHashMap<>();

        for (LanguageProvider provider : providers.values()) {
            LanguageProvider.LanguageConfiguration config = provider.getConfiguration();

            LanguageInfo languageInfo = new LanguageInfo(
                config.getLanguageName(),
                config.getVersion(),
                provider.getFileExtensions(),
                provider.getSupportedTypeKinds(),
                config.supportsGenerics(),
                config.supportsInterfaces(),
                config.supportsModules(),
                config.supportsAsync()
            );

            info.put(config.getLanguageName(), languageInfo);
        }

        return info;
    }

    /**
     * Get provider statistics.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalLanguages", providers.size());
        stats.put("totalExtensions", extensionMap.size());
        stats.put("languages", new ArrayList<>(providers.keySet()));
        stats.put("extensions", new ArrayList<>(extensionMap.keySet()));
        return stats;
    }

    /**
     * Information about a registered language.
     */
    public static class LanguageInfo {
        private final String name;
        private final String version;
        private final List<String> fileExtensions;
        private final List<String> typeKinds;
        private final boolean supportsGenerics;
        private final boolean supportsInterfaces;
        private final boolean supportsModules;
        private final boolean supportsAsync;

        public LanguageInfo(String name, String version, List<String> fileExtensions,
                          List<String> typeKinds, boolean supportsGenerics, boolean supportsInterfaces,
                          boolean supportsModules, boolean supportsAsync) {
            this.name = name;
            this.version = version;
            this.fileExtensions = fileExtensions;
            this.typeKinds = typeKinds;
            this.supportsGenerics = supportsGenerics;
            this.supportsInterfaces = supportsInterfaces;
            this.supportsModules = supportsModules;
            this.supportsAsync = supportsAsync;
        }

        public String getName() { return name; }
        public String getVersion() { return version; }
        public List<String> getFileExtensions() { return fileExtensions; }
        public List<String> getTypeKinds() { return typeKinds; }
        public boolean supportsGenerics() { return supportsGenerics; }
        public boolean supportsInterfaces() { return supportsInterfaces; }
        public boolean supportsModules() { return supportsModules; }
        public boolean supportsAsync() { return supportsAsync; }
    }
}