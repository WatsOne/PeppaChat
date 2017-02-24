package ru.alexkulikov.peppachat.server.connection;

import ru.alexkulikov.peppachat.shared.connection.Connection;

import java.io.IOException;

public interface ServerConnection extends Connection {
    void write(Long sessionId, String message) throws IOException;
}
