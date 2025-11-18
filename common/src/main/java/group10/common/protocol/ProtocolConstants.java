package group10.common.protocol;

public final class ProtocolConstants {
    private ProtocolConstants() {}

    // Login
    public static final String LOGIN = "LOGIN";
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAIL = "LOGIN_FAIL";

    public static final String ONLINE_LIST = "ONLINE_LIST";
    public static final String HEARTBEAT = "HEARTBEAT";
    public static final String HEARTBEAT_ACK = "HEARTBEAT_ACK";


    // invite
    public static final String INVITE = "INVITE";
    public static final String INVITE_RESPONSE = "INVITE_RESPONSE";

    // match
    public static final String START_MATCH = "START_MATCH";
    public static final String START_MATCH_ACK = "START_MATCH_ACK";
    public static final String START_MATCH_TIMEOUT = "START_MATCH_TIMEOUT";
    public static final String START_ROUND = "START_ROUND";
    public static final String REMATCH_REQUEST = "REMATCH_REQUEST";
    public static final String SUBMIT = "SUBMIT";
    public static final String ROUND_RESULT = "ROUND_RESULT";
    public static final String MATCH_RESULT = "MATCH_RESULT";

    // status
    public static final String PING = "PING";
    public static final String PONG = "PONG";
    public static final String ERROR = "ERROR";

    // leaderboard
    public static final String GET_LEADERBOARD = "GET_LEADERBOARD";
    public static final String LEADERBOARD_DATA = "LEADERBOARD_DATA";

    public static final String GET_MATCH_HISTORY = "GET_MATCH_HISTORY";
    public static final String MATCH_HISTORY_DATA = "MATCH_HISTORY_DATA";

}
