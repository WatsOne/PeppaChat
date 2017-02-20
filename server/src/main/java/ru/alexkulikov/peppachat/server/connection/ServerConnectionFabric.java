package ru.alexkulikov.peppachat.server.connection;

public class ServerConnectionFabric {
    public static ServerConnection getServerConnection() {
        return new NIOServerConnection();
    }
}
