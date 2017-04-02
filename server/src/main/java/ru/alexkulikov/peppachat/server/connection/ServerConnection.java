package ru.alexkulikov.peppachat.server.connection;

import ru.alexkulikov.peppachat.shared.Message;
import ru.alexkulikov.peppachat.shared.connection.Connection;

public interface ServerConnection extends Connection {

	void send(Long sessionId, Message message);

	void sendBroadcast(Message message);
}
