package ru.alexkulikov.peppachat.server;

import ru.alexkulikov.peppachat.server.connection.ServerConnection;
import ru.alexkulikov.peppachat.server.connection.ServerConnectionFabric;
import ru.alexkulikov.peppachat.shared.connection.ConnectionEventListener;

public class Server implements ConnectionEventListener {
    private static final int PORT = 10521;
    private static final String HOST = "localhost";

    private void run() throws Exception {
        ServerConnection connection = ServerConnectionFabric.getServerConnection();
        connection.setup(HOST, PORT);
        connection.setEventListener(this);
        connection.start();
    }

    public static void main(String[] args) throws Exception {
        new Server().run();
        System.out.println("Server started...");
    }

    @Override
    public void onDataArrived(String message) {
        System.out.println(message);
    }
}
