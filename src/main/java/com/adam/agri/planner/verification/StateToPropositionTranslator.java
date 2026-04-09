package com.adam.agri.planner.verification;

import com.adam.agri.planner.core.state.State;
import com.adam.agri.planner.core.state.StateType;
import com.adam.agri.planner.logic.Atomic;
import com.adam.agri.planner.logic.Implication;
import com.adam.agri.planner.logic.Modal;
import com.adam.agri.planner.logic.Proposition;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Translates planner States to logic Propositions.
 *
 * This bridge allows verification of plans as proofs by mapping
 * the physical/symbolic state space to the logical proposition space.
 *
 * Translation rules:
 * - State Type -> Proposition type
 * - State Properties -> Atomic propositions
 * - State Relations -> Modal/Implication propositions
 */
public class StateToPropositionTranslator {
    private final Map<String, Proposition> cache;
    private final TranslationStrategy strategy;

    /**
     * Translation strategy enum.
     */
    public enum TranslationStrategy {
        PREDICATE,      // Each state as unary predicate
        TYPE,           // States as types
        RELATION,       // States as relations
        MODAL           // States as modal operators
    }

    /**
     * Create translator with default strategy.
     */
    public StateToPropositionTranslator() {
        this(TranslationStrategy.PREDICATE);
    }

    /**
     * Create translator with specific strategy.
     */
    public StateToPropositionTranslator(TranslationStrategy strategy) {
        this.strategy = strategy;
        this.cache = new HashMap<>();
    }

    /**
     * Translate a State to a Proposition.
     *
     * @param state the state to translate
     * @return corresponding proposition
     */
    public Proposition translate(State state) {
        String key = getCacheKey(state);

        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        Proposition result = switch (strategy) {
            case PREDICATE -> translateAsPredicate(state);
            case TYPE -> translateAsType(state);
            case RELATION -> translateAsRelation(state);
            case MODAL -> translateAsModal(state);
        };

        cache.put(key, result);
        return result;
    }

    /**
     * Translate state as predicate: P(state).
     */
    private Proposition translateAsPredicate(State state) {
        String typeName = state.getType().toString().toLowerCase();
        String stateName = state.getId().toString();
        return new Atomic("HasProperty(" + stateName + ", " + typeName + ")");
    }

    /**
     * Translate state as type.
     */
    private Proposition translateAsType(State state) {
        // State as type annotation
        String stateName = state.getId().toString();
        return new Atomic("StateType_" + stateName);
    }

    /**
     * Translate state as relation between properties.
     */
    private Proposition translateAsRelation(State state) {
        // Simplified: state holds certain properties
        String stateName = state.getId().toString();
        return new Atomic("Holds(" + stateName + ")");
    }

    /**
     * Translate state as modal proposition.
     * Physical states become "necessary", belief states become "belief".
     */
    private Proposition translateAsModal(State state) {
        String stateName = state.getId().toString();
        Proposition base = new Atomic("State_" + stateName);

        return switch (state.getType()) {
            case PHYSICAL -> base;                    // Physical: direct truth
            case SYMBOLIC -> new Modal(base, Modal.Modality.POSSIBLE);
            case BELIEF -> new Modal(base, Modal.Modality.BELIEF);
            default -> base;
        };
    }

    /**
     * Translate precondition between states as implication.
     * precondition -> postcondition
     *
     * @param from source state (precondition)
     * @param to target state (postcondition)
     * @return implication proposition
     */
    public Implication translateTransition(State from, State to) {
        Proposition pre = translate(from);
        Proposition post = translate(to);
        return new Implication(pre, post);
    }

    /**
     * Create a proposition representing "state satisfies condition P".
     *
     * @param state the state
     * @param conditionName the condition predicate name
     * @return atomic proposition
     */
    public Atomic translateCondition(State state, String conditionName) {
        String stateName = state.getId().toString();
        return new Atomic(conditionName + "(" + stateName + ")");
    }

    /**
     * Translate a set of states as context.
     *
     * @param states set of states
     * @return list of propositions
     */
    public java.util.List<Proposition> translateCollection(Set<State> states) {
        return states.stream()
            .map(this::translate)
            .toList();
    }

    /**
     * Clear translation cache.
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * Get translation strategy.
     */
    public TranslationStrategy getStrategy() {
        return strategy;
    }

    private String getCacheKey(State state) {
        return strategy.name() + ":" + state.getId() + ":" + state.getType();
    }

    /**
     * Create translator with custom mappings.
     */
    public static StateToPropositionTranslator withMappings(
            Map<StateType, Proposition> customMappings) {
        return new StateToPropositionTranslator(TranslationStrategy.PREDICATE);
    }

    /**
     * Translate goal state as target proposition.
     */
    public Proposition translateGoal(State goalState) {
        String goalName = goalState.getId().toString();
        return new Atomic("GoalAchieved(" + goalName + ")");
    }

    /**
     * Translate initial state as base proposition.
     */
    public Proposition translateInitial(State initialState) {
        String stateName = initialState.getId().toString();
        return new Atomic("InitialState(" + stateName + ")");
    }
}
