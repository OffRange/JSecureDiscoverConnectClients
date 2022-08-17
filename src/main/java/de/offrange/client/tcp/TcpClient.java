package de.offrange.client.tcp;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.offrange.client.Client;
import de.offrange.client.RsaAesCryptography;
import de.offrange.client.gson.ByteArrayTypeAdapter;
import de.offrange.client.listeners.ErrorOccurredHandler;
import de.offrange.client.listeners.ReceiveHandler;
import de.offrange.client.models.HandshakeModel;
import de.offrange.client.models.IModel;
import de.offrange.client.models.CodeCheckModel;
import de.offrange.client.udp.endpoint.EndpointAddress;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * This class represents a basic tcp client that encrypts the connection to the server.
 * This is achieved by performing a handshake before you can send any data to the server.
 * The connection is secured by an AES key generated on the client side and encrypted with an RSA public key
 * generated on the server side. The AES key is then sent encrypted to the server.
 * The server have to generate a code that you pass into {@link #sendCode(String)} to enable the connection.
 * Before that, the server and the client cannot communicate together.
 * @param <T> the model that represents the server requests and responses.
 */
public class TcpClient<T extends IModel> implements Client {

    private static TcpClient<? extends IModel> instance;

    private final Gson gson;

    private final Socket client;
    private final InetSocketAddress address;
    private SecretKey aesKey;

    private boolean running;
    private boolean handshakeDone;
    private boolean codeChecked;

    private ReceiveHandler<T> receiveHandler;
    private ErrorOccurredHandler errorOccurredHandler;

    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    private Thread waiterThread;

    private final Class<T> modelClass;

    /**
     * Constructs a TcpClient instance with a {@link EndpointAddress} and a class that is used to
     * receive data from the server.It indicates a model that comes from the server as a response.
     * The class must implement the {@link IModel} interface.
     * @param endpointAddress the endpoint information containing hostname and port
     * @param modelClass the model class that specifies the JSON data from the server.
     */
    public TcpClient(EndpointAddress endpointAddress, Class<T> modelClass) {
        this(endpointAddress.getIp(), endpointAddress.getPort(), modelClass);
    }

    /**
     * Constructs a TcpClient instance with a host, port and a class that is used to
     * receive data from the server. It indicates a model that comes from the server as a response.
     * The class must implement the {@link IModel} interface.
     * @param host the hostname or ip address of the server to which the client should connect.
     * @param port the port on which the server listens.
     * @param modelClass the model class that specifies the JSON data from the server.
     */
    public TcpClient(String host, int port, Class<T> modelClass){
        instance = this;
        gson = new GsonBuilder()
                .disableHtmlEscaping()
                .registerTypeAdapter(byte[].class, new ByteArrayTypeAdapter())
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();

        this.modelClass = modelClass;

        client = new Socket();
        this.address = new InetSocketAddress(host, port);

        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            aesKey = keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException ignored) {}
    }

    /**
     * @return the currently running instance. Will be {@code null} if no instance is running,
     * initiated or {@link #disconnect()} was called previous instance.
     * @param <T> the JSON model class that is used to send.
     */
    @SuppressWarnings("unchecked")
    public static <T extends IModel> TcpClient<T> getInstance() {
        return (TcpClient<T>) instance;
    }

    /**
     * Use {@link #setReceiveHandler(ReceiveHandler)} to set a {@code ReceiveHandler}.
     * @return {@link ReceiveHandler<T>} that is used to handle receive events.
     * @see #setReceiveHandler(ReceiveHandler)
     */
    public ReceiveHandler<T> getReceiveHandler() {
        return receiveHandler;
    }

    /**
     * Sets the {@link ReceiveHandler} that can handle events such as {@link ReceiveHandler#onDataReceived(IModel)}
     * and {@link ReceiveHandler#onCodeEvaluationReceived(boolean, TcpClient)}.
     * @param receiveHandler the {@code ReceiveHandler} that will handle the events.
     * @see #getReceiveHandler()
     */
    public void setReceiveHandler(ReceiveHandler<T> receiveHandler) {
        this.receiveHandler = receiveHandler;
    }

    /**
     * Use {@link #setErrorOccurredHandler(ErrorOccurredHandler)} to set a {@code ErrorOccurredHandler}.
     * @return {@link ErrorOccurredHandler} that is used to handle every type of exception the client will throw.
     * @see #setErrorOccurredHandler(ErrorOccurredHandler) 
     */
    public ErrorOccurredHandler getErrorOccurredHandler() {
        return errorOccurredHandler;
    }

    /**
     * Sets the {@link ErrorOccurredHandler} that can handle every kind ov exception the client will ever throw.
     * @param errorOccurredHandler the {@code ErrorOccurredHandler} that will handle the exceptions.
     * @see #getErrorOccurredHandler()
     */
    public void setErrorOccurredHandler(ErrorOccurredHandler errorOccurredHandler) {
        this.errorOccurredHandler = errorOccurredHandler;
    }

    /**
     * Calls the {@code Error Occurred Handler} if one is set.
     * Use different {@code type}s for different states.
     * @param e the {@code Exception} that is thrown.
     * @param type the error type.
     * @see de.offrange.client.listeners.ErrorOccurredHandler.Type
     */
    private void callErrorOccurred(Exception e, ErrorOccurredHandler.Type type){
        if(errorOccurredHandler == null)
            return;

        errorOccurredHandler.onErrorOccurred(this, e, type);
    }

    /**
     * Start the client and connect it to the server specified in the constructor.
     * @throws IOException if an error occurs during the connection.
     * @see #disconnect()
     */
    public void startAndConnect() throws IOException {
        client.connect(address, 5000);

        ClientHandler handler = new ClientHandler();
        handler.start();
    }

    /**
     * Disconnect and stop the client. If an I/O error occurs when closing the socket, it will call
     * {@link de.offrange.client.listeners.ErrorOccurredHandler#onErrorOccurred(Client, Exception, ErrorOccurredHandler.Type)}.
     * When such an error occurs, {@link ErrorOccurredHandler.Type} will be {@link ErrorOccurredHandler.Type#DISCONNECT}.
     * @see #startAndConnect()
     */
    public void disconnect(){
        instance = null;
        if(!isRunning())
            return;

        running = false;
        try {
            client.close();
        } catch (IOException e) {
            callErrorOccurred(e, ErrorOccurredHandler.Type.DISCONNECT);
        }
    }

    /**
     * Starts the handshake with the server to secure the connection. To do this, it obtains an RSA public key from
     * the server and generates an AES key, which is encrypted using the RSA public key, and then sends the
     * encrypted AES key back to the server.
     * <br>
     * {@link de.offrange.client.listeners.ErrorOccurredHandler#onErrorOccurred(Client, Exception, ErrorOccurredHandler.Type)}
     * is called if the server does not send RSA key information.
     * {@link ErrorOccurredHandler.Type} will be {@link ErrorOccurredHandler.Type#HANDSHAKE}.
     * @throws IOException the stream has been closed and the contained input stream does not support reading
     *                     after close, or another I/O error occurs.
     */
    private void doHandshake() throws IOException {
        if(handshakeDone)
            return;

        HandshakeModel receivedModel = gson.fromJson(new String(readFully()), HandshakeModel.class);
        if(receivedModel.getRsaKeyInformation() == null){
            callErrorOccurred(new NullPointerException("received no rsa key information from the server while handshaking"), ErrorOccurredHandler.Type.HANDSHAKE);
            return;
        }

        HandshakeModel sendModel = new HandshakeModel();
        sendModel.setAesKey(aesKey.getEncoded());

        send(sendModel, receivedModel.getRsaKeyInformation().toPublicKey());

        handshakeDone = true;
    }

    /**
     * @return true if the client is running, false otherwise.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * @return true if the handshake was performed and successful, false otherwise.
     */
    public boolean isHandshakeDone() {
        return handshakeDone;
    }

    /**
     * @return true if the code has been reviewed and is correct, false otherwise.
     */
    public boolean isCodeChecked() {
        return codeChecked;
    }

    /**
     * Read raw bytes from the server.
     * @return {@code byte[]} that represents the received data. After the handshake, this byte array will be encrypted.
     * @throws IOException the stream has been closed and the contained input stream does not support reading after
     * close, or another I/O error occurs.
     */
    private byte[] readFully() throws IOException {
        int length = inputStream.readInt();
        byte[] received = new byte[length];
        inputStream.readFully(received);
        return received;
    }

    /**
     * Sends a JSON model that implements the {@link IModel} interface to the server. The parsed model will be
     * encrypted with an AES key. In order to be able to send data you must enable the connection
     * by calling {@link #sendCode(String)}.
     * <br>
     * {@link de.offrange.client.listeners.ErrorOccurredHandler#onErrorOccurred(Client, Exception, ErrorOccurredHandler.Type)}
     * is called if the handshake is not yet complete or the connection is not enabled.
     * {@link ErrorOccurredHandler.Type} will be {@link ErrorOccurredHandler.Type#SEND}.
     * @param model JSON model to send.
     */
    public void send(T model){
        if(!isHandshakeDone()){
            callErrorOccurred(new IllegalStateException("attempting to send data, but the connection has not yet completed a handshake"), ErrorOccurredHandler.Type.SEND);
            return;
        }

        if(!codeChecked && !((model instanceof HandshakeModel) || (model instanceof CodeCheckModel))){
            callErrorOccurred(new IllegalStateException("Enable the connection by passing a code into sendCode(String)"), ErrorOccurredHandler.Type.SEND);
            return;
        }

        send(model, aesKey);
    }

    /**
     * Sends a JSON model that implements the {@link IModel} interface to the server,
     * requires a key to encrypt the data.
     * <br>
     * {@link de.offrange.client.listeners.ErrorOccurredHandler#onErrorOccurred(Client, Exception, ErrorOccurredHandler.Type)}
     * is called if {@link #isRunning()} returns false or an exception occurs while writing data.
     * In both cases, {@link ErrorOccurredHandler.Type} will be {@link ErrorOccurredHandler.Type#SEND}.
     *
     * @param model JSON model to send.
     * @param key key to encrypt
     * @see #send(IModel)
     */
    private <E extends IModel> void send(E model, Key key){
        Objects.requireNonNull(key);
        if(!isRunning()){
            callErrorOccurred(new IllegalStateException("Client is not running"), ErrorOccurredHandler.Type.SEND);
            return;
        }

        byte[] data = gson.toJson(model).getBytes();
        try {
            data = RsaAesCryptography.encrypt(data, key);

            outputStream.writeInt(data.length);
            outputStream.write(data);
        } catch (Exception e) {
            callErrorOccurred(e, ErrorOccurredHandler.Type.SEND);
        }
    }

    /**
     * Receives data from the server and decrypt them. If an error occurs, it will call
     * {@link de.offrange.client.listeners.ErrorOccurredHandler#onErrorOccurred(Client, Exception, ErrorOccurredHandler.Type)}.
     * When such an error occurs, {@link ErrorOccurredHandler.Type} will be {@link ErrorOccurredHandler.Type#RECEIVE}
     * and the function will return {@code null}.
     * @return a {@link IModel} that represents the JSON file sent by the server.
     */
    private IModel receive(){
        if(!isRunning()){
            callErrorOccurred(new IllegalStateException("Client is not running"), ErrorOccurredHandler.Type.RECEIVE);
            return null;
        }

        try {
            byte[] receivedData = readFully();
            return gson.fromJson(new String(RsaAesCryptography.decryptAes(receivedData, aesKey)), isCodeChecked() ? modelClass : CodeCheckModel.class);
        } catch (Exception e) {
            callErrorOccurred(e, ErrorOccurredHandler.Type.RECEIVE);
        }

        return null;
    }

    /**
     * Waits the current thread until the handshake completes. If the handshake is already completed, it
     * will not pause the thread.
     * <br>
     * {@link de.offrange.client.listeners.ErrorOccurredHandler#onErrorOccurred(Client, Exception, ErrorOccurredHandler.Type)}
     * is called if there is already a thread waiting for the handshake to complete.
     * In this case, {@link ErrorOccurredHandler.Type} will be {@link ErrorOccurredHandler.Type#HANDSHAKE}.
     * @throws InterruptedException if any thread interrupted the current thread before or while the current thread
     *                              was waiting. The <em>interrupted status</em> of the current thread is cleared
     *                              when this exception is thrown.
     */
    public void waitForHandshake() throws InterruptedException {
        if(isHandshakeDone())
            return;

        if(waiterThread != null){
            callErrorOccurred(new IllegalStateException("there is already a thread waiting for the handshake to complete"), ErrorOccurredHandler.Type.HANDSHAKE);
            return;
        }

        waiterThread = Thread.currentThread();
        synchronized (waiterThread){
            waiterThread.wait();
        }
    }

    /**
     * Notifies a thread waiting for the handshake to complete.
     */
    private void notifyWaiterThread(){
        if(waiterThread == null)
            return;

        synchronized (waiterThread){
            waiterThread.notify();
        }

        waiterThread = null;
    }

    /**
     * Sends the code to enable the connection, allowing requests and responses from the server.
     * @param code the code to send
     */
    public void sendCode(String code){
        CodeCheckModel codeCheckModel = new CodeCheckModel();
        codeCheckModel.setCode(code);
        send(codeCheckModel, aesKey);
    }

    /**
     * Class extends {@link Thread} used to handle the communication between the server and this client.
     * If the code evaluation is received, the class will call {@link ReceiveHandler#onCodeEvaluationReceived(boolean, TcpClient)}
     * and if data is received, the class will call {@link ReceiveHandler#onDataReceived(IModel)}. It also checks if the
     * connection is enabled and allow or disallow the communication to the server.
     */
    private class ClientHandler extends Thread{

        @Override
        public void run() {
            running = true;

            try {
                inputStream = new DataInputStream(client.getInputStream());
                outputStream = new DataOutputStream(client.getOutputStream());

                doHandshake();
                notifyWaiterThread();
            } catch (IOException e) {
                callErrorOccurred(e, ErrorOccurredHandler.Type.HANDSHAKE);
            }

            while (isRunning()){
                IModel model = receive();
                if(model == null)
                    return;

                if(!codeChecked){
                    codeChecked = ((CodeCheckModel)model).isCodeCorrect();

                    if(getReceiveHandler() != null)
                        getReceiveHandler().onCodeEvaluationReceived(codeChecked, TcpClient.this);

                    continue;
                }

                if(getReceiveHandler() != null)
                    getReceiveHandler().onDataReceived(modelClass.cast(model));
            }
        }
    }
}
