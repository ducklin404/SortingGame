package group10.common;

import org.json.JSONObject;
import org.json.JSONArray;

/**
 * Lớp Message đại diện cho một gói tin JSON trao đổi giữa Client và Server.
 * Mỗi message có 2 phần:
 *   - type: loại thông điệp (MessageType)
 *   - payload: nội dung dữ liệu (String / JSON object / JSON array)
 */
public class Message {
    private MessageType type;
    private Object payload;

    public Message(MessageType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public MessageType getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }

    /**
     *  Chuyển Message thành chuỗi JSON để gửi qua socket
     */
    public String toJson() {
        JSONObject obj = new JSONObject();
        obj.put("type", type.toString());

        //  Nếu payload là chuỗi JSON thật → parse lại để không bị escape
        if (payload instanceof String strPayload) {
            try {
                if (strPayload.trim().startsWith("[")) {
                    obj.put("payload", new JSONArray(strPayload)); // mảng JSON
                } else if (strPayload.trim().startsWith("{")) {
                    obj.put("payload", new JSONObject(strPayload)); // object JSON
                } else {
                    obj.put("payload", strPayload); // chuỗi thường
                }
            } catch (Exception e) {
                obj.put("payload", strPayload);
            }
        } else {
            obj.put("payload", payload);
        }

        return obj.toString() + Protocol.TERMINATOR;
    }

    /**
     *  Parse chuỗi JSON nhận được thành đối tượng Message
     */
    public static Message fromJson(String json) {
        JSONObject obj = new JSONObject(json.trim());
        MessageType type = MessageType.valueOf(obj.getString("type"));
        Object payload = obj.opt("payload");
        return new Message(type, payload);
    }

    @Override
    public String toString() {
        return "[Message type=" + type + ", payload=" + payload + "]";
    }
}
