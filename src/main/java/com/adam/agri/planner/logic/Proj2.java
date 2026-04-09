package com.adam.agri.planner.logic;

/**
 * Second projection π₂.
 */
public record Proj2(Term pair, Proposition rightType) implements Term {
    @Override
    public Proposition type() {
        return rightType;
    }

    @Override
    public boolean isNormal() {
        return false;
    }

    @Override
    public Term substitute(String var, Term replacement) {
        return new Proj2(pair.substitute(var, replacement), rightType);
    }

    @Override
    public String toString() {
        return "π₂(" + pair + ")";
    }
}