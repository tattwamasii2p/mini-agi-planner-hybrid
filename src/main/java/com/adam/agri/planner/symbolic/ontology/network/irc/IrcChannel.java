package com.adam.agri.planner.symbolic.ontology.network.irc;

import com.adam.agri.planner.symbolic.ontology.upper.*;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * IRC Channel - abstract communication space in IRC network.
 *
 * Layer 5: Abstract entity representing a channel.
 * Channels have topics, modes, and member lists - all abstract properties.
 */
public class IrcChannel extends Abstract {

    private final String name;
    private final IrcServer server;
    private Optional<String> topic;
    private final Set<IrcUser> members;
    private final Map<Character, String> modes;
    private final Set<String> banList;
    private final Map<String, Set<Character>> userModes;
    private final Instant creationTime;

    public IrcChannel(EntityId id, Set<Property> properties,
                       String name, IrcServer server) {
        super(id, properties);
        this.name = name;
        this.server = server;
        this.topic = Optional.empty();
        this.members = new HashSet<>();
        this.modes = new HashMap<>();
        this.banList = new HashSet<>();
        this.userModes = new HashMap<>();
        this.creationTime = Instant.now();
    }

    public String getName() {
        return name;
    }

    public IrcServer getServer() {
        return server;
    }

    public Optional<String> getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = Optional.of(topic);
    }

    public Set<IrcUser> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public void addMember(IrcUser user) {
        members.add(user);
        userModes.putIfAbsent(user.getNickname(), new HashSet<>());
    }

    public void removeMember(IrcUser user) {
        members.remove(user);
        userModes.remove(user.getNickname());
    }

    public boolean hasMember(IrcUser user) {
        return members.contains(user);
    }

    public int getMemberCount() {
        return members.size();
    }

    public Map<Character, String> getModes() {
        return Collections.unmodifiableMap(modes);
    }

    public void setMode(char mode, String parameter) {
        modes.put(mode, parameter != null ? parameter : "");
    }

    public void removeMode(char mode) {
        modes.remove(mode);
    }

    public boolean hasMode(char mode) {
        return modes.containsKey(mode);
    }

    public Set<String> getBanList() {
        return Collections.unmodifiableSet(banList);
    }

    public void addBan(String mask) {
        banList.add(mask);
    }

    public void removeBan(String mask) {
        banList.remove(mask);
    }

    public boolean isBanned(String mask) {
        return banList.stream().anyMatch(pattern -> matchesMask(mask, pattern));
    }

    public Set<Character> getUserModes(IrcUser user) {
        return userModes.getOrDefault(user.getNickname(), Set.of());
    }

    public void setUserMode(IrcUser user, char mode) {
        userModes.computeIfAbsent(user.getNickname(), k -> new HashSet<>()).add(mode);
    }

    public void removeUserMode(IrcUser user, char mode) {
        userModes.computeIfAbsent(user.getNickname(), k -> new HashSet<>()).remove(mode);
    }

    public boolean hasUserMode(IrcUser user, char mode) {
        return getUserModes(user).contains(mode);
    }

    public boolean isOperator(IrcUser user) {
        return userModes.getOrDefault(user.getNickname(), Set.of()).contains('@');
    }

    public boolean isHalfOperator(IrcUser user) {
        return userModes.getOrDefault(user.getNickname(), Set.of()).contains('h');
    }

    public boolean isVoiced(IrcUser user) {
        return userModes.getOrDefault(user.getNickname(), Set.of()).contains('+');
    }

    public Instant getCreationTime() {
        return creationTime;
    }

    /**
     * Check if user can send messages (not muted).
     */
    public boolean canSpeak(IrcUser user) {
        if (isOperator(user) || isHalfOperator(user) || isVoiced(user)) {
            return true;
        }
        if (hasMode('m')) { // moderated
            return false;
        }
        return true;
    }

    /**
     * Check if channel is invite-only.
     */
    public boolean isInviteOnly() {
        return hasMode('i');
    }

    /**
     * Check if channel has password.
     */
    public boolean hasPassword() {
        return hasMode('k');
    }

    public Optional<String> getPassword() {
        return Optional.ofNullable(modes.get('k'));
    }

    private boolean matchesMask(String mask, String pattern) {
        // Simple mask matching (irc-style)
        String regex = pattern.replace("*", ".*").replace("?", ".");
        return mask.matches(regex);
    }

    @Override
    public String toString() {
        return "IrcChannel[" + name + "@" + server.getHostname() + "]";
    }
}
