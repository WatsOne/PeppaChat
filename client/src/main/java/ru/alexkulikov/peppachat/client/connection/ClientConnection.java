package ru.alexkulikov.peppachat.client.connection;

import ru.alexkulikov.peppachat.shared.Connection;

public interface ClientConnection extends Connection {
    void setDataProducer(DataProducer consumer);
}