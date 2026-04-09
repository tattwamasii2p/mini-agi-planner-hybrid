package com.adam.agri.planner.logic;

/**
 * Unit term (proof of ⊤).
 */
public record Unit() implements Term {
    @Override
    public Proposition type() {
        return new Atomic("⊤");
    }

    @Override
    public boolean isNormal() {
        return true;
    }

    @Override
    public Term substitute(String var, Term replacement) {
        return this;
    }

    @Override
    public String toString() {
        return "()";
    }
}