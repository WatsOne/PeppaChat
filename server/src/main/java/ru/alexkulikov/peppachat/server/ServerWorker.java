package ru.alexkulikov.peppachat.server;


import ru.alexkulikov.peppachat.shared.SocketUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class ServerWorker implements Runnable {
    private static final int DEFAULT_PORT = 10521;
    private static final String DEFAULT_HOST = "localhost";

    private final ByteBuffer welcomeBuf = ByteBuffer.wrap("Welcome to KulChat!\n".getBytes());
    private ByteBuffer readBuf = ByteBuffer.allocate(256);

    private Selector selector;
    private ServerSocketChannel socket;

    public ServerWorker() throws IOException {
        initialize();
    }

    private void initialize() throws IOException {
        selector = Selector.open();

        socket = ServerSocketChannel.open();
        socket.bind(new InetSocketAddress(DEFAULT_HOST, DEFAULT_PORT));
        socket.configureBlocking(false);

        socket.register(selector, SelectionKey.OP_ACCEPT);
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
        } else  {
            message = key.attachment() + " left the chat.\n";
            clientSocket.close();
        }

        System.out.println("+++ " + message);

        if (message.equals("ww")) {
            write(key);
        }
    }

    @Override
    public void run() {
        try {
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

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        clientChannel.write(ByteBuffer.wrap("HELLO FROM SERVER!".getBytes()));
        key.interestOps(SelectionKey.OP_READ);
    }
}
