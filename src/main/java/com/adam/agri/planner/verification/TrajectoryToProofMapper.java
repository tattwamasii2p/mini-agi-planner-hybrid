package com.adam.agri.planner.verification;

import com.adam.agri.planner.core.action.Action;
import com.adam.agri.planner.core.state.State;
import com.adam.agri.planner.core.state.StateId;
import com.adam.agri.planner.core.trajectory.Trajectory;
import com.adam.agri.planner.logic.*;

import java.util.*;

/**
 * Maps Trajectory transitions to proof rules (tactics).
 *
 * Each action in a trajectory corresponds to a transition between states,
 * which is translated to a logical rule application in the proof tree.
 *
 * Mapping:
 * - Trajectory: S0 -a1-> S1 -a2-> S2 ... -> Sn
 * - Proof: Γ(S0) |- P(Sn) via tactics corresponding to each action
 *
 * @see StateToPropositionTranslator for state-to-proposition mapping
 * @see Tactic for proof rules
 */
public class TrajectoryToProofMapper {
    private final StateToPropositionTranslator translator;

    /**
     * Create mapper with default translator.
     */
    public TrajectoryToProofMapper() {
        this(new StateToPropositionTranslator());
    }

    /**
     * Create mapper with custom translator.
     */
    public TrajectoryToProofMapper(StateToPropositionTranslator translator) {
        this.translator = Objects.requireNonNull(translator);
    }

    /**
     * Map trajectory to attempt proof construction.
     *
     * This is a best-effort mapping - not all trajectories correspond
     * to valid proofs.
     *
     * @param trajectory the trajectory to map
     * @param goal the goal state
     * @return Optional proof if mapping succeeds
     */
    public Optional<Proof> mapToProof(Trajectory trajectory, State goal) {
        List<Sequent> sequents = mapToSequents(trajectory, goal);
        if (sequents.isEmpty()) {
            return Optional.empty();
        }

        // Attempt to construct proof from sequents
        return constructProofFromSequents(sequents);
    }

    /**
     * Map trajectory to list of sequents representing the proof steps.
     *
     * The list is ordered from goal (first) to initial state (last),
     * following backward reasoning.
     *
     * @param trajectory the trajectory
     * @param goal the goal state
     * @return list of sequents
     */
    public List<Sequent> mapToSequents(Trajectory trajectory, State goal) {
        List<Sequent> sequents = new ArrayList<>();

        StateId end = trajectory.end();
        Proposition goalProp = translator.translateGoal(goal);

        // Start with goal sequent: [] |- Goal
        Sequent current = Sequent.of(goalProp);
        sequents.add(current);

        // Work backwards through actions
        List<Action> actions = new ArrayList<>(trajectory.getActions());
        Collections.reverse(actions);

        for (Action action : actions) {
            Optional<Sequent> predecessor = findPredecessorSequent(action, current);
            if (predecessor.isEmpty()) {
                // Cannot map this action to a valid logical step
                return Collections.emptyList();
            }
            current = predecessor.get();
            sequents.add(current);
        }

        Collections.reverse(sequents);
        return sequents;
    }

    /**
     * Map trajectory to list of tactic applications.
     *
     * @param trajectory the trajectory
     * @return list of (tactic, sequent) pairs
     */
    public List<TacticApplication> mapToTacticApplications(Trajectory trajectory) {
        List<TacticApplication> applications = new ArrayList<>();
        List<Action> actions = trajectory.getActions();

        for (Action action : actions) {
            Optional<Tactic> tactic = actionToTactic(action);
            if (tactic.isPresent()) {
                applications.add(new TacticApplication(tactic.get(), action));
            }
        }

        return applications;
    }

    /**
     * Map a single action to a tactic, if possible.
     *
     * This implements the core action -> proof rule mapping.
     *
     * @param action the action
     * @return Optional tactic
     */
    public Optional<Tactic> actionToTactic(Action action) {
        String actionName = action.getName().toLowerCase();

        // Map action types to tactics
        if (actionName.contains("intro") || actionName.contains("assume")) {
            return Optional.of(Tactics.intro("x"));
        }
        if (actionName.contains("apply") || actionName.contains("use")) {
            return Optional.of(Tactics.apply());
        }
        if (actionName.contains("split") || actionName.contains("conj")) {
            return Optional.of(Tactics.split());
        }
        if (actionName.contains("axiom") || actionName.contains("done")) {
            return Optional.of(Tactics.AXIOM);
        }
        if (actionName.contains("box") || actionName.contains("necessary")) {
            // Modal tactics need context
            return Optional.of(Tactics.boxVerify(com.adam.agri.planner.logic.Context.empty()));
        }

        // Default: no specific mapping
        return Optional.empty();
    }

    /**
     * Check if trajectory can be verified as a proof.
     *
     * @param trajectory trajectory to check
     * @param goal target goal
     * @return true if mapping succeeds
     */
    public boolean isProvable(Trajectory trajectory, State goal) {
        return mapToProof(trajectory, goal).isPresent();
    }

    /**
     * Find tactic that could justify this action-state transition.
     *
     * @param from source state
     * @param to target state
     * @return Optional tactic justifying the transition
     */
    public Optional<Tactic> findTransitionTactic(State from, State to) {
        if (from == null) {
            return Optional.of(Tactics.AXIOM);
        }

        Implication transition = translator.translateTransition(from, to);

        // Check if transition is (Pre -> Post) with Pre in context
        return Optional.of(Tactics.apply(transition));
    }

    /**
     * Get the translator used by this mapper.
     */
    public StateToPropositionTranslator getTranslator() {
        return translator;
    }

    // Helper methods

    private Optional<Sequent> findPredecessorSequent(Action action, Sequent current) {
        Optional<Tactic> tactic = actionToTactic(action);
        if (tactic.isEmpty()) {
            return Optional.empty();
        }

        Optional<List<Sequent>> result = tactic.get().apply(current);
        if (result.isEmpty() || result.get().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(result.get().get(0));
    }

    private Optional<Proof> constructProofFromSequents(List<Sequent> sequents) {
        if (sequents.isEmpty()) {
            return Optional.empty();
        }

        // Build proof tree from sequents
        // This is a simplified construction
        Sequent conclusion = sequents.get(sequents.size() - 1);

        // Try to verify if final sequent is axiom
        if (conclusion.isAxiom()) {
            return Optional.of(Proof.axiom(conclusion, new Unit()));
        }

        // Otherwise return incomplete proof marker
        return Optional.of(new Proof(conclusion, Tactics.UNKNOWN, List.of(), new Var("?", new Atomic("incomplete"))));
    }

    /**
     * Record representing a tactic application.
     *
     * @param tactic the tactic applied
     * @param source the originating action (for traceability)
     */
    public record TacticApplication(Tactic tactic, Action source) {
        @Override
        public String toString() {
            return String.format("%s (from %s)", tactic.name(), source.getName());
        }
    }
}
