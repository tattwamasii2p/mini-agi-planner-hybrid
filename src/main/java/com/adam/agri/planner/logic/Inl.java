package com.adam.agri.planner.logic;

/**
 * Left injection inl : A → A ∨ B.
 */
public record Inl(Term term, Proposition rightType) implements Term {
    @Override
    public Proposition type() {
        return new Disjunction(term.type(), rightType);
    }

    @Override
    public boolean isNormal() {
        return term.isNormal();
    }

    @Override
    public Term substitute(String var, Term replacement) {
        return new Inl(term.substitute(var, replacement), rightType);
    }

    @Override
    public String toString() {
        return "inl(" + term + ")";
    }
}