package com.adam.agri.planner.logic;

/**
 * Variable reference.
 */
public record Var(String name, Proposition type) implements Term {
    @Override
    public boolean isNormal() {
        return true;
    }

    @Override
    public Term substitute(String var, Term replacement) {
        if (name.equals(var)) {
            return replacement;
        }
        return this;
    }

    @Override
    public String toString() {
        return name;
    }
}