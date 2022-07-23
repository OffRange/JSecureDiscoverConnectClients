package de.offrange.client.listeners;

import de.offrange.client.models.IModel;
import de.offrange.client.tcp.TcpClient;

/**
 * Interface used to process received data from the server. It should be called by the
 * {@link de.offrange.client.tcp.TcpClient}.
 */
public interface ReceiveHandler<T extends IModel> {

    /**
     * Called when data is received from the server.
     * @param model an {@link IModel} representing the JSON data from the server.
     */
    void onDataReceived(T model);

    /**
     * Called when the code evaluation is received.
     * @param correct true if the entered code was correct, false otherwise. If the evaluation is {@code true},
     * the connection will be enabled and requests and responses are allowed.
     */
    void onCodeEvaluationReceived(boolean correct, TcpClient<T> tcpClient);
}
