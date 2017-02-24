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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class Server implements ConnectionEventListener {
    private static final int PORT = 10521;
    private static final String HOST = "localhost";

    private Gson gson = new Gson();
    private Storage storage;
    private ServerConnection connection;

    private MessageWorker worker = new MessageWorker();
    private Map<Long, LinkedList<Message>> histories = new HashMap<>();

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
    public void onDataArrived(String messageStr) {
        try {
            Message message = gson.fromJson(messageStr, Message.class);
            System.out.println(message);
            switch (message.getCommand()) {
                case REGISTER:
                    register(message);
                    break;
                case HISTORY:
                    loadHistory(message);
                    break;
                case MESSAGE:
                    sendMessage(message);
                    break;
            }
        } catch (Exception e) {

        }
    }

    private void sendMessage(Message message) {
        storage.saveMessage(message);
        message.setText(message.getSession().getUserName() + ": " + message.getText());
        worker.submit(new MessageEvent(connection, message, SendMode.BROADCAST));
    }

    private void register(Message message) throws IOException {
        Session serverSession = storage.getSession(message.getText());
        Session clientSession = message.getSession();
        if (serverSession != null) {
            worker.submit(new MessageEvent(connection, new Message(clientSession, Command.REGISTER, "User already registered")));
        } else{
            clientSession.setUserName(message.getText());
            storage.saveSession(message.getSession());
            worker.submit(new MessageEvent(connection, new Message(clientSession, Command.REGISTER, "Successfully register!")));
        }
    }

    private void loadHistory(Message message) throws IOException {
        Long id = message.getSession().getId();
        if (!histories.containsKey(id)) {
            histories.put(id, storage.getLastMessages());
        }

        LinkedList<Message> queue = histories.get(id);
        if (queue.size() == 0) {
            histories.remove(id);
        } else {
            worker.submit(new MessageEvent(connection, new Message(message.getSession(), Command.HISTORY, queue.poll().getText())));
        }
    }
}
