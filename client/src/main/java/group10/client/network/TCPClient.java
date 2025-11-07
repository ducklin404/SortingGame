package group10.client.network;

import java.io.*;
import java.net.Socket;

public class TCPClient {
    private final Socket socket;
    private final DataOutputStream out;
    private final DataInputStream in;

    public TCPClient(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
    }

    public void send(String json) throws IOException {
        byte[] data = json.getBytes("UTF-8");
        out.writeInt(data.length);
        out.write(data);
        out.flush();
    }

    public String receive() throws IOException {
        int len = in.readInt();
        byte[] data = new byte[len];
        in.readFully(data);
        return new String(data, "UTF-8");
    }

    public void close() throws IOException { socket.close(); }
}
