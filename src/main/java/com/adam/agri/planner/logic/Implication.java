package com.adam.agri.planner.logic;

import com.adam.agri.planner.logic.Proposition.PropositionType;

/**
 * Implication A → B : functional type.
 */
public record Implication(Proposition antecedent, Proposition consequent) implements Proposition {
    @Override
    public boolean isProvable() {
        // Implication is provable if consequent is provable given antecedent
        return false; // requires actual proof term
    }

    @Override
    public boolean isLocallyTrue(Context ctx) {
        Context extended = ctx.extend(antecedent);
        return consequent.isLocallyTrue(extended);
    }

    @Override
    public PropositionType getType() {
        return PropositionType.IMPLICATION;
    }

    @Override
    public String toString() {
        return "(" + antecedent + " → " + consequent + ")";
    }
}