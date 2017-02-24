package ru.alexkulikov.peppachat.server.connection;

import ru.alexkulikov.peppachat.shared.connection.Connection;

import java.io.IOException;

public interface ServerConnection extends Connection {
    void send(Long sessionId, String message) throws IOException;
    void sendBroadcast(String message) throws IOException;
}
