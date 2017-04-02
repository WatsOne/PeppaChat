package ru.alexkulikov.peppachat.client.connection;

import ru.alexkulikov.peppachat.shared.Message;

public interface DataProducer {
	Message getDataToSend();
}
