package ru.alexkulikov.peppachat.client;

import org.apache.commons.lang3.StringUtils;
import ru.alexkulikov.peppachat.client.connection.ClientConnection;
import ru.alexkulikov.peppachat.shared.Command;
import ru.alexkulikov.peppachat.shared.Session;
import ru.alexkulikov.peppachat.shared.connection.ConnectionException;
import ru.alexkulikov.peppachat.client.connection.ClientConnectionFabric;
import ru.alexkulikov.peppachat.client.connection.DataProducer;
import ru.alexkulikov.peppachat.shared.connection.ConnectionEventListener;
import ru.alexkulikov.peppachat.shared.Message;

import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Client implements ConnectionEventListener, DataProducer {

	private static final int PORT = 10521;
	private static final String HOST = "localhost";

	private BlockingQueue<Message> queue = new ArrayBlockingQueue<>(2);
	private Session session;
	private ClientConnection connection;

	private void run(String host, int port) {
		try {
			connection = ClientConnectionFabric.getClientConnection();
			connection.setup(host, port);
			connection.setEventListener(this);
			connection.setDataProducer(this);
		} catch (Exception e) {
			System.out.println("### Can't setup connection: " + e.getMessage());
		}

		new Thread(() -> {
			Scanner scanner = new Scanner(System.in);
			while (true) {
				try {
					String line = scanner.nextLine();

					if (!connection.isAlive()) {
						break;
					}

					try {
						if (session == null) {
							throw new ConnectionException("Session is null");
						}

						if (!isRegister()) {
							queue.put(new Message(session, Command.REGISTER, line));
							connection.notifyToSend();
						} else {
							Command command = Command.getCommand(line);
							if (command != null) {
								processUserCommand(command);
							} else {
								queue.put(new Message(session, Command.MESSAGE, line));
								connection.notifyToSend();
							}
						}
					} catch (InterruptedException e) {
						throw new ConnectionException(e.getMessage());
					}
				} catch (ConnectionException e) {
					System.out.println("### Error: " + e.getMessage());
					System.out.println("### Please, try to restart the application");
				}
			}
		}).start();

		try {
			connection.start();
		} catch (Exception e) {
			System.out.println("### Connection error, try to restart the application");
			connection.shutDown();
		}
	}

	private void processUserCommand(Command command) throws InterruptedException, ConnectionException {
		switch (command) {
			case HELP:
				System.out.println(Command.getCommandInfo());
				break;
			case EXIT:
				connection.shutDown();
				System.out.println("### Successfully disconnected");
				break;
			case CHANGENAME:
				System.out.println("### Enter new username:");
				Scanner scanner = new Scanner(System.in);
				String newUserName = scanner.nextLine();
				if (!StringUtils.isEmpty(newUserName)) {
					queue.put(new Message(session, Command.CHANGENAME, newUserName));
					connection.notifyToSend();
				}
				break;
			default:
				queue.put(new Message(session, command, null));
				connection.notifyToSend();
		}
	}

	public static void main(String[] args) throws Exception {
		String hostArg = args.length > 0 ? args[0] : "";
		String portArg = args.length > 1 ? args[1] : "";

		hostArg = StringUtils.isEmpty(hostArg) ? HOST : hostArg;
		int port = StringUtils.isEmpty(portArg) ? PORT : Integer.valueOf(portArg);

		new Client().run(hostArg, port);
	}

	@Override
	public Message getDataToSend() {
		return queue.poll();
	}

	@Override
	public void onDataArrived(Message message) {
		switch (message.getCommand()) {
			case ID:
				System.out.println("### Successfully connected, id: " + message.getSession().getId());
				System.out.println("### Welcome to PeppaChat! Please enter your name:");
				this.session = message.getSession();
				break;
			case REGISTER:
				this.session = message.getSession();
				serverMessage(message.getText());
				break;
			case SERVER_MESSAGE:
				serverMessage(message.getText());
				break;
			case MESSAGE:
			case HISTORY:
			default:
				System.out.println(message.getText());
		}
	}

	private void serverMessage(String message) {
		System.out.println("### " + message);
	}

	@Override
	public void onDisconnect(Long sessionId) {
		System.out.println("### Server shutdown");
	}

	private boolean isRegister() {
		return !StringUtils.isEmpty(session.getUserName());
	}
}
