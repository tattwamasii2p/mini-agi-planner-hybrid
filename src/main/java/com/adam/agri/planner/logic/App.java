package com.adam.agri.planner.logic;

/**
 * Application M N : apply function to argument.
 */
public record App(Term fun, Term arg) implements Term {
    @Override
    public Proposition type() {
        if (fun.type() instanceof Implication imp) {
            return imp.consequent();
        }
        throw new IllegalStateException("Applying non-function");
    }

    @Override
    public boolean isNormal() {
        return false; // redex
    }

    @Override
    public Term substitute(String var, Term replacement) {
        return new App(
            fun.substitute(var, replacement),
            arg.substitute(var, replacement)
        );
    }

    @Override
    public String toString() {
        return "(" + fun + " " + arg + ")";
    }
}