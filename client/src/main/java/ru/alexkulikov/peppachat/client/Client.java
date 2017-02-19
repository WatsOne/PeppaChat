package ru.alexkulikov.peppachat.client;

import ru.alexkulikov.peppachat.shared.SocketUtils;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static java.nio.ByteBuffer.allocate;
import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

public class Client {

    static final int PORT = 10521;
    static final String ADDRESS = "localhost";
    private ByteBuffer buffer = allocate(256);

    private void run() throws Exception {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        Selector selector = Selector.open();
        channel.register(selector, OP_CONNECT);
        channel.connect(new InetSocketAddress(ADDRESS, PORT));
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(2);

        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String line = scanner.nextLine();
                if ("q".equals(line)) {
                    System.exit(0);
                }
                try {
                    queue.put(line);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                SelectionKey key = channel.keyFor(selector);
                key.interestOps(OP_WRITE);
                selector.wakeup();
            }
        }).start();

        Iterator<SelectionKey> socketIterator;
        SelectionKey socketKey;

        while (channel.isOpen()) {

            selector.select();
            socketIterator = selector.selectedKeys().iterator();

            while (socketIterator.hasNext()) {
                socketKey = socketIterator.next();
                socketIterator.remove();

                if (socketKey.isConnectable()) {
                    channel.finishConnect();
                    socketKey.interestOps(OP_WRITE);
                } else if (socketKey.isReadable()) {
                    buffer.clear();
                    channel.read(buffer);
                    String message = SocketUtils.getBufferData(buffer);
                    System.out.println("Recieved = " + message);
                } else if (socketKey.isWritable()) {
                    String line = queue.poll();
                    if (line != null) {
                        channel.write(ByteBuffer.wrap(line.getBytes()));
                    }
                    socketKey.interestOps(OP_READ);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new Client().run();
    }
}
