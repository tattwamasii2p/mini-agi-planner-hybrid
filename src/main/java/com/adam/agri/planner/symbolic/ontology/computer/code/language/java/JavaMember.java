package com.adam.agri.planner.symbolic.ontology.computer.code.language.java;

import com.adam.agri.planner.symbolic.ontology.upper.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Java Member - base for fields, methods, constructors.
 */
public abstract class JavaMember extends AbstractJavaEntity {

    protected JavaMember(EntityId id, Set<Property> properties,
                         String qualifiedName, JavaVisibility visibility,
                         Set<JavaAnnotation> annotations, Set<JavaModifier> modifiers,
                         Optional<JavaDoc> documentation) {
        super(id, properties, qualifiedName, visibility, annotations, modifiers, documentation);
    }

    /**
     * Get the declaring type of this member.
     */
    public abstract JavaType getDeclaringType();

    @Override
    public String toString() {
        return getSimpleName();
    }
}

/**
 * Java Field.
 */
record JavaField(String name, JavaType type, JavaType declaringType,
                 JavaVisibility visibility, Set<JavaAnnotation> annotations,
                 Set<AbstractJavaEntity.JavaModifier> modifiers) {

    public JavaField {
        annotations = Set.copyOf(annotations);
        modifiers = Set.copyOf(modifiers);
    }

    public String getName() {
        return name;
    }

    public JavaType getType() {
        return type;
    }

    public JavaType getDeclaringType() {
        return declaringType;
    }

    public boolean isStatic() {
        return modifiers.contains(AbstractJavaEntity.JavaModifier.STATIC);
    }
}

/**
 * Java Parameter.
 */
record JavaParameter(String name, JavaType type,
                     Set<JavaAnnotation> annotations) {

    public String getName() {
        return name;
    }

    public JavaType getType() {
        return type;
    }
}

/**
 * Java Method.
 */
record JavaMethod(String name, JavaType returnType, List<JavaParameter> parameters,
                  JavaType declaringType, JavaVisibility visibility,
                  Set<JavaAnnotation> annotations, Set<AbstractJavaEntity.JavaModifier> modifiers) {

    public JavaMethod {
        parameters = List.copyOf(parameters);
        annotations = Set.copyOf(annotations);
        modifiers = Set.copyOf(modifiers);
    }

    public String getName() {
        return name;
    }

    public JavaType getReturnType() {
        return returnType;
    }

    public List<JavaParameter> getParameters() {
        return parameters;
    }

    public boolean isVarArgs() {
        return modifiers.contains(AbstractJavaEntity.JavaModifier.NATIVE);
    }

    public boolean isStatic() {
        return modifiers.contains(AbstractJavaEntity.JavaModifier.STATIC);
    }

    public boolean isAbstract() {
        return modifiers.contains(AbstractJavaEntity.JavaModifier.ABSTRACT);
    }

    public boolean isSynchronized() {
        return modifiers.contains(AbstractJavaEntity.JavaModifier.SYNCHRONIZED);
    }

    /**
     * Check if method signature matches parameters.
     */
    public boolean matchesSignature(JavaType... paramTypes) {
        if (parameters.size() != paramTypes.length) return false;
        for (int i = 0; i < parameters.size(); i++) {
            if (!parameters.get(i).type().equals(paramTypes[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(returnType.getSimpleName()).append(" ").append(name).append("(");
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(parameters.get(i).type().getSimpleName()).append(" ").
                append(parameters.get(i).getName());
        }
        sb.append(")");
        return sb.toString();
    }
}

/**
 * Java Constructor.
 */
record JavaConstructor(String name, List<JavaParameter> parameters,
                       JavaType declaringType, JavaVisibility visibility,
                       Set<JavaAnnotation> annotations, Set<AbstractJavaEntity.JavaModifier> modifiers) {

    public JavaConstructor {
        parameters = List.copyOf(parameters);
        annotations = Set.copyOf(annotations);
        modifiers = Set.copyOf(modifiers);
    }

    public String getName() {
        return name;
    }

    public List<JavaParameter> getParameters() {
        return parameters;
    }
}
