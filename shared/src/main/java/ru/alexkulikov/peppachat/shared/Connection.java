package ru.alexkulikov.peppachat.shared;

import java.io.IOException;

public interface Connection {
    void notifyToSend() throws ConnectionException;

    void setEventListener(ConnectionEventListener listener);

    void setup(String host, int port) throws IOException;

    void start() throws ConnectionException, IOException;

    void shutDown();

    boolean isAlive();
}
