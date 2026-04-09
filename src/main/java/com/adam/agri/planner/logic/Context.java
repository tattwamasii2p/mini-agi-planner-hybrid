package com.adam.agri.planner.logic;

import com.adam.agri.planner.sheaf.Sheaf;

/**
 * Context for local truth evaluation (Layer 10 sheaf semantics).
 */
public record Context(java.util.List<Proposition> facts, Sheaf<Context> sheaf) {
    public static Context empty() {
        return new Context(java.util.List.of(), (Sheaf<Context>)null);
    }

    public Context extend(Proposition p) {
        java.util.List<Proposition> extended = new java.util.ArrayList<>(facts);
        extended.add(p);
        return new Context(extended, sheaf);
    }
    
    public java.util.List<Proposition> facts() {return facts;}
}