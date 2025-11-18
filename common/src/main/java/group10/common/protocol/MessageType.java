package group10.common.protocol;

public enum MessageType {
    LOGIN,
    LOGIN_SUCCESS,
    LOGIN_FAIL,
    ONLINE_LIST,
    INVITE,
    INVITE_RESPONSE,
    START_MATCH,
    START_ROUND,
    SUBMIT,
    ROUND_RESULT,
    MATCH_RESULT,
    HEARTBEAT,
    HEARTBEAT_ACK,
    PING,
    PONG,
    ERROR,
    UNKNOWN,
    GET_LEADERBOARD,
    LEADERBOARD_DATA;


    public static MessageType fromString(String s) {
        if (s == null) return UNKNOWN;
        try { return MessageType.valueOf(s); }
        catch (IllegalArgumentException e) { return UNKNOWN; }
    }
}
