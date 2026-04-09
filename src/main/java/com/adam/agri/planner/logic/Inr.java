package com.adam.agri.planner.logic;

/**
 * Right injection inr : B → A ∨ B.
 */
public record Inr(Term term, Proposition leftType) implements Term {
    @Override
    public Proposition type() {
        return new Disjunction(leftType, term.type());
    }

    @Override
    public boolean isNormal() {
        return term.isNormal();
    }

    @Override
    public Term substitute(String var, Term replacement) {
        return new Inr(term.substitute(var, replacement), leftType);
    }

    @Override
    public String toString() {
        return "inr(" + term + ")";
    }
}