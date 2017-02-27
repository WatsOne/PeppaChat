package ru.alexkulikov.peppachat.server;


import ru.alexkulikov.peppachat.server.connection.ServerConnection;
import ru.alexkulikov.peppachat.server.storage.Storage;
import ru.alexkulikov.peppachat.shared.Message;

import java.util.concurrent.LinkedBlockingQueue;

public class MessageWorker implements Runnable {

	private LinkedBlockingQueue<MessageEvent> sendQueue = new LinkedBlockingQueue<>();
	private Storage storage;

	public MessageWorker(Storage storage) {
		this.storage = storage;
	}

	public void submit(MessageEvent event) {
		try {
			sendQueue.put(event);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while (true) {
			try {
				MessageEvent event = sendQueue.take();
				switch (event.getMode()) {
					case RESPONSE:
						Long id = event.getMessage().getSession().getId();
						event.getConnection().send(id, event.getMessage());
						break;
					case BROADCAST_AUTHORIZED:
						final ServerConnection connection = event.getConnection();
						final Message message = event.getMessage();
						storage.getAllSession().forEach(s -> connection.send(s.getId(), message));
						break;
					case BROADCAST:
						event.getConnection().sendBroadcast(event.getMessage());
						break;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
