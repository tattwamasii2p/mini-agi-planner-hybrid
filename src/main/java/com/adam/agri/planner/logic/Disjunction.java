package com.adam.agri.planner.logic;

import com.adam.agri.planner.logic.Proposition.PropositionType;

/**
 * Disjunction A ∨ B : sum type.
 */
public record Disjunction(Proposition left, Proposition right) implements Proposition {
    @Override
    public boolean isProvable() {
        return left.isProvable() || right.isProvable();
    }

    @Override
    public boolean isLocallyTrue(Context ctx) {
        return left.isLocallyTrue(ctx) || right.isLocallyTrue(ctx);
    }

    @Override
    public PropositionType getType() {
        return PropositionType.DISJUNCTION;
    }

    @Override
    public String toString() {
        return "(" + left + " ∨ " + right + ")";
    }
}