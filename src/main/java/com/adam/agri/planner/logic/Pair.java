package com.adam.agri.planner.logic;

/**
 * Pair constructor (A, B) : A ∧ B.
 */
public record Pair(Term left, Term right, Proposition leftType, Proposition rightType) implements Term {
    @Override
    public Proposition type() {
        return new Conjunction(leftType, rightType);
    }

    @Override
    public boolean isNormal() {
        return left.isNormal() && right.isNormal();
    }

    @Override
    public Term substitute(String var, Term replacement) {
        return new Pair(
            left.substitute(var, replacement),
            right.substitute(var, replacement),
            leftType,
            rightType
        );
    }

    @Override
    public String toString() {
        return "⟨" + left + ", " + right + "⟩";
    }
}