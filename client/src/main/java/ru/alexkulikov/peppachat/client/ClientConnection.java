package ru.alexkulikov.peppachat.client;


import ru.alexkulikov.peppachat.shared.ConnectionDataProducer;
import ru.alexkulikov.peppachat.shared.ConnectionEventListener;

import java.io.IOException;

public interface ClientConnection {
    void notifyToSend() throws ClientConnectionException;

    void setListener(ConnectionEventListener listener);

    void setDataProducer(ConnectionDataProducer consumer);

    void setup(String host, int port) throws IOException;

    void start() throws ClientConnectionException, IOException;

    void shutDown();
}
