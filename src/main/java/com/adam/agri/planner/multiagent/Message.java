package com.adam.agri.planner.multiagent;

/**
 * Message between agents.
 */
public interface Message {
    AgentId getSender();
    AgentId getReceiver();
    String getType();
    Object getPayload();
}
