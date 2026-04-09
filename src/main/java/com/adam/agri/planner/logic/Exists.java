package com.adam.agri.planner.logic;

import com.adam.agri.planner.logic.Proposition.PropositionType;

/**
 * Existential quantification ∃.
 */
public record Exists(String variable, Proposition body) implements Proposition {
    @Override
    public boolean isProvable() {
        return false; // need witness
    }

    @Override
    public boolean isLocallyTrue(Context ctx) {
        return body.isLocallyTrue(ctx);
    }

    @Override
    public PropositionType getType() {
        return PropositionType.EXISTS;
    }

    @Override
    public String toString() {
        return "∃" + variable + ". " + body;
    }
}