package com.adam.agri.planner.multiagent;

/**
 * Unique identifier for agents.
 */
public final class AgentId {
    private final String value;
    private static int counter = 0;

    public AgentId(String value) {
        this.value = value;
    }

    public static AgentId generate() {
        return new AgentId("agent_" + (++counter));
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentId agentId = (AgentId) o;
        return value.equals(agentId.value);
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
