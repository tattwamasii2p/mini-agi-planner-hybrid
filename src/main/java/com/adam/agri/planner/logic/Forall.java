package com.adam.agri.planner.logic;

import com.adam.agri.planner.logic.Proposition.PropositionType;

/**
 * Universal quantification ∀.
 */
public record Forall(String variable, Proposition body) implements Proposition {
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
        return PropositionType.FORALL;
    }

    @Override
    public String toString() {
        return "∀" + variable + ". " + body;
    }
}