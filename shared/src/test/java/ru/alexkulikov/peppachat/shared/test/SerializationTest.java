package ru.alexkulikov.peppachat.shared.test;

import org.junit.Assert;
import org.junit.Test;
import ru.alexkulikov.peppachat.shared.Command;
import ru.alexkulikov.peppachat.shared.Message;
import ru.alexkulikov.peppachat.shared.MessageSerializer;
import ru.alexkulikov.peppachat.shared.Session;

public class SerializationTest extends Assert {

	@Test
	public void serializeTest() {
		Session session = new Session();
		session.setUserName("test");
		session.setId(1L);

		Message message = new Message(session, Command.MESSAGE, "test message");

		MessageSerializer serializer = new MessageSerializer();
		String serializeMessage = serializer.serialize(message);

		Message deserializeMessage = serializer.getMessage(serializeMessage);

		assertNotNull(deserializeMessage);
		assertEquals(message.getCommand(), deserializeMessage.getCommand());
		assertEquals(message.getText(), deserializeMessage.getText());

		Session deserializeSession = deserializeMessage.getSession();

		assertEquals(session.getId(), deserializeSession.getId());
		assertEquals(session.getUserName(), deserializeSession.getUserName());
	}
}
