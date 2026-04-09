package com.adam.agri.planner.symbolic.ontology.upper;

/**
 * Entity identifier for ontology.
 */
public final class EntityId {
    private static int counter = 0;
    private final String value;

    public EntityId(String value) {
        this.value = value;
    }

    public static EntityId generate() {
        return new EntityId("e_" + (++counter));
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityId entityId = (EntityId) o;
        return value.equals(entityId.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }

	public static EntityId of(String string) {
		return new EntityId(string);
	}
}
