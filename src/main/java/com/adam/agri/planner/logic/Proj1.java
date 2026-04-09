package com.adam.agri.planner.logic;

/**
 * First projection π₁.
 */
public record Proj1(Term pair, Proposition leftType) implements Term {
    @Override
    public Proposition type() {
        return leftType;
    }

    @Override
    public boolean isNormal() {
        return false; // redex if pair is pair
    }

    @Override
    public Term substitute(String var, Term replacement) {
        return new Proj1(pair.substitute(var, replacement), leftType);
    }

    @Override
    public String toString() {
        return "π₁(" + pair + ")";
    }
}