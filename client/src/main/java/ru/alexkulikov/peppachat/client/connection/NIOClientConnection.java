package ru.alexkulikov.peppachat.client.connection;

import ru.alexkulikov.peppachat.shared.Message;
import ru.alexkulikov.peppachat.shared.MessageSerializer;
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

import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

public class NIOClientConnection implements ClientConnection {

    private Selector selector;
    private SocketChannel socket;

    private DataProducer dataProducer;
    private ConnectionEventListener listener;

    private MessageSerializer serializer;
    private ByteBuffer buffer = ByteBuffer.allocate(8096);

    @Override
    public void notifyToSend() throws ConnectionException {
        checkSetup();
        SelectionKey key = socket.keyFor(selector);

        if (key == null || !key.isValid()) {
        	return;
        }

        key.interestOps(OP_WRITE);
        selector.wakeup();
    }

    @Override
    public boolean isAlive() {
        return socket.isOpen() && socket.isConnected();
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

        serializer = new MessageSerializer();
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

                if (socketKey == null || !socketKey.isValid()) {
                	continue;
                }

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

    private void processRead() {
        buffer.clear();
        int read;
        StringBuilder builder = new StringBuilder();

        try {
            while ((read = socket.read(buffer)) > 0) {
                builder.append(SocketUtils.getBufferData(buffer));
            }

            if (read < 0) {
                disconnect();
                return;
            }

            List<Message> messages = serializer.getMessages(builder.toString());
            messages.forEach(listener::onDataArrived);
        } catch (IOException e) {
        	if (socket.isOpen()) {
		        disconnect();
	        }
        }
    }

    private void processWrite(SelectionKey socketKey) throws IOException {
        Message message = dataProducer.getDataToSend();
        String data = serializer.serialize(message);
        socket.write(ByteBuffer.wrap(data.getBytes()));
        socketKey.interestOps(OP_READ);
    }

    private void disconnect() {
        listener.onDisconnect(null);
        shutDown();
    }

    @Override
    public void shutDown() {
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Can't close socket");
        }
    }

    private void checkSetup() throws ConnectionException {
        if (socket == null || selector == null) {
            throw new ConnectionException("Connection doesn't setup");
        }
    }
}
