package ru.alexkulikov.peppachat.server;

import ru.alexkulikov.peppachat.server.connection.ServerConnection;
import ru.alexkulikov.peppachat.shared.Message;

public class MessageEvent {
	private ServerConnection connection;
	private Message message;
	private SendMode mode;

	public MessageEvent(ServerConnection connection, Message message, SendMode mode) {
		this(connection, message);
		this.mode = mode;
	}

	public MessageEvent(ServerConnection connection, Message message) {
		this.connection = connection;
		this.message = message;
		this.mode = SendMode.RESPONSE;
	}

	public ServerConnection getConnection() {
		return connection;
	}

	public Message getMessage() {
		return message;
	}

	public SendMode getMode() {
		return mode;
	}
}
