package ru.alexkulikov.peppachat.server.connection;

import com.google.gson.Gson;
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
import java.util.Iterator;

public class NIOServerConnection implements ServerConnection {

    private ByteBuffer readBuf = ByteBuffer.allocate(256);
    private Selector selector;
    private ServerSocketChannel socket;

    private ConnectionEventListener listener;

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

        socket.register(selector, SelectionKey.OP_ACCEPT);
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
        SocketChannel clientSocket = ((ServerSocketChannel) key.channel()).accept();
        clientSocket.configureBlocking(false);
        clientSocket.register(selector, SelectionKey.OP_READ);
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
        Message m = new Gson().fromJson(message, Message.class);

        if (m.getText().equals("ww")) {
            write(key);
        }
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        Message message = new Message();
        message.setText("HEELO! FROM SERVER");
        clientChannel.write(ByteBuffer.wrap(new Gson().toJson(message, Message.class).getBytes()));
        key.interestOps(SelectionKey.OP_READ);
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
}
