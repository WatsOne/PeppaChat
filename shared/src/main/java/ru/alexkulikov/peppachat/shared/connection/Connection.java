package ru.alexkulikov.peppachat.shared.connection;

import java.io.IOException;

public interface Connection {
	void setEventListener(ConnectionEventListener listener);

	void setup(String host, int port) throws IOException;

	void start() throws ConnectionException, IOException;

	void shutDown();
}
