package com.adam.agri.planner.logic;

/**
 * Lambda abstraction λx.M : A → B.
 */
public record Lam(String var, Term body, Proposition argType, Proposition resType) implements Term {
    @Override
    public Proposition type() {
        return new Implication(argType, resType);
    }

    @Override
    public boolean isNormal() {
        return body.isNormal();
    }

    @Override
    public Term substitute(String v, Term replacement) {
        if (!var.equals(v)) {
            return new Lam(var, body.substitute(v, replacement), argType, resType);
        }
        return this;
    }

    @Override
    public String toString() {
        return "λ" + var + "." + body;
    }
}