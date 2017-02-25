package ru.alexkulikov.peppachat.server;

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

    private Storage storage;
    private ServerConnection connection;

    private MessageWorker worker = new MessageWorker();

    private void run() throws Exception {
        new Thread(worker).start();
        storage = StorageFactory.getStorage();

        connection = ServerConnectionFabric.getServerConnection();
        connection.setup(HOST, PORT);
        connection.setEventListener(this);
        connection.start();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Server started...");
        new Server().run();
    }

    @Override
    public void onDataArrived(Message message) {
        try {
            System.out.println(message);
            switch (message.getCommand()) {
                case REGISTER:
                    register(message);
                    break;
                case MESSAGE:
                    sendMessage(message);
                    break;
                default:
                    processCommand(message);
            }
        } catch (Exception e) {

        }
    }

    @Override
    public void onDisconnect(Long sessionId) {
        Session session = storage.getSession(sessionId);
        storage.removeSession(sessionId);
        Message message = new Message(session, Command.SERVER_MESSAGE, "User \"" + session.getUserName() + "\" has left the chat.");
        worker.submit(new MessageEvent(connection, message, SendMode.BROADCAST));
    }

    private void sendMessage(Message message) {
        storage.saveMessage(message);
        message.setText(message.getSession().getUserName() + ": " + message.getText());
        worker.submit(new MessageEvent(connection, message, SendMode.BROADCAST));
    }

    private void register(Message message) throws IOException {
        Session clientSession = message.getSession();
        if (userNameExists(message.getText())) {
            worker.submit(new MessageEvent(connection, new Message(clientSession, Command.REGISTER, "User already register, please, enter a new name:")));
        } else {
            clientSession.setUserName(message.getText());
            storage.saveSession(message.getSession());
            worker.submit(new MessageEvent(connection, new Message(clientSession, Command.REGISTER, "Successfully register!")));
            worker.submit(new MessageEvent(connection, new Message(clientSession, Command.MESSAGE, storage.getLastMessages())));
        }
    }

    private boolean userNameExists(String userName) {
        return storage.getAllSession().stream().anyMatch(s -> s.getUserName().equals(userName));
    }

    private void processCommand(Message message) {
        String commandResult;
        switch (message.getCommand()) {
            case ONLINE:
                commandResult = "Current online: " + String.valueOf(storage.getAllSession().size());
                break;
            default:
                commandResult = "Command not implemented";
        }

        worker.submit(new MessageEvent(connection, new Message(message.getSession(), Command.SERVER_MESSAGE, commandResult)));
    }
}
