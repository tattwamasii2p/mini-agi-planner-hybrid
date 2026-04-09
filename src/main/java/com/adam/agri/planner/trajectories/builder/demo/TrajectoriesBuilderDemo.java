package com.adam.agri.planner.trajectories.builder.demo;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.adam.agri.planner.agent.perception.PerceptionEvent;
import com.adam.agri.planner.core.state.PhysicalState;
import com.adam.agri.planner.core.state.StateId;
import com.adam.agri.planner.core.state.SymbolicState;
import com.adam.agri.planner.symbolic.ontology.upper.EntityId;
import com.adam.agri.planner.trajectories.builder.TrajectoriesBuilder;
import com.adam.agri.planner.trajectories.builder.worldmodel.IntegratedWorldModel;

/**
 * Demonstration of the TrajectoriesBuilder.
 */
public class TrajectoriesBuilderDemo {

    public static void main(String[] args) {
        System.out.println("=== Trajectories Builder Demo ===\n");

        demo1SymbolicObservations();
        demo2PhysicalObservations();
        demo3NaturalLanguage();
        demo4IntegratedWorldModel();
    }

    static void demo1SymbolicObservations() {
        System.out.println("--- Demo 1: Symbolic Observations ---");

        SymbolicState state = new SymbolicState(
            StateId.of("datacenter_state"),
            Set.of(),
            Map.of("region", "us-east", "server_count", 3)
        );

        System.out.println("Symbolic observation: " + state);

        TrajectoriesBuilder builder = TrajectoriesBuilder.create()
            .withSymbolicObservation(state);

        System.out.println("  Added to builder: " + builder);
        System.out.println();
    }

    static void demo2PhysicalObservations() {
        System.out.println("--- Demo 2: Physical Observations ---");

        double[] robotPosition = {1.5, 2.0, 0.0};
        PhysicalState robotState = new PhysicalState(
            StateId.of("robot_001"),
            robotPosition,
            Map.of("battery", 0.85, "temperature", 22.5),
            System.currentTimeMillis()
        );

        PerceptionEvent cameraEvent = PerceptionEvent.builder()
            .source(new EntityId("camera_001"))
            .timestamp(Instant.now())
            .type(PerceptionEvent.PerceptionType.VISUAL)
            .feature("objects_detected", List.of("chair", "table"))
            .feature("distance_m", 2.5)
            .confidence(0.85)
            .salience(0.7)
            .build();

        System.out.println("Physical state: " + robotState);
        System.out.println("Perception event: type=" + cameraEvent.getType() +
                           ", confidence=" + cameraEvent.getConfidence());

        TrajectoriesBuilder builder = TrajectoriesBuilder.create()
            .withPhysicalObservation(robotState)
            .withPhysicalObservation(cameraEvent);

        System.out.println("  Added to builder: " + builder);
        System.out.println();
    }

    static void demo3NaturalLanguage() {
        System.out.println("--- Demo 3: Natural Language ---");

        String goal = "Deploy the web service on the least loaded server";
        String constraint = "Complete within 5 minutes";
        String desire = "Prefer eco-friendly data centers";
        String worldDesc = "3 servers: 4 cores/16GB, 8 cores/64GB, 2 cores/8GB";

        TrajectoriesBuilder builder = TrajectoriesBuilder.create()
            .withGoal(goal)
            .withConstraint(constraint)
            .withDesire(desire)
            .withWorldDescription(worldDesc);

        System.out.println("Natural language inputs:");
        System.out.println("  Goal: " + goal);
        System.out.println("  Constraint: " + constraint);
        System.out.println("  Desire: " + desire);

        var goals = builder.extractGoals();
        System.out.println("\nExtracted goals: " + goals.size());
        System.out.println();
    }

    static void demo4IntegratedWorldModel() {
        System.out.println("--- Demo 4: Integrated World Model ---");

        TrajectoriesBuilder builder = TrajectoriesBuilder.create()
            .withSymbolicObservation(new SymbolicState(
                StateId.of("initial"),
                Set.of(),
                Map.of("task", "deployment")
            ))
            .withWorldDescription("Servers: web-server (4 cores, 16GB), db-server (8 cores, 64GB)")
            .withGoal("Deploy service with low latency")
            .withConstraint("Under 10 minutes");

        System.out.println("Building integrated world model...");

        IntegratedWorldModel world = builder.buildWorldModel();

        System.out.println("Computer systems: " + world.getComputerSystems().getSystems().size());
        System.out.println("Physical entities: " + world.getPhysicalWorld().getPhysicalEntities().size());
        System.out.println("Total entities: " + world.getAllEntities().size());

        var goals = builder.extractGoals();
        System.out.println("\nExtracted goals: " + goals.size());
        System.out.println();
    }
}
