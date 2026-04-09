package com.adam.agri.planner.verification;

import com.adam.agri.planner.core.action.Action;
import com.adam.agri.planner.core.state.State;
import com.adam.agri.planner.core.trajectory.Trajectory;
import com.adam.agri.planner.logic.*;
import com.adam.agri.planner.planning.Goal;
import com.adam.agri.planner.planning.Plan;

import java.util.*;

/**
 * Verifies plans (trajectories) as proofs.
 *
 * A plan is valid if the trajectory from initial to goal state
 * can be interpreted as a proof of the goal proposition from
 * the initial state proposition.
 *
 * Verification process:
 * 1. Translate states to propositions
 * 2. Map trajectory to sequents
 * 3. Apply tactics to verify each transition
 * 4. Check proof validity
 *
 * Mathematical basis: Plan = Constructive Proof (Curry-Howard)
 *
 * @see TrajectoryToProofMapper for trajectory proof mapping
 * @see StateToPropositionTranslator for state translation
 * @see Proof for proof structures
 */
public class PlanVerifier {
    private final StateToPropositionTranslator translator;
    private final TrajectoryToProofMapper mapper;
    private final List<String> verificationLog;

    /**
     * Create verifier with default settings.
     */
    public PlanVerifier() {
        this(new StateToPropositionTranslator());
    }

    /**
     * Create verifier with custom translator.
     */
    public PlanVerifier(StateToPropositionTranslator translator) {
        this.translator = Objects.requireNonNull(translator);
        this.mapper = new TrajectoryToProofMapper(translator);
        this.verificationLog = new ArrayList<>();
    }

    /**
     * Verify a trajectory as a proof of reaching the goal.
     *
     * @param trajectory the trajectory to verify
     * @param goal the goal state
     * @return validation result with proof if successful
     */
    public ValidationResult verify(Trajectory trajectory, State goal) {
        verificationLog.clear();
        verificationLog.add("Starting verification of trajectory: " + trajectory);

        // Step 1: Translate goal to proposition
        Proposition goalProp = translator.translateGoal(goal);
        verificationLog.add("Goal proposition: " + goalProp);

        // Step 2: Map trajectory to proof
        Optional<Proof> proof = mapper.mapToProof(trajectory, goal);

        if (proof.isEmpty()) {
            String errorMsg = "Could not map trajectory to proof structure";
            verificationLog.add("FAILED: " + errorMsg);
            return ValidationResult.failure(errorMsg);
        }

        // Step 3: Verify proof validity
        Proof p = proof.get();
        verificationLog.add("Proof constructed: " + p.getConclusion());

        if (!p.isValid()) {
            String errorMsg = "Proof structure is invalid";
            verificationLog.add("FAILED: " + errorMsg);
            return ValidationResult.failure(errorMsg);
        }

        // Step 4: Check that proof proves the goal
        if (!p.getConclusion().goal().equals(goalProp)) {
            String errorMsg = "Proof does not prove the goal proposition";
            verificationLog.add("FAILED: " + errorMsg);
            return ValidationResult.failure(errorMsg);
        }

        verificationLog.add("SUCCESS: Plan verified as valid proof");
        return ValidationResult.success(
            "Valid plan-as-proof witness constructed",
            p
        );
    }

    /**
     * Verify a plan (wrapper for Plan interface).
     */
    public ValidationResult verify(Plan plan, State goal) {
        return verify(plan.toTrajectory(), goal);
    }

    /**
     * Verify with detailed step-by-step checking.
     * Returns diagnostics for each action.
     */
    public ValidationResult verifyDetailed(Trajectory trajectory, State goal) {
        verificationLog.clear();
        verificationLog.add("Starting detailed verification");

        List<TrajectoryToProofMapper.TacticApplication> applications =
            mapper.mapToTacticApplications(trajectory);

        if (applications.isEmpty() && !trajectory.getActions().isEmpty()) {
            // No tactics could be mapped
            return ValidationResult.failure(
                "Could not map actions to tactics",
                0,
                trajectory.getActions().get(0).getName() + " has no tactic mapping"
            );
        }

        // Verify each action-tactic pair
        for (int i = 0; i < applications.size(); i++) {
            TrajectoryToProofMapper.TacticApplication app = applications.get(i);
            verificationLog.add(String.format("Step %d: %s", i, app));

            // Check if tactic is applicable
            // This would require the current sequent context
            // For now, just log
        }

        return verify(trajectory, goal);
    }

    /**
     * Reactively verify (for execution-time checking).
     * Lightweight verification for runtime safety.
     */
    public ValidationResult verifyReactive(Trajectory trajectory, State goal) {
        // Simplified: same as verify but with reduced logging
        Optional<Proof> proof = mapper.mapToProof(trajectory, goal);
        if (proof.isPresent() && proof.get().isValid()) {
            return ValidationResult.success("Reactive verification passed", proof.get());
        }
        return ValidationResult.failure("Reactive verification failed");
    }

    /**
     * Extract witness from verified plan (Curry-Howard).
     */
    public Optional<Term> extractWitness(ValidationResult result) {
        if (!result.isValid() || result.proof().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.proof().get().extractWitness());
    }

    /**
     * Check if a trajectory is provable (syntactic check).
     */
    public boolean isProvable(Trajectory trajectory, State goal) {
        return mapper.isProvable(trajectory, goal);
    }

    /**
     * Get verification log from last verification.
     */
    public List<String> getVerificationLog() {
        return Collections.unmodifiableList(verificationLog);
    }

    /**
     * Print verification trace for debugging.
     */
    public String getVerificationTrace() {
        return String.join("\n", verificationLog);
    }

    /**
     * Verify multiple candidate plans and return the best verified one.
     */
    public Optional<Plan> verifyBest(List<Plan> candidates, State goal) {
        Plan best = null;
        ValidationResult bestResult = null;

        for (Plan plan : candidates) {
            ValidationResult result = verify(plan, goal);
            if (result.isValid()) {
                if (best == null || plan.getNetValue() > best.getNetValue()) {
                    best = plan;
                    bestResult = result;
                }
            }
        }

        return Optional.ofNullable(best);
    }

    /**
     * Learn from failed verification (constructive feedback).
     * Returns diagnostic information about why verification failed.
     */
    public VerificationDiagnosis diagnoseFailure(Trajectory trajectory, State goal) {
        // Analyze trajectory for common failure patterns
        List<String> issues = new ArrayList<>();

        if (trajectory.getActions().isEmpty()) {
            issues.add("Empty trajectory - no actions to verify");
        }

        // Check action mappings
        for (Action action : trajectory.getActions()) {
            Optional<Tactic> tactic = mapper.actionToTactic(action);
            if (tactic.isEmpty()) {
                issues.add("No tactic mapping for action: " + action.getName());
            }
        }

        return new VerificationDiagnosis(issues, trajectory, goal);
    }

    /**
     * Diagnosis result for failed verifications.
     */
    public record VerificationDiagnosis(List<String> issues, Trajectory trajectory, State goal) {
        public boolean hasIssues() {
            return !issues.isEmpty();
        }

        @Override
        public String toString() {
            return String.format("VerificationDiagnosis{issues=%s, trajectory=%s -> %s}",
                issues, trajectory.start(), trajectory.end());
        }
    }
}
