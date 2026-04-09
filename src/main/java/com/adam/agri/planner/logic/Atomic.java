package com.adam.agri.planner.logic;

import com.adam.agri.planner.logic.Proposition.PropositionType;

/**
 * Atomic proposition (primitive fact).
 */
public record Atomic(String name) implements Proposition {
    @Override
    public boolean isProvable() {
        return false; // atomic facts need evidence
    }

    @Override
    public boolean isLocallyTrue(Context ctx) {
        return ctx.facts().contains(this);
    }

    @Override
    public PropositionType getType() {
        return PropositionType.ATOMIC;
    }

    @Override
    public String toString() {
        return name;
    }
}