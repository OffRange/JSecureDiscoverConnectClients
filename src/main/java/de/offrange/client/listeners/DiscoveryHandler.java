package de.offrange.client.listeners;

import de.offrange.client.udp.DiscoveredEndpoint;
import de.offrange.client.udp.endpoint.DiscoveryClient;

import java.util.List;

/**
 * Interface used to handle specific events called by the {@link DiscoveryClient}.
 */
public interface DiscoveryHandler {

    /**
     * Is called when the {@link DiscoveryClient} discovered a local server.
     * @param discoveredEndpoint the {@link DiscoveredEndpoint} representing the server. It contains the name and the
     * {@link de.offrange.client.udp.endpoint.EndpointAddress} containing the ip address and the port of the server.
     */
    void onDiscovered(DiscoveredEndpoint discoveredEndpoint);

    /**
     * Called when the {@link DiscoveryClient} has finished discovering a server on the local network.
     * @param discoveredEndpoints represents a list containing all {@link DiscoveredEndpoint}s the client
     * could find on the local network.
     */
    void onFinish(List<DiscoveredEndpoint> discoveredEndpoints);
}
