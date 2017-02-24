package ru.alexkulikov.peppachat.server.connection;

import com.google.gson.Gson;
import ru.alexkulikov.peppachat.server.ChangeRequest;
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

    private ByteBuffer readBuf = ByteBuffer.allocate(256);
    private Selector selector;
    private ServerSocketChannel socket;

    private ConnectionEventListener listener;
    //    private Map<Long, SelectionKey> connections = new HashMap<>();
    private Map<Long, SocketChannel> connections = new HashMap<>();
    private Gson gson = new Gson();

    private Long id = 1L;

    private final List<ChangeRequest> changeRequests = new LinkedList<>();
    private final Map<SocketChannel, List<ByteBuffer>> pendingData = new HashMap<>();

    @Override
    public void notifyToSend() throws ConnectionException {

    }

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
                for (ChangeRequest change : changeRequests) {
                    switch (change.type) {
                        case ChangeRequest.CHANGEOPS:
                            SelectionKey key = change.socket.keyFor(selector);
                            key.interestOps(change.ops);
                            break;
                        default:
                    }
                }
                changeRequests.clear();
            }

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

                if (socketKey.isWritable()) {
                    write2(socketKey);
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
        channel.register(selector, OP_READ);

        connections.put(id, channel);
        channel.write(ByteBuffer.wrap(buildIdMessage(id).getBytes()));
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

    private String buildIdMessage(Long id) throws IOException {
        Message message = new Message();
        Session session = new Session();
        session.setId(id);
        message.setSession(session);
        message.setCommand(Command.ID);
        return gson.toJson(message, Message.class);
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
    public void write(Long sessionId, String message) {
//        try {
//            SelectionKey key = connections.get(sessionId);
//            SocketChannel clientChannel = (SocketChannel) key.channel();
//            clientChannel.write(ByteBuffer.wrap(message.getBytes()));
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//        }


        synchronized (changeRequests) {
            SocketChannel channel = connections.get(sessionId);
            changeRequests.add(new ChangeRequest(channel, ChangeRequest.CHANGEOPS, OP_WRITE));
            synchronized (pendingData) {
                List<ByteBuffer> queue = pendingData.get(channel);
                if (queue == null) {
                    queue = new ArrayList<>();
                    pendingData.put(channel, queue);
                }
                queue.add(ByteBuffer.wrap(message.getBytes()));
            }
        }
        selector.wakeup();
    }

    private void write2(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        synchronized (pendingData) {
            List<ByteBuffer> queue = pendingData.get(socketChannel);
            while (!queue.isEmpty()) {
                ByteBuffer buf = queue.get(0);
                socketChannel.write(buf);
                if (buf.remaining() > 0) {
                    break;
                }
                System.out.println("Send echo = " + new String(queue.get(0).array()));
                queue.remove(0);
            }
            if (queue.isEmpty()) {
                key.interestOps(OP_READ);
            }
        }
    }
}
