package ru.alexkulikov.peppachat.server.connection;

import ru.alexkulikov.peppachat.shared.*;
import ru.alexkulikov.peppachat.shared.connection.ConnectionEventListener;
import ru.alexkulikov.peppachat.shared.connection.ConnectionException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

public class NIOServerConnection implements ServerConnection {

	private ByteBuffer readBuf = ByteBuffer.allocate(Constants.BUFFER_SIZE);
	private Selector selector;
	private ServerSocketChannel socket;

	private ConnectionEventListener listener;
	private Map<Long, SelectionKey> connections = new HashMap<>();

	private final List<SelectionKey> changeRequests = new LinkedList<>();
	private final Map<SelectionKey, List<Message>> pendingData = new HashMap<>();

	private Long id = 1L;
	private MessageSerializer serializer;

	@Override
	public void setEventListener(ConnectionEventListener listener) {
		this.listener = listener;
	}

	@Override
	public void setup(String host, int port) throws IOException {
		selector = Selector.open();

		socket = ServerSocketChannel.open();
		socket.bind(new InetSocketAddress(host, port));
		socket.configureBlocking(false);

		socket.register(selector, OP_ACCEPT);

		serializer = new MessageSerializer();
	}

	@Override
	public void start() throws ConnectionException, IOException {
		Iterator<SelectionKey> socketIterator;
		SelectionKey socketKey;

		while (socket.isOpen()) {

			synchronized (changeRequests) {
				changeRequests.stream().filter(k -> k != null && k.isValid()).forEach(k -> k.interestOps(OP_WRITE));
				changeRequests.clear();
			}

			selector.select();
			socketIterator = selector.selectedKeys().iterator();

			while (socketIterator.hasNext()) {
				socketKey = socketIterator.next();
				socketIterator.remove();

				if (socketKey.isAcceptable()) {
					processAccept(socketKey);
				} else if (socketKey.isReadable()) {
					processRead(socketKey);
				} else if (socketKey.isWritable()) {
					processWrite(socketKey);
				}
			}
		}
	}

	@Override
	public void shutDown() {
		try {
			socket.close();
		} catch (IOException e) {
			System.out.println("Can't close socket");
		}
	}

	private void processAccept(SelectionKey key) throws IOException {
		SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
		channel.configureBlocking(false);
		SelectionKey clientKey = channel.register(selector, OP_READ);

		clientKey.attach(id);
		connections.put(id, clientKey);

		Session session = new Session();
		session.setId(id);
		send(id, new Message(session, Command.ID));

		id++;
	}

	private void processRead(SelectionKey key) throws IOException {
		SocketChannel clientSocket = (SocketChannel) key.channel();

		readBuf.clear();
		int readCount;
		try {
			readCount = clientSocket.read(readBuf);
		} catch (IOException e) {
			processUserDisconnect(key);
			return;
		}

		if (readCount < 0) {
			processUserDisconnect(key);
			return;
		}

		String messageStr = SocketUtils.getBufferData(readBuf);
		Message message = serializer.getMessage(messageStr);
		if (message != null) {
			listener.onDataArrived(message);
		}
	}

	private void processUserDisconnect(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		Long sessionId = (Long) key.attachment();
		connections.remove(sessionId);
		listener.onDisconnect(sessionId);
		channel.close();
	}

	@Override
	public void send(Long sessionId, Message message) {
		SelectionKey key = connections.get(sessionId);
		synchronized (changeRequests) {
			changeRequests.add(key);
			synchronized (pendingData) {
				List<Message> queue = pendingData.computeIfAbsent(key, k -> new ArrayList<>());
				queue.add(message);
			}
		}
		selector.wakeup();
	}

	private void processWrite(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		synchronized (pendingData) {
			try {
				List<Message> queue = pendingData.get(key);
				if (queue.size() > 0) {
					socketChannel.write(ByteBuffer.wrap(serializer.serialize(queue).getBytes()));
					queue.clear();
				}
				key.interestOps(OP_READ);
			} catch (IOException e) {
				processUserDisconnect(key);
			}
		}
	}

	@Override
	public void sendBroadcast(Message message) {
		connections.forEach((i, k) -> send(i, message));
	}
}
