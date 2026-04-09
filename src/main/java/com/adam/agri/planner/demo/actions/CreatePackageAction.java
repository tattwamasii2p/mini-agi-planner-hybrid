package com.adam.agri.planner.demo.actions;

import com.adam.agri.planner.core.action.Effect;
import com.adam.agri.planner.core.action.Precondition;
import com.adam.agri.planner.core.state.State;
import com.adam.agri.planner.demo.state.SoftwareSystemState;
import com.adam.agri.planner.symbolic.ontology.computer.code.language.java.*;
import com.adam.agri.planner.symbolic.ontology.upper.EntityId;
import com.adam.agri.planner.symbolic.ontology.upper.Property;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;

/**
 * Action to create a package in the software system ontology.
 *
 * Creates a JavaPackage entity and adds it to the SoftwareSystem.
 * This represents the ontology-level package creation, distinct from
 * physical file system operations (which are handled by FileActuator).
 */
public class CreatePackageAction extends FileAction {
 private final String packageName;

 public CreatePackageAction(String packageName, Path codebaseRoot) {
 super("create_package", Paths.get(packageName.replace('.', '/')));
 this.packageName = packageName;

 // Precondition: package doesn't exist in the software system
 addPrecondition(new Precondition() {
 @Override
 public boolean isSatisfiedBy(State state) {
 if (state instanceof SoftwareSystemState sss) {
 return !sss.hasPackage(packageName);
 }
 return false;
 }

 @Override
 public String getDescription() {
 return "package '" + packageName + "' does not exist in system";
 }
 });

 // Effect: package now exists in the ontology
 addEffect(new Effect() {
 @Override
 public State apply(State state) {
 if (state instanceof SoftwareSystemState sss) {
 // Create JavaPackage ontology entity
 JavaPackage pkg = new JavaPackage(
 EntityId.of("pkg:" + packageName),
 Set.of(),
 packageName,
 Optional.empty() // root package
 );
 return sss.withPackage(pkg);
 }
 return state;
 }

 @Override
 public String getDescription() {
 return "create ontology package '" + packageName + "'";
 }
 });
 }

 public String getPackageName() {
 return packageName;
 }

 /**
 * Get the directory path for this package.
 */
 public Path getDirectoryPath(Path codebaseRoot) {
 return codebaseRoot.resolve(packageName.replace('.', '/'));
 }

 @Override
 public String toString() {
 return "CreatePackage[" + packageName + "]";
 }
}
