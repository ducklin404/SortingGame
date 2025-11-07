package group10.server;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.Socket;
import java.util.UUID;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final PlayerService service = new PlayerService();

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }
    public UUID userId = null;
    @Override
    public void run() {

        try {
            while (true) {
                int len = in.readInt();
                byte[] data = new byte[len];
                in.readFully(data);
                JSONObject msg = new JSONObject(new String(data));
                String type = msg.getString("type");
                JSONObject payload = msg.getJSONObject("payload");

                switch (type) {
                    case "LOGIN_REQUEST" -> handleLogin(payload);
                    case "REGISTER_REQUEST" -> handleRegister(payload);
                    case "FORGOT_PASSWORD_REQUEST" -> handleForgotPassword(payload);
                    case "PLAYER_LIST_REQUEST" -> handlePlayerList(payload);
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Client disconnected");
        } finally {
            if(userId != null){
                service.logout(userId);
            }
            try{
                socket.close();
            }catch (IOException ignored){}
        }
    }

    private void handleLogin(JSONObject p) throws IOException {
        var result = service.login(p.getString("username"), p.getString("password"));
        JSONObject res = new JSONObject().put("type", "LOGIN_RESPONSE");
        JSONObject data = new JSONObject();
        if (result.isPresent()) {
            JSONObject info = result.get();
            userId = UUID.fromString(info.getString("id"));
            data.put("success", true);
            data.put("displayName", info.getString("displayName"));
            data.put("id", info.getString("id"));
        } else {
            data.put("success", false);
        }
        res.put("payload", data);
        send(res.toString());
    }

    private void handleRegister(JSONObject p) throws IOException {
        boolean ok = service.register(p.getString("username"), p.getString("display_name"), p.getString("password"));
        JSONObject res = new JSONObject()
                .put("type", "REGISTER_RESPONSE")
                .put("payload", new JSONObject().put("success", ok));
        send(res.toString());
    }

    private void handleForgotPassword(JSONObject p) throws IOException {
        var result = service.forgotPassword(p.getString("username"));
        JSONObject res = new JSONObject();
        res.put("type", "NEW_PASSWORD_RESPONSE");
        JSONObject data = new JSONObject();
        data.put("success", result.isPresent());
        data.put("new_password", result.orElse(""));
        res.put("payload", data);
        send(res.toString());
    }

    private void handlePlayerList(JSONObject p) throws IOException {
        JSONArray list = service.getActivePlayer(p.getString("userId"));
        JSONObject res = new JSONObject();
        res.put("type", "PLAYER_LIST_RESPONSE");
        JSONObject datas = new JSONObject();
        datas.put("players", list);
        res.put("payload", datas);
        send(res.toString());
    }


    private void send(String json) throws IOException {
        byte[] bytes = json.getBytes("UTF-8");
        out.writeInt(bytes.length);
        out.write(bytes);
        out.flush();
    }
}
