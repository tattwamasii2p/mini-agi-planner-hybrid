package com.adam.agri.planner.logic;

import com.adam.agri.planner.logic.Proposition.PropositionType;

/**
 * Conjunction A ∧ B : product type.
 */
public record Conjunction(Proposition left, Proposition right) implements Proposition {
    @Override
    public boolean isProvable() {
        return left.isProvable() && right.isProvable();
    }

    @Override
    public boolean isLocallyTrue(Context ctx) {
        return left.isLocallyTrue(ctx) && right.isLocallyTrue(ctx);
    }

    @Override
    public PropositionType getType() {
        return PropositionType.CONJUNCTION;
    }

    @Override
    public String toString() {
        return "(" + left + " ∧ " + right + ")";
    }
}