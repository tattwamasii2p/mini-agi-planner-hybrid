package com.adam.agri.planner.symbolic.ontology.network.irc;

import com.adam.agri.planner.symbolic.ontology.upper.*;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * IRC User - entity participating in IRC network.
 *
 * Layer 4-5: Users can be physical (real user) or abstract (nickname).
 */
public class IrcUser extends Abstract {

    private final String nickname;
    private Optional<String> username;
    private Optional<String> hostname;
    private Optional<String> realName;
    private final Set<IrcServer> connectedServers;
    private final Set<IrcChannel> joinedChannels;
    private Instant signonTime;
    private UserStatus status;
    private boolean isAuthenticated;
    private Optional<String> account;

    public enum UserStatus {
        ONLINE, AWAY, OFFLINE
    }

    public IrcUser(EntityId id, Set<Property> properties, String nickname) {
        super(id, properties);
        this.nickname = nickname;
        this.username = Optional.empty();
        this.hostname = Optional.empty();
        this.realName = Optional.empty();
        this.connectedServers = new HashSet<>();
        this.joinedChannels = new HashSet<>();
        this.status = UserStatus.OFFLINE;
        this.isAuthenticated = false;
        this.account = Optional.empty();
    }

    public String getNickname() {
        return nickname;
    }

    public Optional<String> getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = Optional.of(username);
    }

    public Optional<String> getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = Optional.of(hostname);
    }

    public Optional<String> getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = Optional.of(realName);
    }

    public String getHostmask() {
        return nickname + "!" + username.orElse("*") + "@" + hostname.orElse("*");
    }

    public Set<IrcServer> getConnectedServers() {
        return Collections.unmodifiableSet(connectedServers);
    }

    public void connect(IrcServer server) {
        connectedServers.add(server);
        if (status == UserStatus.OFFLINE) {
            status = UserStatus.ONLINE;
            signonTime = Instant.now();
        }
    }

    public void disconnect(IrcServer server) {
        connectedServers.remove(server);
        if (connectedServers.isEmpty()) {
            status = UserStatus.OFFLINE;
        }
    }

    public Set<IrcChannel> getJoinedChannels() {
        return Collections.unmodifiableSet(joinedChannels);
    }

    public void joinChannel(IrcChannel channel) {
        joinedChannels.add(channel);
        channel.addMember(this);
    }

    public void leaveChannel(IrcChannel channel) {
        joinedChannels.remove(channel);
        channel.removeMember(this);
    }

    public boolean isInChannel(IrcChannel channel) {
        return joinedChannels.contains(channel);
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public boolean isAway() {
        return status == UserStatus.AWAY;
    }

    public boolean isOnline() {
        return status == UserStatus.ONLINE;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        isAuthenticated = authenticated;
    }

    public Optional<String> getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = Optional.of(account);
    }

    public Instant getSignonTime() {
        return signonTime;
    }

    public void setSignonTime(Instant signonTime) {
        this.signonTime = signonTime;
    }

    /**
     * Check if user ident matches pattern.
     */
    public boolean matchesIdent(String ident) {
        return username.map(u -> u.equalsIgnoreCase(ident)).orElse(false);
    }

    @Override
    public String toString() {
        return "IrcUser[" + getHostmask() + "]";
    }
}
