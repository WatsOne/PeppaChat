package ru.alexkulikov.peppachat.client.connection;

public class ClientConnectionFabric {

	public static ClientConnection getClientConnection() {
		return new NIOClientConnection();
	}
}
