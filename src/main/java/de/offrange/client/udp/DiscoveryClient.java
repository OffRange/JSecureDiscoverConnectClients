package de.offrange.client.udp;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.offrange.client.gson.ByteArrayTypeAdapter;
import de.offrange.client.listeners.DiscoveryHandler;
import de.offrange.client.listeners.ErrorOccurredHandler;
import de.offrange.client.models.UdpModel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class used to discover open UDP servers on the local network.
 */
public class DiscoveryClient {

    public static final String BROADCAST_IP = "255.255.255.255";
    public static final String DEFAULT_NAME = "udp-discover-client";
    public static final int DEFAULT_TIMEOUT = 500;
    public static final int DEFAULT_DISCOVERY_TIME = 5000;

    private final Gson gson;

    private DatagramSocket udp;
    private int timeout;

    private boolean discovering;

    private final int port;
    private int discoveryTime;
    private String name;

    private DiscoveryHandler discoveryHandler;
    private ErrorOccurredHandler errorOccurredHandler;

    /**
     * Constructs a DiscoveryClient instance with a port.
     * @param port used to discover a UDP server.
     */
    public DiscoveryClient(int port) {
        this.port = port;
        gson = new GsonBuilder()
                .disableHtmlEscaping()
                .registerTypeAdapter(byte[].class, new ByteArrayTypeAdapter())
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();

        name = DEFAULT_NAME;
        discoveryTime = DEFAULT_DISCOVERY_TIME;
        timeout = DEFAULT_TIMEOUT;
    }

    /**
     * @return the name of this client.
     * @see #setName(String)
     */
    public String getName() {
        return name;
    }

    /**
     * Used to set the name of this client. It is optional since {@link #DEFAULT_NAME} is set as the name
     * by the constructor.
     * @param name the name the client should have.
     * @see #getName()
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the length of time the client should scan for open UDP servers on the local network.
     * @see #setDiscoveryTime(int)
     */
    public int getDiscoveryTime() {
        return discoveryTime;
    }

    /**
     * Used to set the length of time the client should scan for open UDP servers on the local network. It is optional
     * since {@link #DEFAULT_DISCOVERY_TIME} is set as the discovery time by the constructor.
     * @param discoveryTime the length of time the client should scan in milliseconds.
     * @see #getDiscoveryTime()
     */
    public void setDiscoveryTime(int discoveryTime) {
        this.discoveryTime = discoveryTime;
    }

    /**
     * @return the timeout after one UDP scan times out.
     * @see #setTimeout(int)
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Used to set the timeout. If one UDP scan reach this amount of time, it closes and start a new scan with this
     * timeout until {@link #getDiscoveryTime()} is reached. It is optional to set since {@link #DEFAULT_TIMEOUT} is set
     * as the timeout by the constructor.
     * @param timeout the length of time the client should scan in milliseconds.
     * @see #setTimeout(int)
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * @return the {@link DiscoveryHandler} that is used to handle discover events.
     * @see #setDiscoveryHandler(DiscoveryHandler)
     */
    public DiscoveryHandler getDiscoveryHandler() {
        return discoveryHandler;
    }

    /**
     * Used to handle new discovered servers.
     * @param discoveryHandler the {@link DiscoveryHandler} used to handle discover events.
     * @see #getDiscoveryHandler()
     */
    public void setDiscoveryHandler(DiscoveryHandler discoveryHandler) {
        this.discoveryHandler = discoveryHandler;
    }

    /**
     * @return the {@link ErrorOccurredHandler} that is used to handle error events.
     * @see #setErrorOccurredHandler(ErrorOccurredHandler)
     */
    public ErrorOccurredHandler getErrorOccurredHandler() {
        return errorOccurredHandler;
    }

    /**
     * Used to handle errors encountered while scanning for servers.
     * @param errorOccurredHandler the {@link ErrorOccurredHandler} that is used to handle error events.
     * @see #setErrorOccurredHandler(ErrorOccurredHandler)
     */
    public void setErrorOccurredHandler(ErrorOccurredHandler errorOccurredHandler) {
        this.errorOccurredHandler = errorOccurredHandler;
    }

    /**
     * Starts the discovering process. Please note that this method creates a new thread and any event called
     * by the discovery process will be called within this new thread.
     */
    public void startDiscovering() {
        discovering = true;
        DiscoveryThread discoveryThread = new DiscoveryThread();
        discoveryThread.setName(getName());
        discoveryThread.start();
    }

    /**
     * @return true if client is discovering and {@link #startDiscovering()} is running, false otherwise.
     */
    public boolean isDiscovering() {
        return discovering;
    }

    /**
     * Cancel the currently running discovery.
     */
    public void cancelDiscovering(){
        discovering = false;
        try{
            if(!udp.isClosed())
                udp.setSoTimeout(0);
        }catch (SocketException e){
            if(errorOccurredHandler != null)
                errorOccurredHandler.onErrorOccurred(e, ErrorOccurredHandler.Type.UDP_CANCEL);
        }
    }

    private class DiscoveryThread extends Thread{

        @Override
        public void run() {
            try{
                udp = new DatagramSocket();
                udp.setBroadcast(true);
                udp.setSoTimeout(timeout);

                byte[] sendData = gson.toJson(new UdpModel(this.getName())).getBytes();

                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(BROADCAST_IP), port);
                udp.send(sendPacket);

                List<DiscoveredEndpoint> endpoints = new ArrayList<>();

                long endTime = System.currentTimeMillis() + discoveryTime;
                while (System.currentTimeMillis() < endTime && discovering){
                    try {
                        byte[] data = new byte[512];
                        DatagramPacket receivePacket = new DatagramPacket(data, data.length);
                        udp.receive(receivePacket);

                        String json = new String(receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength());
                        UdpModel receivedModel = gson.fromJson(json, UdpModel.class);
                        if(!receivedModel.isValidResponse())
                            return;

                        if(endpoints.stream().anyMatch(e -> e.getAddress().getIp().equals(receivedModel.getAddress().getIp())))
                            continue;

                        DiscoveredEndpoint endpoint = new DiscoveredEndpoint(receivedModel.getName(), receivedModel.getAddress());
                        endpoints.add(endpoint);

                        if(discoveryHandler != null)
                            discoveryHandler.onDiscovered(endpoint);
                    }catch (Exception e){
                        if((e instanceof SocketTimeoutException))
                            continue;

                        if(errorOccurredHandler != null)
                            errorOccurredHandler.onErrorOccurred(e, ErrorOccurredHandler.Type.UDP_DISCOVERING);
                    }
                }

                udp.close();
                discovering = false;

                if(discoveryHandler != null)
                    discoveryHandler.onFinish(endpoints);
            }catch (IOException e){
                if(errorOccurredHandler != null)
                    errorOccurredHandler.onErrorOccurred(e, ErrorOccurredHandler.Type.UDP_DISCOVERING);
            }
        }
    }
}
