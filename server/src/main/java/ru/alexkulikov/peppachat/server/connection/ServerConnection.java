package ru.alexkulikov.peppachat.server.connection;

import ru.alexkulikov.peppachat.shared.Command;
import ru.alexkulikov.peppachat.shared.Session;
import ru.alexkulikov.peppachat.shared.connection.Connection;

import java.io.IOException;

public interface ServerConnection extends Connection {
    void write(Session session, String text, Command command) throws IOException;
}
