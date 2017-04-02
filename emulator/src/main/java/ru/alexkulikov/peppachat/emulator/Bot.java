package ru.alexkulikov.peppachat.emulator;

import ru.alexkulikov.peppachat.client.connection.ClientConnection;
import ru.alexkulikov.peppachat.client.connection.ClientConnectionFabric;
import ru.alexkulikov.peppachat.client.connection.DataProducer;
import ru.alexkulikov.peppachat.shared.Command;
import ru.alexkulikov.peppachat.shared.Message;
import ru.alexkulikov.peppachat.shared.Session;
import ru.alexkulikov.peppachat.shared.connection.ConnectionEventListener;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

public class Bot implements ConnectionEventListener, DataProducer {
	private static final String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	private Random rnd = new Random();

	private static final int PORT = 10521;
	private static final String HOST = "localhost";

	private BlockingQueue<Message> queue = new ArrayBlockingQueue<>(2);
	private Session session;
	private String botName;
	private ClientConnection connection;
	private boolean observe;
	private Queue<Event> receive;
	private Queue<Bot> bots;

	public void start(String botName, boolean observe, final Queue<Event> send, final Queue<Event> receive, final Queue<Bot> bots, int sendCount, CountDownLatch doneLatch) {
		try {
			connection = ClientConnectionFabric.getClientConnection();
			connection.setup(HOST, PORT);
			connection.setEventListener(this);
			connection.setDataProducer(this);

			this.botName = botName;
			this.observe = observe;
			this.receive = receive;
			this.bots = bots;

			new Thread(() -> {
				try {
					while (send.size() <= sendCount) {
						Thread.sleep(1000 * (rnd.nextInt(15) + 2));
						if (send.size() <= sendCount) {
							long time = System.currentTimeMillis();
							String msg = getRandomString(rnd.nextInt(20) + 1) + "|" + time;
							send.add(new Event(time, bots.size()));
							queue.put(new Message(session, Command.MESSAGE, msg));
							connection.notifyToSend();
						}
					}

					Thread.sleep(100);
					connection.shutDown();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					bots.remove(this);
					doneLatch.countDown();
				}
			}).start();

			connection.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String getRandomString(int len) {
		StringBuilder sb = new StringBuilder(len);
		for(int i = 0; i < len; i++) {
			sb.append(chars.charAt(rnd.nextInt(chars.length())));
		}
		return sb.toString();
	}

	@Override
	public Message getDataToSend() {
		return queue.poll();
	}

	@Override
	public void onDataArrived(Message message) {
		try {
			switch (message.getCommand()) {
				case ID:
					if (observe) {
						System.out.println("### Successfully connected, id: " + message.getSession().getId());
						System.out.println("### Welcome to PeppaChat! Please enter your name:");
					}
					this.session = message.getSession();
					queue.put(new Message(session, Command.REGISTER, botName));
					connection.notifyToSend();
					break;
				case REGISTER:
					this.session = message.getSession();
					serverMessage(message.getText());
					break;
				case SERVER_MESSAGE:
					serverMessage(message.getText());
					break;
				case HISTORY:
					if (observe) {
						System.out.println(message.getText());
					}
					break;
				case MESSAGE:
				default:
					if (observe) {
						System.out.println("(" + botName + ") " + message.getText());
					}

					String[] msg = message.getText().split("\\|");
					String time= msg[1];

					Event event = new Event(System.currentTimeMillis() - Long.parseLong(time), bots.size());

					receive.add(event);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void serverMessage(String message) {
		if (observe) {
			System.out.println("### " + message);
		}
	}

	@Override
	public void onDisconnect(Long sessionId) {
		System.out.println("### Server shutdown, try to restart the app");
	}
}
