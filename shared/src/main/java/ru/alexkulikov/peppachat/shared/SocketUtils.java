package ru.alexkulikov.peppachat.shared;

import java.nio.ByteBuffer;

public class SocketUtils {

    public static String getBufferData(ByteBuffer buffer) {
        buffer.array();
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        String result = new String(bytes);
        buffer.clear();
        return result;
    }
}
