package com.adam.agri.planner.symbolic.ontology.network.irc;

import com.adam.agri.planner.symbolic.ontology.upper.*;

import java.util.HashSet;
import java.util.Set;

/**
 * IRC Server - physical entity representing an IRC server in the network.
 *
 * Extends Physical as servers have:
 * - Location (IP address, data center)
 * - Time interval (online duration)
 * - Network properties (latency, bandwidth)
 *
 * Layer 2-4: Physical representation of distributed communication infrastructure.
 */
public class IrcServer extends Physical {

    private final String hostname;
    private final int port;
    private final String network;
    private final double uplinkCapacity;      // Mbps
    private final boolean supportsTls;
    private final Set<String> capabilities;   // Extended capabilities (SASL, CAP, etc.)
    private ServerStatus status;

    public enum ServerStatus {
        CONNECTED, DISCONNECTED, CONNECTING, ERROR
    }

    public IrcServer(EntityId id, Set<Property> properties, Location location,
                     TimeInterval timeInterval, String hostname, int port,
                     String network, double uplinkCapacity, boolean supportsTls) {
        super(id, properties, location, timeInterval);
        this.hostname = hostname;
        this.port = port;
        this.network = network;
        this.uplinkCapacity = uplinkCapacity;
        this.supportsTls = supportsTls;
        this.capabilities = new HashSet<>();
        this.status = ServerStatus.DISCONNECTED;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public String getNetwork() {
        return network;
    }

    public double getUplinkCapacity() {
        return uplinkCapacity;
    }

    public boolean supportsTls() {
        return supportsTls;
    }

    public Set<String> getCapabilities() {
        return Set.copyOf(capabilities);
    }

    public void addCapability(String capability) {
        capabilities.add(capability);
    }

    public ServerStatus getStatus() {
        return status;
    }

    public void setStatus(ServerStatus status) {
        this.status = status;
    }

    public boolean isConnected() {
        return status == ServerStatus.CONNECTED;
    }

    /**
     * Check if server has required capability.
     */
    public boolean hasCapability(String capability) {
        return capabilities.contains(capability);
    }

    /**
     * Get connection string.
     */
    public String getConnectionString() {
        return (supportsTls ? "ircs://" : "irc://") + hostname + ":" + port;
    }

    @Override
    public String toString() {
        return "IrcServer[" + hostname + ":" + port + " " + status + "]";
    }
}
