package com.adam.agri.planner.symbolic.ontology.computer.code.language.java;

import java.util.Optional;
import java.util.Set;

import com.adam.agri.planner.symbolic.ontology.upper.Abstract;
import com.adam.agri.planner.symbolic.ontology.upper.EntityId;
import com.adam.agri.planner.symbolic.ontology.upper.Property;

/**
 * Abstract Java Entity - base for all Java language constructs.
 *
 * Represents code constructs as Abstract entities (conceptual representations).
 * Layer 3-6: Code as abstract structure subject to planning and transformation.
 */
public abstract class AbstractJavaEntity extends Abstract {

    protected final String qualifiedName;
    protected final JavaVisibility visibility;
    protected final Set<JavaAnnotation> annotations;
    protected final Set<JavaModifier> modifiers;
    protected final Optional<JavaDoc> documentation;

    public enum JavaModifier {
        STATIC, FINAL, ABSTRACT, SYNCHRONIZED, VOLATILE, TRANSIENT, NATIVE, STRICTFP
    }

    public AbstractJavaEntity(EntityId id, Set<Property> properties,
                               String qualifiedName, JavaVisibility visibility,
                               Set<JavaAnnotation> annotations, Set<JavaModifier> modifiers,
                               Optional<JavaDoc> documentation) {
        super(id, properties);
        this.qualifiedName = qualifiedName;
        this.visibility = visibility;
        this.annotations = Set.copyOf(annotations);
        this.modifiers = Set.copyOf(modifiers);
        this.documentation = documentation;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public String getSimpleName() {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }

    public JavaVisibility getVisibility() {
        return visibility;
    }

    public Set<JavaAnnotation> getAnnotations() {
        return annotations;
    }

    public Set<JavaModifier> getModifiers() {
        return modifiers;
    }

    public Optional<JavaDoc> getDocumentation() {
        return documentation;
    }

    public boolean hasModifier(JavaModifier modifier) {
        return modifiers.contains(modifier);
    }

    public boolean hasAnnotation(String annotationName) {
        return annotations.stream()
            .anyMatch(a -> a.name().equals(annotationName));
    }

    /**
     * Check if this entity is accessible from given context.
     */
    public boolean isAccessibleFrom(JavaPackage context) {
        return switch (visibility) {
            case PUBLIC -> true;
            case PROTECTED -> isInSamePackage(context);
            case PACKAGE_PRIVATE -> isInSamePackage(context);
            case PRIVATE -> false; // requires subclass check
        };
    }

    protected abstract boolean isInSamePackage(JavaPackage other);

    @Override
    public String toString() {
        return "Java[" + qualifiedName + "]";
    }
}

/**
 * Java annotation.
 */
record JavaAnnotation(String name, java.util.Map<String, String> values) {
}

/**
 * JavaDoc documentation.
 */
record JavaDoc(String text, java.util.Map<String, String> tags) {
}
