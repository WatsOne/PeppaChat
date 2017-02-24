package ru.alexkulikov.peppachat.server;

import com.google.gson.Gson;
import ru.alexkulikov.peppachat.server.connection.ServerConnection;
import ru.alexkulikov.peppachat.server.connection.ServerConnectionFabric;
import ru.alexkulikov.peppachat.server.storage.Storage;
import ru.alexkulikov.peppachat.server.storage.StorageFactory;
import ru.alexkulikov.peppachat.shared.Command;
import ru.alexkulikov.peppachat.shared.Message;
import ru.alexkulikov.peppachat.shared.Session;
import ru.alexkulikov.peppachat.shared.connection.ConnectionEventListener;

import java.io.IOException;

public class Server implements ConnectionEventListener {
    private static final int PORT = 10521;
    private static final String HOST = "localhost";

    private Gson gson = new Gson();
    private Storage storage;
    private ServerConnection connection;

    private void run() throws Exception {
        storage = StorageFactory.getStorage();

        connection = ServerConnectionFabric.getServerConnection();
        connection.setup(HOST, PORT);
        connection.setEventListener(this);
        connection.start();
    }

    public static void main(String[] args) throws Exception {
        new Server().run();
        System.out.println("Server started...");
    }

    @Override
    public void onDataArrived(String messageStr) {
        try {
            Message message = gson.fromJson(messageStr, Message.class);
            System.out.println(message);
            switch (message.getCommand()) {
                case REGISTER:
                    register(message);
                    break;
            }
        } catch (Exception e) {

        }
    }

    private void register(Message message) throws IOException {
        Session serverSession = storage.getSession(message.getText());
        Session clientSession = message.getSession();
        if (serverSession == null) {
            clientSession.setName(message.getText());
            storage.saveSession(message.getSession());
            connection.write(clientSession, "Successfully register!", Command.REGISTER);
        } else {
            connection.write(message.getSession(), "User already registered", Command.REGISTER);
        }
    }
}
