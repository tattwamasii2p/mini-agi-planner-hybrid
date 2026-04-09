package com.adam.agri.planner.demo.actions;

import com.adam.agri.planner.core.action.Effect;
import com.adam.agri.planner.core.action.Precondition;
import com.adam.agri.planner.core.state.State;
import com.adam.agri.planner.demo.state.SoftwareSystemState;
import com.adam.agri.planner.symbolic.ontology.upper.EntityId;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Action to write code content and add types to the software system ontology.
 *
 * Creates type entities and adds them to the appropriate package,
 * maintaining rich semantic structure in the ontology.
 */
public class WriteCodeAction extends FileAction {
    private final String filePath;
    private final String content;
    private final String className;
    private final String packageName;

    public WriteCodeAction(String filePath, String content, String className, String packageName) {
        super("write_code", Path.of(filePath));
        this.filePath = filePath;
        this.content = content;
        this.className = className;
        this.packageName = packageName;

        // Precondition: parent package exists in ontology
        if (packageName != null && !packageName.isEmpty()) {
            addPrecondition(new Precondition() {
                @Override
                public boolean isSatisfiedBy(State state) {
                    if (state instanceof SoftwareSystemState sss) {
                        return sss.hasPackage(packageName);
                    }
                    return false;
                }

                @Override
                public String getDescription() {
                    return "package '" + packageName + "' exists in system ontology";
                }
            });
        }

        // Effect: type exists in package
        addEffect(new Effect() {
            @Override
            public State apply(State state) {
                if (state instanceof SoftwareSystemState sss) {
                    // Find the package using generic API
                    var pkgOpt = sss.getPackageGeneric(packageName);
                    if (pkgOpt.isEmpty()) {
                        return state;
                    }

                    // For now, just create a type reference using the generic API
                    // The actual type creation would use LanguageProvider
                    var pkg = pkgOpt.get();

                    return sss.withType(packageName, className);
                }
                return state;
            }

            @Override
            public String getDescription() {
                return "add type '" + className + "' to package '" + packageName + "'";
            }
        });
    }

    public String getFilePath() {
        return filePath;
    }

    public String getContent() {
        return content;
    }

    public String getClassName() {
        return className;
    }

    public String getPackageName() {
        return packageName;
    }

    /**
     * Builder for creating WriteCodeAction instances.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String filePath;
        private String content;
        private String className;
        private String packageName = "";

        public Builder filePath(String path) {
            this.filePath = path;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder className(String name) {
            this.className = name;
            return this;
        }

        public Builder packageName(String name) {
            this.packageName = name;
            return this;
        }

        public WriteCodeAction build() {
            Objects.requireNonNull(filePath, "filePath required");
            Objects.requireNonNull(content, "content required");
            return new WriteCodeAction(filePath, content, className, packageName);
        }
    }

    @Override
    public String toString() {
        return "WriteCode[" + filePath + "]";
    }
}