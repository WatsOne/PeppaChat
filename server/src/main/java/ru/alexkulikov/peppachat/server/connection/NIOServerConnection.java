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

public class NIOServerConnection implements ServerConnection {

    private ByteBuffer readBuf = ByteBuffer.allocate(256);
    private Selector selector;
    private ServerSocketChannel socket;

    private ConnectionEventListener listener;
    private Map<Long, SelectionKey> connections = new HashMap<>();
    private Gson gson = new Gson();

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

            selector.select();
            socketIterator = selector.selectedKeys().iterator();

            while (socketIterator.hasNext()) {
                socketKey = socketIterator.next();
                socketIterator.remove();

                if (socketKey.isAcceptable()) {
                    processAccept(socketKey);
                }

                if (socketKey.isReadable()) {
                    processRead(socketKey);
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

        connections.put(id, clientKey);

        Session session = new Session();
        session.setId(id);
        channel.write(ByteBuffer.wrap(gson.toJson(new Message(session, Command.ID)).getBytes()));
        id++;
    }

    private void processRead(SelectionKey key) throws IOException {
        SocketChannel clientSocket = (SocketChannel) key.channel();

        readBuf.clear();
        int readCount = clientSocket.read(readBuf);

        String message;

        if (readCount >= 0) {
            message = SocketUtils.getBufferData(readBuf);
        } else {
            message = key.attachment() + " left the chat.\n";
            clientSocket.close();
        }

        System.out.println("+++ " + message);
        listener.onDataArrived(message);
    }

    @Override
    public boolean isAlive() {
        return true;
    }

    private void checkSetup() throws ConnectionException {
        if (socket == null || selector == null) {
            throw new ConnectionException("Connection doesn't setup");
        }
    }

    @Override
    public void send(Long sessionId, String message) throws IOException {
        SelectionKey key = connections.get(sessionId);
        SocketChannel channel = (SocketChannel) key.channel();
        channel.write(ByteBuffer.wrap(message.getBytes()));
    }

    @Override
    public void sendBroadcast(String message) throws IOException {
        connections.forEach((i, k) -> {
            try {
                SocketChannel channel = (SocketChannel) k.channel();
                channel.write(ByteBuffer.wrap(message.getBytes()));
            } catch (IOException e) {
            }
        });
    }
}
