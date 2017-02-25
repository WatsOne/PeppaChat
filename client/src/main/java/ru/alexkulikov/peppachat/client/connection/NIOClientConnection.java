package ru.alexkulikov.peppachat.client.connection;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ru.alexkulikov.peppachat.shared.Message;
import ru.alexkulikov.peppachat.shared.SocketUtils;
import ru.alexkulikov.peppachat.shared.connection.ConnectionEventListener;
import ru.alexkulikov.peppachat.shared.connection.ConnectionException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import static java.nio.ByteBuffer.allocate;
import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

public class NIOClientConnection implements ClientConnection {

    private Selector selector;
    private SocketChannel socket;

    private DataProducer dataProducer;
    private ConnectionEventListener listener;

    private ByteBuffer buffer = allocate(2048);

    @Override
    public void notifyToSend() throws ConnectionException {
        checkSetup();

        SelectionKey key = socket.keyFor(selector);
        key.interestOps(OP_WRITE);
        selector.wakeup();
    }

    @Override
    public void setEventListener(ConnectionEventListener listener) {
        this.listener = listener;
    }

    @Override
    public void setDataProducer(DataProducer dataProducer) {
        this.dataProducer = dataProducer;
    }

    @Override
    public void setup(String host, int port) throws IOException {
        socket = SocketChannel.open();
        socket.configureBlocking(false);

        selector = Selector.open();

        socket.register(selector, OP_CONNECT);
        socket.connect(new InetSocketAddress(host, port));
    }

    @Override
    public void start() throws ConnectionException, IOException {
        checkSetup();

        Iterator<SelectionKey> socketIterator;
        SelectionKey socketKey;

        while (socket.isOpen()) {

            selector.select();
            socketIterator = selector.selectedKeys().iterator();

            while (socketIterator.hasNext()) {
                socketKey = socketIterator.next();
                socketIterator.remove();

                if (socketKey.isConnectable()) {
                    processAccept(socketKey);
                } else if (socketKey.isReadable()) {
                    processRead();
                } else if (socketKey.isWritable()) {
                    processWrite(socketKey);
                }
            }
        }
    }

    private void processAccept(SelectionKey socketKey) throws IOException {
        socket.finishConnect();
        socketKey.interestOps(OP_READ);
    }

    private void processRead() throws IOException {
        buffer.clear();
        int read = 0;
        StringBuilder builder = new StringBuilder();
        while ((read = socket.read(buffer)) > 0) {
            builder.append(SocketUtils.getBufferData(buffer));
        }

        List<Message> messages = new Gson().fromJson(builder.toString(), new TypeToken<List<Message>>(){}.getType());
        messages.forEach(listener::onDataArrived);
    }

    private void processWrite(SelectionKey socketKey) throws IOException {
        String message = dataProducer.getDataToSend();
        if (!StringUtils.isEmpty(message)) {
            socket.write(ByteBuffer.wrap(message.getBytes()));
            socketKey.interestOps(OP_READ);
        }
    }

    @Override
    public void shutDown() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public boolean isAlive() {
        return socket.isOpen() && socket.isConnected();
    }

    private void checkSetup() throws ConnectionException {
        if (socket == null || selector == null) {
            throw new ConnectionException("Connection doesn't setup");
        }
    }
}
