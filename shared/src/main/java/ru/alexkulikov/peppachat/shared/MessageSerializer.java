package ru.alexkulikov.peppachat.shared;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class MessageSerializer {

	private Gson gson;

	public MessageSerializer() {
		gson = new Gson();
	}

	public List<Message> getMessages(String data) {
		try {
			String concatenateParts = data.replaceAll("\\]\\[", ",");
			return gson.fromJson(concatenateParts, new TypeToken<List<Message>>(){}.getType());
		} catch (Exception e) {
			System.out.println("### Can't parse messages");
			return new ArrayList<>();
		}
	}

	public Message getMessage(String data) {
		try {
			return gson.fromJson(data, Message.class);
		} catch (Exception e) {
			System.out.println("### Can't parse messages");
			return null;
		}
	}

	public String serialize(Message message) {
		return gson.toJson(message);
	}

	public String serialize(List<Message> messages) {
		return gson.toJson(messages);
	}
}
