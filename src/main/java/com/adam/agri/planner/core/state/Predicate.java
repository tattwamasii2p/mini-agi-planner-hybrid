package com.adam.agri.planner.core.state;

import java.util.List;

/**
 * Simple predicate representation for symbolic states.
 */
public record Predicate(String name, List<Object> args) {
    public Predicate(String name) {
        this(name, List.of());
    }

    @Override
    public String toString() {
        return name + args;
    }
}
