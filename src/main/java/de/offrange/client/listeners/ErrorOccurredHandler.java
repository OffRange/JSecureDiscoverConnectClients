package de.offrange.client.listeners;

import de.offrange.client.Client;
import de.offrange.client.udp.DiscoveryClient;

/**
 * Interface used to catch all kinds of errors and exceptions encountered on the
 * {@link DiscoveryClient} and/or {@link de.offrange.client.tcp.TcpClient} side.
 * @see #onErrorOccurred(Client, Exception, Type)
 */
public interface ErrorOccurredHandler {

    /**
     * Called if any error or exception occurs.
     * @param client the client sender sending the exception
     * @param exception the exception that is thrown.
     * @param type represents the state in which the error/exception occurs.
     * @see Type
     */
    void onErrorOccurred(Client client, Exception exception, Type type);

    /**
     * Enum representing the state in which the error/exception occurred.
     */
    enum Type{
        /**
         * Error occurred while disconnecting.
         */
        DISCONNECT,

        /**
         * Error occurred while handshaking.
         */
        HANDSHAKE,

        /**
         * Error occurred while sending data to the server.
         */
        SEND,

        /**
         * Error occurred while receiving data from the server.
         */
        RECEIVE,


        /**
         * Error occurred while canceling the {@link DiscoveryClient}.
         */
        UDP_CANCEL,

        /**
         * Error occurred while discovering an endpoint.
         */
        UDP_DISCOVERING
    }
}
