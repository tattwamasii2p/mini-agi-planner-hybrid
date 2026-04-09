# Multilanguage Support Documentation

## Overview

The AGI Planner now supports multiple programming languages through a plugin-based architecture. The system has been refactored from Java-specific implementations to language-agnostic abstractions that can support any programming language.

## Architecture

### Core Abstractions

The multilanguage support is built on these core interfaces:

#### 1. AbstractType (`symbolic/ontology/computer/code/AbstractType`)
Language-agnostic representation of types:
- Primitive types (int, float, etc.)
- Object/Reference types (classes, structs)
- Array and collection types
- Generic/parameterized types
- Function types

```java
// Create a type via LanguageProvider
Optional<AbstractType> type = LanguageRegistry.getInstance()
    .createType("java", "MyClass", "class");
```

#### 2. Package (`symbolic/ontology/computer/code/Package`)
Language-agnostic namespace container:
```java
// Create a package for any language
Optional<Package> pkg = LanguageRegistry.getInstance()
    .createPackage("java", "com.example.myapp");
```

#### 3. Module (`symbolic/ontology/computer/code/Module`)
Compilation unit abstraction supporting:
- Java 9+ modules
- Python packages
- C# assemblies
- etc.

#### 4. TypeKind (`symbolic/ontology/computer/code/TypeKind`)
Generic type classifications:
- `PRIMITIVE` - Built-in basic types
- `OBJECT` - Classes/objects
- `ARRAY` - Array types
- `INTERFACE` - Protocol/contract definitions
- `ENUM` - Enumerated types
- `GENERIC_TYPE` - Parameterized types
- `FUNCTIONAL` - Function/lambda types
- `VOID` - Void/unit types

### Plugin System

#### LanguageProvider Interface

Implement `LanguageProvider` to add support for a new language:

```java
public class PythonLanguageProvider implements LanguageProvider {
    @Override
    public String getLanguageName() {
        return "python";
    }

    @Override
    public List<String> getFileExtensions() {
        return Arrays.asList("py", "pyc");
    }

    @Override
    public Optional<AbstractType> createType(String name, String kind) {
        // Create Python-specific type implementation
        return Optional.of(new PythonType(name, kind));
    }

    @Override
    public Optional<Package> createPackage(String name) {
        return Optional.of(new PythonPackage(name));
    }

    @Override
    public LanguageConfiguration getConfiguration() {
        return new LanguageConfiguration(
            "python", "3.11", "",
            false,  // no generics
            false,  // no interfaces (uses duck typing)
            false,  // no modules (uses packages)
            true    // supports async/await
        );
    }
}
```

#### ServiceLoader Registration

Register your provider in `META-INF/services/com.adam.agri.planner.symbolic.ontology.computer.LanguageProvider`:

```
com.adam.agri.planner.symbolic.ontology.computer.code.PythonLanguageProvider
```

## Using Multilanguage Support

### Creating Software Systems

Create a language-agnostic software system:

```java
// Java system (default)
SoftwareSystemState javaSystem = new SoftwareSystemState(
    StateId.generate(),
    "MyApp",
    "1.0.0",
    outputPath
);

// Python system
SoftwareSystemState pythonSystem = new SoftwareSystemState(
    StateId.generate(),
    "MyApp",
    "1.0.0",
    outputPath,
    "python"
);
```

### Working with Types

```java
// Get the language registry
LanguageRegistry registry = LanguageRegistry.getInstance();

// Check supported languages
Set<String> languages = registry.getLanguages();
// Returns: ["java", "python", ...]

// Create a type for a specific language
Optional<AbstractType> type = registry.createType("python", "MyClass", "class");

// Check type properties
if (type.isPresent()) {
    AbstractType t = type.get();
    System.out.println(t.getQualifiedName());
    System.out.println(t.getTypeKind());
    System.out.println(t.getLanguage()); // "python"
}
```

### Working with Packages

```java
// Create a package
Optional<Package> pkg = registry.createPackage("java", "com.example.utils");

if (pkg.isPresent()) {
    Package p = pkg.get();

    // Check if root package
    boolean isRoot = p.isRoot();

    // Get file path representation
    String path = p.getFilePath(); // "com/example/utils/"

    // Get parent package
    Optional<Package> parent = p.getParent();

    // Create subpackage
    Package sub = p.createSubpackage("helpers");
}
```

### Language Registry Queries

```java
LanguageRegistry registry = LanguageRegistry.getInstance();

// Get provider for a language
Optional<LanguageProvider> provider = registry.getProvider("java");

// Get provider by file extension
Optional<LanguageProvider> javaProvider = registry.getProviderByExtension("java");

// Get language capabilities
Map<String, LanguageInfo> info = registry.getLanguageInfo();
for (LanguageInfo lang : info.values()) {
    System.out.println(lang.getName() + ":");
    System.out.println("  Supports generics: " + lang.supportsGenerics());
    System.out.println("  Supports interfaces: " + lang.supportsInterfaces());
    System.out.println("  File extensions: " + lang.getFileExtensions());
}
```

## Migration Guide

### From Old Java-Specific APIs

The old Java-specific APIs are now deprecated but still functional for backward compatibility:

**Old Way (Deprecated):**
```java
// Old - Java-specific
JavaPackage pkg = new JavaPackage(...);
JavaType type = new JavaReferenceType(...);
sss.addPackage(pkg);
sss.withType(packageName, type);
```

**New Way (Recommended):**
```java
// New - Language-agnostic
Package pkg = LanguageRegistry.getInstance()
    .createPackage("java", "com.example")
    .orElseThrow();

// Create system with specific language
SoftwareSystemState sss = new SoftwareSystemState(
    id, name, version, outputPath, "java"
);

// Use generic methods
Optional<Package> pkg = sss.getPackageGeneric("com.example");
Set<AbstractType> types = sss.getTypesInPackage("com.example");
```

### Deprecated Methods in SoftwareSystemState

| Old Method | New Method | Notes |
|------------|------------|-------|
| `getPackage(String)` | `getPackageGeneric(String)` | Returns `Optional<Package>` |
| `getPackages()` | `getPackagesGeneric()` | Returns `Set<Package>` |
| `hasClass(String)` | `hasType(String)` | Uses type terminology |
| `getClass(String)` | `getTypeGeneric(String)` | Returns generic type |
| `getClassesInPackage(String)` | `getTypesInPackage(String)` | Returns generic types |
| `getAllClasses()` | `getAllTypes()` | Uses type terminology |

## Adding New Language Support

### Step 1: Create Type Implementation

```java
public class PythonType implements AbstractType {
    private final String name;
    private final TypeKind kind;
    private final PythonPackage pkg;

    public PythonType(String name, TypeKind kind, PythonPackage pkg) {
        this.name = name;
        this.kind = kind;
        this.pkg = pkg;
    }

    @Override
    public String getQualifiedName() {
        return pkg.getQualifiedName() + "." + name;
    }

    @Override
    public TypeKind getTypeKind() {
        return kind;
    }

    @Override
    public String getLanguage() {
        return "python";
    }

    // ... implement other methods
}
```

### Step 2: Create Package Implementation

```java
public class PythonPackage implements Package {
    private final String name;
    private final char separator = '.'; // Python uses dots

    @Override
    public char getSeparator() {
        return separator;
    }

    @Override
    public String getFilePath() {
        // Python: com.example.utils -> com/example/utils/
        return name.replace(separator, '/') + '/';
    }

    @Override
    public String getFileExtension() {
        return ".py";
    }

    // ... implement other methods
}
```

### Step 3: Create LanguageProvider

See [Plugin System](#plugin-system) section above.

### Step 4: Register with ServiceLoader

Create file:
`src/main/resources/META-INF/services/com.adam.agri.planner.symbolic.ontology.computer.LanguageProvider`

Containing:
```
com.example.PythonLanguageProvider
```

## Implementation Status

### Currently Supported Languages

| Language | Status | File Extensions | Notes |
|----------|--------|-----------------|-------|
| Java | Full | .java, .class, .jar | Reference implementation |

### Planned Language Support

| Language | Priority | Notes |
|----------|----------|-------|
| Python | High | Dynamic typing support |
| TypeScript | Medium | JavaScript superset |
| Rust | Medium | Memory safety focus |
| Go | Medium | Simplicity focused |
| Kotlin | Low | JVM compatible |
| C++ | Low | Template support needed |

## Configuration

### Language Capabilities

Each language provider declares its capabilities:

```java
public class LanguageConfiguration {
    private final String languageName;      // "java"
    private final String version;           // "17"
    private final String basePackage;       // "java.lang"
    private final boolean supportsGenerics; // true
    private final boolean supportsInterfaces; // true
    private final boolean supportsModules;  // true
    private final boolean supportsAsync;    // true
}
```

### Checking Capabilities

```java
LanguageProvider provider = LanguageRegistry.getInstance()
    .getProvider("java")
    .orElseThrow();

LanguageConfiguration config = provider.getConfiguration();

if (config.supportsGenerics()) {
    // Can use generic type operations
}

if (config.supportsAsync()) {
    // Can handle async/await constructs
}
```

## Type System Mapping

### Common Type Mappings

| Concept | Java | Python | C# | Rust |
|---------|------|--------|-----|------|
| Integer | `int`/`Integer` | `int` | `int` | `i32` |
| Float | `float`/`Float` | `float` | `float` | `f32` |
| String | `String` | `str` | `string` | `String` |
| Boolean | `boolean` | `bool` | `bool` | `bool` |
| Array | `T[]` | `list[T]` | `T[]` | `Vec<T>` |
| Optional | `Optional<T>` | `Optional[T]` | `T?` | `Option<T>` |
| Class | `class` | `class` | `class` | `struct` |

## Best Practices

1. **Always use generic interfaces** for new code
2. **Check language capabilities** before using language-specific features
3. **Use Optional** for all language registry operations
4. **Handle nulls gracefully** when converting between type systems
5. **Test with multiple languages** to ensure compatibility

## Troubleshooting

### Common Issues

**Issue:** `NoSuchElementException` when creating types
```java
// Don't do this
AbstractType type = registry.createType("java", "MyClass", "class").get();

// Do this instead
Optional<AbstractType> typeOpt = registry.createType("java", "MyClass", "class");
if (typeOpt.isPresent()) {
    AbstractType type = typeOpt.get();
}
```

**Issue:** Language not registered
```java
// Check if language is supported first
if (!LanguageRegistry.getInstance().isLanguageSupported("python")) {
    throw new IllegalArgumentException("Python support not available");
}
```

## Future Enhancements

- Cross-language type interoperability
- Language-specific AST parsing
- Type inference across languages
- Polyglot debugging support
- Language-aware code generation

## See Also

- [CLAUDE.md](../CLAUDE.md) - Project architecture overview
- [COMPUTER_SYSTEM_AGENT.md](COMPUTER_SYSTEM_AGENT.md) - Computer system as agent
- [PERCEPTION_ACTUATION.md](PERCEPTION_ACTUATION.md) - Sensorimotor loop
