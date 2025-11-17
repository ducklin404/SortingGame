package group10.common.net;

import group10.common.util.JsonUtil;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class LengthPrefixedIO {
    private LengthPrefixedIO() {}

    // Read one envelope JSON string (throws EOFException on closed socket)
    public static String readJson(Socket socket) throws IOException {
        InputStream in = socket.getInputStream();
        DataInputStream dis = new DataInputStream(in);

        // read 4-byte length (big-endian)
        int len;
        try {
            len = dis.readInt();
        } catch (EOFException e) {
            throw e;
        }
        if (len <= 0 || len > 10_000_000) {
            throw new IOException("Invalid message length: " + len);
        }
        byte[] buf = new byte[len];
        dis.readFully(buf);
        return new String(buf, StandardCharsets.UTF_8);
    }

    // Write JSON string as length-prefixed message
    public static void writeJson(Socket socket, String json) throws IOException {
        OutputStream out = socket.getOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        dos.writeInt(bytes.length);
        dos.write(bytes);
        dos.flush();
    }

    // Convenience: serialize object to JSON and send
    public static void writeObject(Socket socket, Object obj) throws IOException {
        String json = JsonUtil.MAPPER.writeValueAsString(obj);
        writeJson(socket, json);
    }
}
