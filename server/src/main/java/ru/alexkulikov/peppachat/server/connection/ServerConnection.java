package ru.alexkulikov.peppachat.server.connection;

import ru.alexkulikov.peppachat.shared.Message;
import ru.alexkulikov.peppachat.shared.connection.Connection;

import java.io.IOException;

public interface ServerConnection extends Connection {

    void send(Long sessionId, Message message) throws IOException;

    void sendBroadcast(Message message) throws IOException;
}
