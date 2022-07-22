package de.offrange.client.udp;

import de.offrange.client.udp.endpoint.EndpointAddress;

/**
 * Class that represent a discovered endpoint.
 */
public class DiscoveredEndpoint {

    private String name;
    private EndpointAddress address;

    /**
     * Constructs a new DiscoveredEndpoint instance with a name and the address of the endpoint.
     * @param name name of the endpoint.
     * @param address endpoint address information.
     */
    public DiscoveredEndpoint(String name, EndpointAddress address) {
        this.name = name;
        this.address = address;
    }

    /**
     * @return the name of this discovered endpoint.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the endpoint address information.
     */
    public EndpointAddress getAddress() {
        return address;
    }
}
