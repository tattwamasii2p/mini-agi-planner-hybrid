package com.adam.agri.planner.multiagent;

import java.util.List;

/**
 * Communication channel between agents.
 */
public interface CommunicationChannel {
    void send(AgentId to, Message message);
    List<Message> receive();
}
