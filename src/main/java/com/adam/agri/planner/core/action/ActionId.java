package com.adam.agri.planner.core.action;

/**
 * Unique identifier for actions.
 */
public final class ActionId {
    private final String value;
    private static int counter = 0;

    public ActionId(String value) {
        this.value = value;
    }

    public static ActionId generate() {
        return new ActionId("a_" + (++counter));
    }

    public static ActionId of(String value) {
        return new ActionId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionId actionId = (ActionId) o;
        return value.equals(actionId.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
