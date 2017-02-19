package ru.alexkulikov.peppachat.client;

public class ClientConnectionFabric {

    public static ClientConnection getClienConnection() {
        return new NIOClientConnection();
    }
}
