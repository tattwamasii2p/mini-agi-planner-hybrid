package com.adam.agri.planner.logic;

import com.adam.agri.planner.logic.Proposition.PropositionType;

/**
 * Modal operators □A and ◇A (Layer 9).
 */
public record Modal(Proposition inner, Modality mode) implements Proposition {
    public enum Modality {
        NECESSARY,   // □ : true in all worlds (formal proof)
        POSSIBLE,    // ◇ : true in some world (LLM belief)
        BELIEF       // Belief with confidence
    }

    @Override
    public boolean isProvable() {
        return switch (mode) {
            case NECESSARY -> inner.isProvable(); // □A requires proof in all worlds
            case POSSIBLE -> true; // ◇A is true if some world is possible
            case BELIEF -> inner.isProvable(); // but with lower confidence
        };
    }

    @Override
    public boolean isLocallyTrue(Context ctx) {
        return switch (mode) {
            case NECESSARY -> inner.isLocallyTrue(ctx);
            case POSSIBLE -> true; // simplified
            case BELIEF -> inner.isLocallyTrue(ctx);
        };
    }

    @Override
    public PropositionType getType() {
        return PropositionType.MODAL;
    }

    @Override
    public String toString() {
        return switch (mode) {
            case NECESSARY -> "□" + inner;
            case POSSIBLE -> "◇" + inner;
            case BELIEF -> "B" + inner;
        };
    }
}