package com.adam.agri.planner.logic;

/**
 * Case analysis for disjunction elimination.
 */
public record Case(Term disjunction, String leftVar, Term leftCase,
              String rightVar, Term rightCase, Proposition resultType) implements Term {
    @Override
    public Proposition type() {
        return resultType;
    }

    @Override
    public boolean isNormal() {
        return false;
    }

    @Override
    public Term substitute(String var, Term replacement) {
        return new Case(
            disjunction.substitute(var, replacement),
            leftVar,
            leftVar.equals(var) ? leftCase : leftCase.substitute(var, replacement),
            rightVar,
            rightVar.equals(var) ? rightCase : rightCase.substitute(var, replacement),
            resultType
        );
    }

    @Override
    public String toString() {
        return "case " + disjunction + " of { " +
            leftVar + " → " + leftCase + " | " +
            rightVar + " → " + rightCase + " }";
    }
}