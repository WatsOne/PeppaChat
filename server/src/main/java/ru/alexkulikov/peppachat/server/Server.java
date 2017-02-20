package ru.alexkulikov.peppachat.server;

import ru.alexkulikov.peppachat.server.connection.ServerConnection;
import ru.alexkulikov.peppachat.server.connection.ServerConnectionFabric;

public class Server {
    private static final int PORT = 10521;
    private static final String HOST = "localhost";

    private void run() throws Exception {
        ServerConnection connection = ServerConnectionFabric.getServerConnection();
        connection.setup(HOST, PORT);
        connection.start();
    }

    public static void main(String[] args) throws Exception {
        new Server().run();
        System.out.println("Server started...");
    }
}
