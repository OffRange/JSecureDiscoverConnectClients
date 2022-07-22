package de.offrange.client.models;

import de.offrange.client.udp.endpoint.EndpointAddress;

/**
 * Model class that implements the {@link IModel} interface. This model is only used for the UDP connection
 * between the client and the server.
 * This model is serialized and deserialized by {@link com.google.gson.Gson}.
 */
public class UdpModel {

    private UdpType type;
    private String name;

    private EndpointAddress address;

    /**
     * Constructs a UdpModel instance with a {@link String name}.
     * @param name the name of the software or os
     */
    public UdpModel(String name) {
        this.name = name;
    }

    /**
     * @return {@link UdpType} that indicates if this model is used for a {@link UdpType#REQUEST}
     * or {@link UdpType#RESPONSE}
     */
    public UdpType getType() {
        return type;
    }

    /**
     * @return name specified by the server or client
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name, may be the software name or the os name
     * @param name software or os name or custom string
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return {@link EndpointAddress} that includes the server ip and port
     */
    public EndpointAddress getAddress() {
        return address;
    }

    /**
     * @return true if nothing is null and <pre>{@code getType() == UdpType.RESPONSE}</pre>
     */
    public boolean isValidResponse(){
        return getAddress() != null && getAddress().isValid() && getName() != null && getType() == UdpType.RESPONSE;
    }

    /**
     * Enum to indicate which connection type the model is representing.
     */
    public enum UdpType{
        REQUEST,
        RESPONSE
    }
}
