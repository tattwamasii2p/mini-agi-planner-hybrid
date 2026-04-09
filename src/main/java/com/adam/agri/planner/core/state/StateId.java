package com.adam.agri.planner.core.state;

/**
 * Unique identifier for states.
 */
public final class StateId {
    private final String value;
    private static int counter = 0;

    public StateId(String value) {
        this.value = value;
    }

    public static StateId generate() {
        return new StateId("s_" + (++counter));
    }

    public static StateId of(String value) {
        return new StateId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StateId stateId = (StateId) o;
        return value.equals(stateId.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "StateId{" + value + '}';
    }
}
