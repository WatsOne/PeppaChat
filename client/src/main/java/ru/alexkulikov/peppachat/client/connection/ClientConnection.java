package ru.alexkulikov.peppachat.client.connection;

import ru.alexkulikov.peppachat.shared.connection.Connection;
import ru.alexkulikov.peppachat.shared.connection.ConnectionException;

public interface ClientConnection extends Connection {

	void setDataProducer(DataProducer consumer);

	void notifyToSend() throws ConnectionException;

	boolean isAlive();
}
