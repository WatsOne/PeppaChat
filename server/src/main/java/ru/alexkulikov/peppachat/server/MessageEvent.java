package ru.alexkulikov.peppachat.server;

import ru.alexkulikov.peppachat.server.connection.ServerConnection;
import ru.alexkulikov.peppachat.shared.Message;

public class MessageEvent {
    private ServerConnection connection;
    private Message message;

    public MessageEvent(ServerConnection connection, Message message) {
        this.connection = connection;
        this.message = message;
    }

    public ServerConnection getConnection() {
        return connection;
    }

    public Message getMessage() {
        return message;
    }
}
