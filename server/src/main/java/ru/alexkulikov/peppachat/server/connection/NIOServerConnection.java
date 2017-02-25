package ru.alexkulikov.peppachat.server.connection;

import com.google.gson.Gson;
import ru.alexkulikov.peppachat.shared.Command;
import ru.alexkulikov.peppachat.shared.Session;
import ru.alexkulikov.peppachat.shared.connection.ConnectionEventListener;
import ru.alexkulikov.peppachat.shared.connection.ConnectionException;
import ru.alexkulikov.peppachat.shared.Message;
import ru.alexkulikov.peppachat.shared.SocketUtils;

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

    private ByteBuffer readBuf = ByteBuffer.allocate(8096);
    private Selector selector;
    private ServerSocketChannel socket;

    private ConnectionEventListener listener;
    private Map<Long, SelectionKey> connections = new HashMap<>();
    private Gson gson = new Gson();

    private final List<SelectionKey> changeRequests = new LinkedList<>();
    private final Map<SelectionKey, List<Message>> pendingData = new HashMap<>();

    private Long id = 1L;

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
    }

    @Override
    public void start() throws ConnectionException, IOException {
        Iterator<SelectionKey> socketIterator;
        SelectionKey socketKey;

        while (socket.isOpen()) {

            synchronized (changeRequests) {
                changeRequests.stream().forEach(k -> k.interestOps(OP_WRITE));
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
        int readCount = 0;
        try {
            readCount = clientSocket.read(readBuf);
        } catch (IOException e) {
            processUserDisconnect((Long) key.attachment(), clientSocket);
        }

        if (readCount < 0) {
            processUserDisconnect((Long) key.attachment(), clientSocket);
            return;
        }

        String message = SocketUtils.getBufferData(readBuf);
        System.out.println(message);
        listener.onDataArrived(gson.fromJson(message, Message.class));
    }

    private void processUserDisconnect(Long sessionId, SocketChannel channel) throws IOException {
        connections.remove(sessionId);
        listener.onDisconnect(sessionId);
        channel.close();
    }

    private void checkSetup() throws ConnectionException {
        if (socket == null || selector == null) {
            throw new ConnectionException("Connection doesn't setup");
        }
    }

    @Override
    public void send(Long sessionId, Message message) throws IOException {
        SelectionKey key = connections.get(sessionId);
        synchronized (changeRequests) {
            changeRequests.add(key);
            synchronized (pendingData) {
                List<Message> queue = pendingData.get(key);
                if (queue == null) {
                    queue = new ArrayList<>();
                    pendingData.put(key, queue);
                }
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
                    socketChannel.write(ByteBuffer.wrap(gson.toJson(queue).getBytes()));
                    queue.clear();
                }
                key.interestOps(OP_READ);
            } catch (IOException e) {
//                processUserDisconnect((Long) key.attachment(), socketChannel);
            }
        }
    }

    @Override
    public void sendBroadcast(Message message) throws IOException {
        connections.forEach((i, k) -> {
            try {
                send(i, message);
            } catch (IOException e) {
            }
        });
    }

    private void sendUserDisconnected() {

    }
}
