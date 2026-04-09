package com.adam.agri.planner.logic;

/**
 * Term witnesses (Curry-Howard isomorphism).
 *
 * Proof term = Program that inhabits the type.
 * Type = Proposition being proved.
 */
public sealed interface Term permits Var, Lam, App, Pair, Proj1, Proj2, Inl, Inr, Case, Unit {

    /**
     * Get the type of this term = proposition it proves.
     */
    Proposition type();

    /**
     * Check if term is in normal form.
     */
    boolean isNormal();

    /**
     * Substitute variable with term.
     */
    Term substitute(String var, Term replacement);
}