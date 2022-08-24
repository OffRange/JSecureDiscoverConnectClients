package de.offrange.client.udp.endpoint;

/**
 * Class that contains information about an endpoint.
 * This class is serialized and deserialized by {@link com.google.gson.Gson}
 */
public class EndpointAddress {

    private String ip;
    private int port;

    /**
     * Constructs a new EndpointAddress instance with an ip address and a port.
     * @param ip the endpoint's ip address.
     * @param port the endpoint's port.
     */
    public EndpointAddress(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    /**
     * Empty constructor used by the {@link com.google.gson.Gson} framework to serialize this class
     */
    public EndpointAddress() {}

    /**
     * @return the ip address of the endpoint.
     */
    public String getIp() {
        return ip;
    }

    /**
     * @return the port of the endpoint.
     */
    public int getPort() {
        return port;
    }

    /**
     * @return true if ip address and port are not null
     */
    public boolean isValid(){
        return ip != null & port != 0;
    }
}
