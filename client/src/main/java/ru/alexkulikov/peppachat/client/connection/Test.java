package ru.alexkulikov.peppachat.client.connection;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ru.alexkulikov.peppachat.shared.Command;
import ru.alexkulikov.peppachat.shared.Message;
import ru.alexkulikov.peppachat.shared.Session;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by BigWats on 25.02.2017.
 */
public class Test {
    public static void main(String[] args) {
        Session session = new Session();
        session.setId(2L);
        session.setUserName("aaa");
        Message message = new Message(session, Command.MESSAGE, "first");
        Message message2 = new Message(session, Command.MESSAGE, "Second");

        List<Message> messages = new ArrayList<>();
        messages.add(message);
        messages.add(message2);

        String a = new Gson().toJson(messages);

        System.out.println(a);

        List<Message> a2 = new Gson().fromJson(a, new TypeToken<List<Message>>(){}.getType());
        System.out.println(a2.get(0));
        System.out.println(a2.get(1));
    }
}
