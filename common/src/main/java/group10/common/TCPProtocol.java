package group10.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Helper utilities for sending and receiving framed JSON messages over TCP sockets.
 * Each message is encoded as:
 *   [4-byte big-endian length][UTF-8 JSON payload]
 */
public final class TCPProtocol {

    private TCPProtocol() {
        // Utility class
    }

    public static void sendMessage(OutputStream out, Message message) throws IOException {
        if (out == null) {
            throw new IllegalArgumentException("OutputStream cannot be null");
        }
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }

        byte[] data = message.toJson().getBytes(Protocol.CHARSET);
        DataOutputStream dos = out instanceof DataOutputStream dataOutputStream
                ? dataOutputStream
                : new DataOutputStream(out);

        dos.writeInt(data.length);
        dos.write(data);
        dos.flush();
    }

    public static Message receiveMessage(InputStream in) throws IOException {
        if (in == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }

        DataInputStream dis = in instanceof DataInputStream dataInputStream
                ? dataInputStream
                : new DataInputStream(in);

        int length;
        try {
            length = dis.readInt();
        } catch (EOFException eof) {
            return null; // Stream closed cleanly
        }

        if (length <= 0) {
            throw new IOException("Invalid message length: " + length);
        }

        byte[] buffer = new byte[length];
        dis.readFully(buffer);

        String json = new String(buffer, Protocol.CHARSET);
        return Message.fromJson(json);
    }

    public static void closeSocket(Socket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
