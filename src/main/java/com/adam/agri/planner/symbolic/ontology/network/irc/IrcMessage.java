package com.adam.agri.planner.symbolic.ontology.network.irc;

import com.adam.agri.planner.symbolic.ontology.upper.*;
import com.adam.agri.planner.symbolic.ontology.upper.Process;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * IRC Message - process representing a message event.
 *
 * Layer 7: Process (dynamic, happening in time).
 * Messages have: sender, target, content, timestamp.
 */
public class IrcMessage extends Process {

    private final IrcUser sender;
    private final MessageTarget target;
    private final String content;
    private final Instant timestamp;
    private final String rawLine;
    private final boolean isQuery;
    private Optional<String> command;

    public enum MessageTarget {
        CHANNEL, USER, SERVER, BROADCAST
    }

    public enum MessageType {
        PRIVMSG, NOTICE, JOIN, PART, QUIT, NICK,
        MODE, TOPIC, INVITE, KICK, PING, PONG,
        ERROR, NUMERIC, CAP, AUTHENTICATE, AWAY
    }

    public IrcMessage(EntityId id, Set<Property> properties, Location location,
                      TimeInterval timeInterval, IrcUser sender, MessageTarget target,
                      String content, Instant timestamp) {
        super(id, properties, location, timeInterval);
        this.sender = sender;
        this.target = target;
        this.content = content;
        this.timestamp = timestamp;
        this.rawLine = ":" + sender.getHostmask() + " " + getCommandString(target) + " " + content;
        this.isQuery = target == MessageTarget.USER;
        this.command = Optional.empty();
    }

    private String getCommandString(MessageTarget target) {
        return switch (target) {
            case CHANNEL -> "PRIVMSG";
            case USER -> "NOTICE";
            case SERVER -> "COMMAND";
            case BROADCAST -> "WALLOPS";
        };
    }

    public IrcUser getSender() {
        return sender;
    }

    public MessageTarget getTarget() {
        return target;
    }

    public String getContent() {
        return content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getRawLine() {
        return rawLine;
    }

    public boolean isQuery() {
        return isQuery;
    }

    public boolean isCommand() {
        return content.startsWith("!") || content.startsWith("/");
    }

    public String getCommandName() {
        if (!isCommand()) return "";
        String cmd = content.startsWith("!") ? content.substring(1) : content.substring(1);
        int space = cmd.indexOf(' ');
        return space > 0 ? cmd.substring(0, space).toLowerCase() : cmd.toLowerCase();
    }

    public String getCommandArgs() {
        if (!isCommand()) return "";
        String cmd = content.startsWith("!") ? content.substring(1) : content.substring(1);
        int space = cmd.indexOf(' ');
        return space > 0 ? cmd.substring(space + 1).trim() : "";
    }

    public void setCommand(String cmd) {
        this.command = Optional.of(cmd);
    }

    public Optional<String> getCommand() {
        return command;
    }

    /**
     * Check if message is authentication attempt.
     */
    public boolean isAuthMessage() {
        if (!isCommand()) return false;
        String cmd = getCommandName().toLowerCase();
        return cmd.equals("auth") || cmd.equals("identify") || cmd.equals("login");
    }

    /**
     * Check if message is action (/me).
     */
    public boolean isAction() {
        return content.startsWith("\u0001ACTION ") && content.endsWith("\u0001");
    }

    public String getCleanContent() {
        String clean = content;
        // Remove formatting codes
        clean = clean.replaceAll("\u0003[0-9]{1,2}(,[0-9]{1,2})?", ""); // colors
        clean = clean.replace("\u0002", ""); // bold
        clean = clean.replace("\u001F", ""); // underline
        clean = clean.replace("\u0016", ""); // reverse
        clean = clean.replace("\u000F", ""); // reset
        clean = clean.replace("\u0001", ""); // CTCP
        return clean.trim();
    }

    @Override
    public String toString() {
        return "IrcMessage[" + sender.getNickname() + " -> " + target + ": " +
            (content.length() > 30 ? content.substring(0, 30) + "..." : content) + "]";
    }
}
