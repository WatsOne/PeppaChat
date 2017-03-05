package ru.alexkulikov.peppachat.server.test;

import org.junit.Assert;
import org.junit.Test;
import ru.alexkulikov.peppachat.server.storage.Storage;
import ru.alexkulikov.peppachat.server.storage.StorageFactory;
import ru.alexkulikov.peppachat.shared.Command;
import ru.alexkulikov.peppachat.shared.Constants;
import ru.alexkulikov.peppachat.shared.Message;
import ru.alexkulikov.peppachat.shared.Session;

public class StorageTest extends Assert {

	@Test
	public void storageTest() {
		Storage storage = StorageFactory.getStorage();
		assertNotNull(storage);

		Session session = new Session();
		session.setUserName("test");
		session.setId(1L);

		storage.saveSession(session);
		assertEquals(storage.getAllSession().size(), 1);

		Session storageSession = storage.getSession(1L);
		assertNotNull(storageSession);
		assertEquals(session.getUserName(), storageSession.getUserName());

		for (int i = 0; i < Constants.STORE_MESSAGE_COUNT + 10; i++) {
			storage.saveMessage(new Message(session, Command.MESSAGE, "t"));
		}

		assertEquals(storage.getLastMessages().length(), Constants.STORE_MESSAGE_COUNT * 2);
	}
}
