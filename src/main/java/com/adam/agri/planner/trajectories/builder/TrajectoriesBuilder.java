package com.adam.agri.planner.trajectories.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import com.adam.agri.planner.agent.perception.PerceptionEvent;
import com.adam.agri.planner.core.constraints.ConstraintSet;
import com.adam.agri.planner.core.state.PhysicalState;
import com.adam.agri.planner.core.state.Predicate;
import com.adam.agri.planner.core.state.SymbolicState;
import com.adam.agri.planner.core.trajectory.Trajectory;
import com.adam.agri.planner.planning.Goal;
import com.adam.agri.planner.symbolic.reasoning.llm.LLMReasoningBridge;
import com.adam.agri.planner.trajectories.builder.fusion.ObservationFusion;
import com.adam.agri.planner.trajectories.builder.fusion.ObservationFusion.FusedEntity;
import com.adam.agri.planner.trajectories.builder.goals.DesireModel;
import com.adam.agri.planner.trajectories.builder.goals.GoalConstraintExtractor;
import com.adam.agri.planner.trajectories.builder.observations.NaturalLanguageAdapter;
import com.adam.agri.planner.trajectories.builder.observations.ObservationSource;
import com.adam.agri.planner.trajectories.builder.observations.PhysicalObservationAdapter;
import com.adam.agri.planner.trajectories.builder.observations.SymbolicObservationAdapter;
import com.adam.agri.planner.trajectories.builder.worldmodel.ComputerSystemsModelBuilder;
import com.adam.agri.planner.trajectories.builder.worldmodel.ComputerSystemsModelBuilder.ComputerSystemsModel;
import com.adam.agri.planner.trajectories.builder.worldmodel.IntegratedWorldModel;
import com.adam.agri.planner.trajectories.builder.worldmodel.PhysicalWorldModelBuilder;
import com.adam.agri.planner.trajectories.builder.worldmodel.PhysicalWorldModelBuilder.PhysicalWorldModel;

/**
 * Main trajectories builder integrating multiple observation sources.
 *
 * Combines:
 * - Symbolic observations (SymbolicState, Predicate)
 * - Physical observations (PerceptionEvent, PhysicalState)
 * - Natural language (goals, constraints, descriptions)
 */
public class TrajectoriesBuilder {

    private final SymbolicObservationAdapter symbolicAdapter;
    private final PhysicalObservationAdapter physicalAdapter;
    private final NaturalLanguageAdapter nlAdapter;
    private final List<ObservationSource<?>> customAdapters;

    private LLMReasoningBridge llmBridge;
    private double confidenceThreshold;
    private final GoalConstraintExtractor goalExtractor;

    public enum NLType {
        GOAL, CONSTRAINT, WORLD_DESC, DESIRE, ACTION
    }

    public enum Optimizer {
        MIN_COST, MIN_TIME, MIN_RISK, MAX_UTILITY, BALANCED, PARETO
    }

    private TrajectoriesBuilder() {
        this.symbolicAdapter = new SymbolicObservationAdapter();
        this.physicalAdapter = new PhysicalObservationAdapter();
        this.nlAdapter = new NaturalLanguageAdapter();
        this.customAdapters = new ArrayList<>();
        this.goalExtractor = new GoalConstraintExtractor();
        this.confidenceThreshold = 0.5;
    }

    public static TrajectoriesBuilder create() {
        return new TrajectoriesBuilder();
    }

    public TrajectoriesBuilder withSymbolicObservation(SymbolicState state) {
        symbolicAdapter.addObservation(state);
        return this;
    }

    public TrajectoriesBuilder withSymbolicPredicate(Predicate predicate) {
        symbolicAdapter.addPredicate(predicate);
        return this;
    }

    public TrajectoriesBuilder withPhysicalObservation(PhysicalState state) {
        physicalAdapter.addPhysicalState(state);
        return this;
    }

    public TrajectoriesBuilder withPhysicalObservation(PerceptionEvent event) {
        physicalAdapter.addPerceptionEvent(event);
        return this;
    }

    public TrajectoriesBuilder withPhysicalObservations(List<PerceptionEvent> events) {
        for (PerceptionEvent event : events) {
            physicalAdapter.addPerceptionEvent(event);
        }
        return this;
    }

    public TrajectoriesBuilder withNaturalLanguage(String text, NLType type) {
        NaturalLanguageAdapter.NLType nlType = switch (type) {
            case GOAL -> NaturalLanguageAdapter.NLType.GOAL;
            case CONSTRAINT -> NaturalLanguageAdapter.NLType.CONSTRAINT;
            case WORLD_DESC -> NaturalLanguageAdapter.NLType.WORLD_DESC;
            case DESIRE -> NaturalLanguageAdapter.NLType.DESIRE;
            case ACTION -> NaturalLanguageAdapter.NLType.ACTION;
        };
        nlAdapter.addText(text, nlType);
        goalExtractor.addGoal(text);
        return this;
    }

    public TrajectoriesBuilder withGoal(String goalText) {
        nlAdapter.addGoal(goalText);
        goalExtractor.addGoal(goalText);
        return this;
    }

    public TrajectoriesBuilder withConstraint(String constraintText) {
        nlAdapter.addConstraint(constraintText);
        goalExtractor.addConstraint(constraintText);
        return this;
    }

    public TrajectoriesBuilder withWorldDescription(String description) {
        nlAdapter.addWorldDescription(description);
        return this;
    }

    public TrajectoriesBuilder withDesire(String preference) {
        nlAdapter.addDesire(preference);
        goalExtractor.addDesire(preference);
        return this;
    }

    public TrajectoriesBuilder withLLMBridge(LLMReasoningBridge bridge) {
        this.llmBridge = bridge;
        nlAdapter.setLLMBridge(bridge);
        return this;
    }

    public TrajectoriesBuilder withConfidenceThreshold(double threshold) {
        this.confidenceThreshold = Math.max(0, Math.min(1, threshold));
        return this;
    }

    public IntegratedWorldModel buildWorldModel() {
        List<ObservationSource<?>> allSources = new ArrayList<>();
        allSources.add(symbolicAdapter);
        allSources.add(physicalAdapter);
        allSources.add(nlAdapter);
        allSources.addAll(customAdapters);

        List<ObservationSource<?>> validSources = allSources.stream()
            .filter(s -> s.getSourceConfidence() >= confidenceThreshold)
            .filter(ObservationSource::hasObservations)
            .toList();

        if (validSources.isEmpty()) {
            return new IntegratedWorldModel(
                new ComputerSystemsModel(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()),
                new PhysicalWorldModel(new ArrayList<>(), new ArrayList<>(), new HashMap<>())
            );
        }

        ObservationFusion fusion = new ObservationFusion();
        List<FusedEntity> fused = fusion.fuse(validSources);

        ComputerSystemsModelBuilder computerBuilder = new ComputerSystemsModelBuilder();
        List<String> worldDescs = nlAdapter.getWorldDescriptionTexts();
        for (String desc : worldDescs) {
            computerBuilder.parseNaturalLanguage(desc);
        }
        computerBuilder.fromEntities(
            fused.stream().map(FusedEntity::getEntity).toList()
        );
        ComputerSystemsModel computerModel = computerBuilder.build();

        PhysicalWorldModelBuilder physicalBuilder = new PhysicalWorldModelBuilder();
        for (String desc : worldDescs) {
            physicalBuilder.parseNaturalLanguage(desc);
        }
        physicalBuilder.fromObservationEntities(
            fused.stream().map(FusedEntity::getEntity).toList()
        );
        PhysicalWorldModel physicalModel = physicalBuilder.build();

        return new IntegratedWorldModel(computerModel, physicalModel);
    }

    public List<Goal> extractGoals() {
        return goalExtractor.extractGoals();
    }

    public Optional<Goal> extractMainGoal() {
        return goalExtractor.extractMainGoal();
    }

    public ConstraintSet extractConstraints() {
        return goalExtractor.extractConstraints();
    }

    public DesireModel extractDesires() {
        return goalExtractor.extractDesires();
    }

    public List<Trajectory> generateTrajectories() {
        return generateTrajectories(buildWorldModel());
    }

    public List<Trajectory> generateTrajectories(IntegratedWorldModel world) {
        Optional<Goal> goal = extractMainGoal();
        if (goal.isEmpty()) {
            return Collections.emptyList();
        }

        List<Trajectory> trajectories = new ArrayList<>();
        var systems = world.getComputerSystems().getAvailableSystems();
        if (systems.size() >= 2) {
            var traj = world.computeTrajectory(
                systems.get(0),
                systems.get(1),
                new IntegratedWorldModel.TrajectoryPreferences()
            );
        }

        return trajectories;
    }

    public Trajectory selectOptimal(List<Trajectory> candidates, Optimizer strategy) {
        if (candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        switch (strategy) {
            case MIN_COST:
                return candidates.stream()
                    .min(Comparator.comparingDouble(Trajectory::cost))
                    .orElse(candidates.get(0));
            case MIN_TIME:
                return candidates.stream()
                    .min(Comparator.comparingDouble(Trajectory::time))
                    .orElse(candidates.get(0));
            case MIN_RISK:
                return candidates.stream()
                    .min(Comparator.comparingDouble(Trajectory::risk))
                    .orElse(candidates.get(0));
            case MAX_UTILITY:
                return candidates.stream()
                    .max(Comparator.comparingDouble(t -> t.probability() - t.risk()))
                    .orElse(candidates.get(0));
            case BALANCED:
                return candidates.stream()
                    .min(Comparator.comparingDouble(t -> 0.5 * t.cost() + 0.3 * t.time() + 0.2 * t.risk()))
                    .orElse(candidates.get(0));
            case PARETO:
            default:
                return candidates.get(0);
        }
    }

    public TrajectoriesBuilder clear() {
        symbolicAdapter.clear();
        physicalAdapter.clear();
        nlAdapter.clear();
        customAdapters.clear();
        return this;
    }

    @Override
    public String toString() {
        return "TrajectoriesBuilder{" +
               "symbolic=" + symbolicAdapter.getObservations().size() +
               ", physical=" + physicalAdapter.getObservations().size() +
               ", nl=" + nlAdapter.getObservations().size() +
               "}";
    }
}
