package com.adam.agri.planner.demo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.adam.agri.planner.agent.execution.ActuationPublisher;
import com.adam.agri.planner.agent.execution.ExecutionContext;
import com.adam.agri.planner.agent.execution.ExecutionResult;
import com.adam.agri.planner.agent.execution.FileActuator;
import com.adam.agri.planner.agent.execution.PhysicalExecutor;
import com.adam.agri.planner.agent.execution.SafetyMonitor;
import com.adam.agri.planner.core.action.Action;
import com.adam.agri.planner.core.constraints.ConstraintSet;
import com.adam.agri.planner.core.constraints.CostConstraint;
import com.adam.agri.planner.core.constraints.TimeConstraint;
import com.adam.agri.planner.core.state.StateId;
import com.adam.agri.planner.core.trajectory.Trajectory;
import com.adam.agri.planner.core.trajectory.TrajectoryMetrics;
import com.adam.agri.planner.demo.actions.CreatePackageAction;
import com.adam.agri.planner.demo.actions.WriteCodeAction;
import com.adam.agri.planner.demo.state.SoftwareSystemState;
import com.adam.agri.planner.planning.Condition;
import com.adam.agri.planner.planning.DijkstraPlanner;
import com.adam.agri.planner.planning.Goal;
import com.adam.agri.planner.planning.Plan;
import com.adam.agri.planner.planning.PlanningContext;
import com.adam.agri.planner.planning.UtilityFunction;
import com.adam.agri.planner.planning.WeightConfig;
import com.adam.agri.planner.sheaf.SheafGlue;
import com.adam.agri.planner.trajectories.builder.TrajectoriesBuilder;

/**
 * HybridPlannerDemoAutoCoder - demonstrates code generation via planning using
 * SoftwareSystem ontology.
 *
 * This demo: 1. Parses observations from a codebase (existing code structure)
 * 2. Parses a program specification (requirements) 3. Builds a SoftwareSystem
 * ontology entity representing the target system 4. Uses DijkstraPlanner to
 * generate a plan (sequence of ontology operations) 5. Executes the plan using
 * PhysicalExecutor with FileActuator 6. Writes generated code to the output
 * directory
 *
 * The SoftwareSystem ontology provides rich semantic structure: -
 * SoftwareSystem: abstract, versioned software entity (DOLCE Layer 3-4) -
 * JavaPackage: namespace containers within the system - JavaType: classes,
 * interfaces, enums with full type information - Dependencies: declared
 * requirements between systems
 *
 * Based on the Hybrid AGI Planner architecture from thread log
 * AgiIncremental_ThreadLog1.txt
 */
public class HybridPlannerDemoAutoCoder {

	private final Path outputRoot;
	private final ActuationPublisher publisher;

	public HybridPlannerDemoAutoCoder(Path outputRoot) {
		this.outputRoot = outputRoot;
		this.publisher = new ActuationPublisher();
		setupLogging();
	}

	private void setupLogging() {
		publisher.subscribe(event -> {
			switch (event.getType()) {
			case ACTION_STARTED -> System.out.println(" [EXEC] Starting: " + event.action().getName());
			case ACTION_COMPLETED -> System.out.println(" [EXEC] Completed: " + event.action().getName());
			case ACTION_FAILED -> System.out.println(" [EXEC] Failed: " + event.action().getName() + " - "
					+ (event.error() != null ? event.error().getMessage() : "unknown"));
			default -> System.out.println(" [EXEC] " + event.getType() + ": " + event.action().getName());
			}
		});
	}

	public static void main(String[] args) {
		System.out.println("=== HybridPlanner Demo: AutoCoder (SoftwareSystem Ontology) ===\n");
		System.out.println("Code generation via planning + ontology + sheaf gluing + execution\n");
		System.out.println("Uses SoftwareSystem (DOLCE Layer 3-4) as abstract software entity\n");

		// Parse command line arguments
		Config config = parseArgs(args);
		if (config == null) {
			printUsage();
			return;
		}

		try {
			// Create output directory
			Files.createDirectories(config.outputRoot);

			// Create demo instance
			HybridPlannerDemoAutoCoder demo = new HybridPlannerDemoAutoCoder(config.outputRoot);

			// Run the demo
			demo.run(config);

		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static class Config {
		Path codebaseRoot;
		String specification;
		Path outputRoot;
		String systemName;
		String systemVersion;
	}

	private static Config parseArgs(String[] args) {
		Config config = new Config();
		config.codebaseRoot = Paths.get(".");
		config.specification = "Create a simple Calculator class with add and subtract methods";
		config.outputRoot = Paths.get("./generated");
		config.systemName = "GeneratedSystem";
		config.systemVersion = "1.0.0";

		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith("--codebase=")) {
				config.codebaseRoot = Paths.get(args[i].substring(11));
			} else if (args[i].startsWith("--spec=")) {
				config.specification = args[i].substring(7);
			} else if (args[i].startsWith("--spec-file=")) {
				try {
					config.specification = Files.readString(Paths.get(args[i].substring(12)));
				} catch (IOException e) {
					System.err.println("Could not read spec file: " + e.getMessage());
				}
			} else if (args[i].startsWith("--output=")) {
				config.outputRoot = Paths.get(args[i].substring(9));
			} else if (args[i].startsWith("--name=")) {
				config.systemName = args[i].substring(7);
			} else if (args[i].startsWith("--version=")) {
				config.systemVersion = args[i].substring(10);
			}
		}

		return config;
	}

	private static void printUsage() {
		System.out.println("Usage: java HybridPlannerDemoAutoCoder [options]");
		System.out.println("Options:");
		System.out.println(" --codebase=<path> Root of existing codebase (default: .)");
		System.out.println(" --spec=<text> Program specification text");
		System.out.println(" --spec-file=<path> File containing program specification");
		System.out.println(" --output=<path> Output directory for generated code (default: ./generated)");
		System.out.println(" --name=<name> System name for SoftwareSystem ontology (default: GeneratedSystem)");
		System.out.println(" --version=<ver> System version (default: 1.0.0)");
	}

	public void run(Config config) throws Exception {
		System.out.println("--- Phase 1: Observation ---");
		System.out.println("Parsing codebase: " + config.codebaseRoot.toAbsolutePath());
		System.out.println("Specification: "
				+ config.specification.substring(0, Math.min(50, config.specification.length())) + "...\n");

		// Build observations from codebase and specification
		TrajectoriesBuilder builder = TrajectoriesBuilder.create();

		// Parse existing codebase into SoftwareSystem ontology
		SoftwareSystemState systemState = parseCodebaseIntoOntology(config.codebaseRoot, config.systemName,
				config.systemVersion, config.outputRoot);
		builder.withSymbolicObservation(systemState);

		// Parse specification as natural language
		builder.withGoal(config.specification);
		builder.withConstraint("Files should be placed in com.example.generated package");

		System.out.println("Ontology-based observations collected");
		System.out.println("SoftwareSystem: " + systemState.getSystemName() + ":" + systemState.getSystemVersion());
		System.out.println("Artifact ID: " + systemState.getArtifactId());

		// Extract goals from specification
		System.out.println("\n--- Phase 2: Goal Extraction ---");
		List<Goal> goals = extractGoalsFromSpec(config.specification, systemState);
		System.out.println("Extracted " + goals.size() + " goals");
		for (Goal g : goals) {
			System.out.println(" - " + g);
		}

		System.out.println("\n--- Phase 3: Planning ---");
		System.out.println("Using Dijkstra planner with modal reasoning (cost + alpha*risk)\n");

		// Create actions for code generation
		List<Action> codeGenActions = createCodeGenActions(goals, config.specification, config.outputRoot);
		System.out.println("Available actions: " + codeGenActions.size());

		// Setup planner with risk-weighted cost function
		WeightConfig weights = new WeightConfig(1.0, 0.5, 0.3, 0.0);
		DijkstraPlanner planner = new DijkstraPlanner(weights, codeGenActions);

		// Create planning context
		PlanningContext planningContext = new PlanningContext().withMaxDepth(100).withTimeout(30000L);

		// Add constraints
		ConstraintSet constraints = new ConstraintSet();
		constraints.addHard(new CostConstraint(1000.0));
		constraints.addHard(new TimeConstraint(10.0));
		planningContext.getHardConstraints().addHard(new CostConstraint(1000.0));
		planningContext.getHardConstraints().addHard(new TimeConstraint(10.0));

		// For each goal, generate a plan
		List<Plan> generatedPlans = new ArrayList<>();
		for (Goal goal : goals) {
			System.out.println("\nPlanning for: " + goal);
			Optional<Plan> plan = planner.plan(systemState, goal, planningContext);

			if (plan.isPresent()) {
				generatedPlans.add(plan.get());
				System.out.println(" Plan found with " + plan.get().length() + " steps");
			} else {
				System.out.println(" No plan found for this goal");
			}
		}

		// Combine plans using sheaf gluing if multiple goals
		System.out.println("\n--- Phase 4: Sheaf Gluing ---");
		Trajectory combinedTrajectory;
		if (generatedPlans.isEmpty()) {
			System.out.println("No plans to combine");
			return;
		} else if (generatedPlans.size() == 1) {
			combinedTrajectory = generatedPlans.get(0).toTrajectory();
			System.out.println("Single plan - no gluing needed");
		} else {
			combinedTrajectory = mergePlansWithSheaf(generatedPlans);
			System.out.println("Combined " + generatedPlans.size() + " plans via sheaf gluing");
		}

		System.out.println("Total actions: " + combinedTrajectory.getActions().size());
		System.out.println("Estimated cost: " + combinedTrajectory.cost());
		System.out.println("Estimated risk: " + combinedTrajectory.risk());

		// Execute the plan
		System.out.println("\n--- Phase 5: Execution ---");
		executePlan(combinedTrajectory, systemState, config.outputRoot);

		System.out.println("\n=== Generation Complete ===");
		System.out.println("Output written to: " + config.outputRoot.toAbsolutePath());
		System.out.println("SoftwareSystem state:");
		System.out.println(" Packages: " + systemState.getPackages().size());
		System.out.println(" Types: " + systemState.getTypeCount());
	}

	/**
	 * Parse existing codebase into SoftwareSystem ontology structure.
	 */
	private SoftwareSystemState parseCodebaseIntoOntology(Path root, String name, String version, Path outputRoot)
			throws IOException {
		StateId id = StateId.of("software_system_initial");
		SoftwareSystemState state = new SoftwareSystemState(id, name, version, outputRoot);

		if (!Files.exists(root)) {
			return state;
		}

		// Walk directory and collect Java files
		SoftwareSystemState[] stateHolder = new SoftwareSystemState[] { state };
		try (Stream<Path> paths = Files.walk(root)) {
			paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".java")).forEach(file -> {
				try {
					// Extract package name from file content
					String content = Files.readString(file);
					String packageName = extractPackageName(content);
					String className = extractClassName(content, file);

					if (packageName != null && !stateHolder[0].hasPackage(packageName)) {
						// Package creation happens via actions, not directly here
						// Just track what we observed
					}
				} catch (IOException e) {
					// Skip unreadable files
				}
			});
		}

		return stateHolder[0];
	}

	private String extractPackageName(String content) {
		// Simple extraction - look for "package " declaration
		int idx = content.indexOf("package ");
		if (idx >= 0) {
			int end = content.indexOf(';', idx);
			if (end > idx) {
				return content.substring(idx + 8, end).trim();
			}
		}
		return null;
	}

	private String extractClassName(String content, Path file) {
		// Try to extract class name from file
		String filename = file.getFileName().toString();
		if (filename.endsWith(".java")) {
			return filename.substring(0, filename.length() - 5);
		}
		return null;
	}

	/**
	 * Extract goals from specification text using SoftwareSystem ontology.
	 */
	private List<Goal> extractGoalsFromSpec(String spec, SoftwareSystemState initialState) {
		List<Goal> goals = new ArrayList<>();
		String lower = spec.toLowerCase();

		// Default package
		final String defaultPackage = "com.example.generated";

		// Goal 1: Package exists in ontology
		Condition packageExists = state -> {
			if (state instanceof SoftwareSystemState sss) {
				return sss.hasPackage(defaultPackage);
			}
			return false;
		};
		goals.add(new Goal(packageExists, UtilityFunction.standard()));

		// Goal 2: Classes exist (extracted from spec)
		Set<String> classNames = extractClassNamesFromSpec(spec);
		for (String className : classNames) {
			final String finalClassName = className;
			Condition classExists = state -> {
				if (state instanceof SoftwareSystemState sss) {
					return sss.hasClass(finalClassName);
				}
				return false;
			};
			goals.add(new Goal(classExists, UtilityFunction.standard()));
		}

		// If no specific goals, create a generic one
		if (goals.isEmpty()) {
			Condition systemNotEmpty = state -> {
				if (state instanceof SoftwareSystemState sss) {
					return !sss.getPackages().isEmpty();
				}
				return false;
			};
			goals.add(new Goal(systemNotEmpty, UtilityFunction.standard()));
		}

		return goals;
	}

	private String extractPackageFromSpec(String spec) {
		// Look for "package X" or "in package X"
		String[] patterns = { "package ", "in package " };
		for (String pattern : patterns) {
			int idx = spec.toLowerCase().indexOf(pattern);
			if (idx >= 0) {
				int start = idx + pattern.length();
				int end = spec.indexOf(' ', start);
				if (end < 0)
					end = spec.indexOf('\n', start);
				if (end < 0)
					end = spec.length();
				if (end > start) {
					return spec.substring(start, end).trim().replace(";", "");
				}
			}
		}
		return "com.example.generated";
	}

	private Set<String> extractClassNamesFromSpec(String spec) {
		Set<String> classes = new HashSet<>();
		String lower = spec.toLowerCase();

		// Look for patterns like "Create a X class" or "class X"
		String[] patterns = { "create a ", "class ", "define ", "implement " };
		for (String pattern : patterns) {
			int idx = 0;
			while ((idx = lower.indexOf(pattern, idx)) >= 0) {
				int start = idx + pattern.length();
				int end = lower.indexOf(' ', start + 1);
				if (end < 0)
					end = lower.indexOf(" class", start);
				if (end < 0)
					end = lower.length();

				if (end > start) {
					String potentialClass = spec.substring(start, end).trim();
					// Capitalize first letter
					if (!potentialClass.isEmpty() && Character.isLowerCase(potentialClass.charAt(0))) {
						potentialClass = Character.toUpperCase(potentialClass.charAt(0)) + potentialClass.substring(1);
					}
					if (isValidClassName(potentialClass)) {
						classes.add(potentialClass);
					}
				}
				idx = start;
			}
		}

		// If no classes found, infer from common patterns
		if (classes.isEmpty()) {
			if (lower.contains("calculator")) {
				classes.add("Calculator");
			} else if (lower.contains("service")) {
				classes.add("Service");
			} else if (lower.contains("manager")) {
				classes.add("Manager");
			} else {
				classes.add("GeneratedClass");
			}
		}

		return classes;
	}

	private boolean isValidClassName(String name) {
		if (name == null || name.isEmpty())
			return false;
		return Character.isUpperCase(name.charAt(0)) && name.matches("[a-zA-Z0-9_]+");
	}

	/**
	 * Create actions for code generation based on goals and spec.
	 */
	private List<Action> createCodeGenActions(List<Goal> goals, String spec, Path outputRoot) {
		List<Action> actions = new ArrayList<>();

		// Determine package
		String packageName = extractPackageFromSpec(spec);
		actions.add(new CreatePackageAction(packageName, outputRoot));

		// Determine what classes to create from spec
		Set<String> classNames = extractClassNamesFromSpec(spec);

		// Add code writing actions for each class
		for (String className : classNames) {
			String filePath = packageName.replace('.', '/') + "/" + className + ".java";
			String content = generateClassCode(className, packageName, spec);
			actions.add(new WriteCodeAction(filePath, content, className, packageName));
		}

		return actions;
	}

	/**
	 * Generate code for a specific class.
	 */
	private String generateClassCode(String className, String packageName, String spec) {
		StringBuilder sb = new StringBuilder();
		String lower = spec.toLowerCase();

		// Package declaration
		sb.append("package ").append(packageName).append(";\n\n");

		// Javadoc
		sb.append("/**\n");
		sb.append(" * Generated by HybridPlannerDemoAutoCoder\n");
		sb.append(" * Ontology: SoftwareSystem (DOLCE Layer 3-4)\n");
		sb.append(" * Specification: ").append(spec.substring(0, Math.min(60, spec.length()))).append("\n");
		sb.append(" */\n");

		// Class declaration
		sb.append("public class ").append(className).append(" {\n\n");

		// Generate methods based on specification keywords
		if (lower.contains("calculator")) {
			if (lower.contains("add")) {
				sb.append("    /**\n");
				sb.append("     * Adds two integers.\n");
				sb.append("     * Ontology: JavaMethod with return type JavaType.INT\n");
				sb.append("     */\n");
				sb.append("    public int add(int a, int b) {\n");
				sb.append("        return a + b;\n");
				sb.append("    }\n\n");
			}
			if (lower.contains("subtract")) {
				sb.append("    /**\n");
				sb.append("     * Subtracts two integers.\n");
				sb.append("     * Ontology: JavaMethod with return type JavaType.INT\n");
				sb.append("     */\n");
				sb.append("    public int subtract(int a, int b) {\n");
				sb.append("        return a - b;\n");
				sb.append("    }\n\n");
			}
			if (lower.contains("multiply")) {
				sb.append("    /**\n");
				sb.append("     * Multiplies two integers.\n");
				sb.append("     * Ontology: JavaMethod with return type JavaType.INT\n");
				sb.append("     */\n");
				sb.append("    public int multiply(int a, int b) {\n");
				sb.append("        return a * b;\n");
				sb.append("    }\n\n");
			}
			if (lower.contains("divide")) {
				sb.append("    /**\n");
				sb.append("     * Divides two doubles.\n");
				sb.append("     * Ontology: JavaMethod with return type JavaType.DOUBLE\n");
				sb.append("     */\n");
				sb.append("    public double divide(double a, double b) {\n");
				sb.append("        if (b == 0) {\n");
				sb.append("            throw new IllegalArgumentException(\"Division by zero\");\n");
				sb.append("        }\n");
				sb.append("        return a / b;\n");
				sb.append("    }\n\n");
			}
		} else {
			// Generic placeholder methods
			sb.append("    // TODO: Implement based on specification\n");
			sb.append("    public void execute() {\n");
			sb.append("        // Implementation required\n");
			sb.append("    }\n\n");
		}

		sb.append("}\n");

		return sb.toString();
	}

	/**
	 * Merge multiple plans using sheaf gluing. Trajectories are merged when end(a)
	 * == start(b).
	 */
	private Trajectory mergePlansWithSheaf(List<Plan> plans) {
		SheafGlue glue = new SheafGlue();

		// Convert plans to trajectories
		List<Trajectory> trajectories = new ArrayList<>();
		for (int i = 0; i < plans.size(); i++) {
			Trajectory traj = plans.get(i).toTrajectory();
			trajectories.add(traj);
			glue.collect(traj, "agent_" + i);
		}

		// Try to merge trajectories in order
		if (trajectories.isEmpty()) {
			return null;
		}

		Trajectory result = trajectories.get(0);
		for (int i = 1; i < trajectories.size(); i++) {
			Optional<Trajectory> merged = Trajectory.tryMerge(result, trajectories.get(i));
			if (merged.isPresent()) {
				result = merged.get();
			} else {
				// Incompatible - just append actions
				List<Action> combined = new ArrayList<>(result.getActions());
				combined.addAll(trajectories.get(i).getActions());
				result = new Trajectory(result.start(), trajectories.get(i).end(), combined,
						new TrajectoryMetrics(result.cost() + trajectories.get(i).cost(),
								result.time() + trajectories.get(i).time(),
								result.probability() * trajectories.get(i).probability(),
								Math.max(result.risk(), trajectories.get(i).risk()), 0));
			}
		}

		return result;
	}

	/**
	 * Execute the generated plan using PhysicalExecutor with FileActuator.
	 */
	private void executePlan(Trajectory trajectory, SoftwareSystemState initialState, Path outputRoot) {
		// Create actuators
		List<PhysicalExecutor.Actuator> actuators = new ArrayList<>();
		actuators.add(new FileActuator(outputRoot));

		// Create executor
		SafetyMonitor safety = new SafetyMonitor();
		PhysicalExecutor executor = new PhysicalExecutor(actuators, safety);

		// Execute each action
		SoftwareSystemState currentState = initialState;
		int actionsExecuted = 0;
		for (Action action : trajectory.getActions()) {
			// Create execution context
			ExecutionContext context = ExecutionContext.builder(currentState).timeout(30000).verifyEffects(true)
					.build();

			// Check if applicable
			if (!action.isApplicableIn(currentState)) {
				System.out.println(" [SKIP] " + action.getName() + " - preconditions not met");
				continue;
			}

			// Execute
			publisher.publishActionStarted(action, context);
			ExecutionResult result = executor.execute(action, context);
			publisher.publishActionCompleted(action, context, result);

			if (result.isSuccess()) {
				// Update state (apply effects)
				currentState = (SoftwareSystemState) action.apply(currentState);
				actionsExecuted++;
				System.out.println(" [OK] " + action.getName());
			} else {
				System.out.println(" [FAIL] " + action.getName() + ": " + result.getErrorMessage());
			}
		}

		// Print final ontology stats
		System.out.println("\nOntology Execution Stats:");
		System.out.println(" Actions executed: " + actionsExecuted);
		System.out.println(" SoftwareSystem: " + currentState.getSystemName());
		System.out.println(" Version: " + currentState.getSystemVersion());
		System.out.println(" Packages: " + currentState.getPackages().size());
		System.out.println(" Types defined: " + currentState.getTypeCount());
	}
}
