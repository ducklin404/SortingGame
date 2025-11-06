package group10.common;

/**
 * Lớp Protocol chứa các hằng số và quy ước dùng chung giữa client và server.
 * Mục tiêu là đảm bảo hai bên "nói cùng một ngôn ngữ".
 */
public class Protocol {

    /* Cấu hình mạng */
    public static final String SERVER_HOST = "localhost";  // Địa chỉ server (hoặc IP thật)
    public static final int SERVER_PORT = 2206;            // Cổng TCP để client kết nối

    /*  Mã hóa dữ liệu */
    public static final String CHARSET = "UTF-8";          // Đảm bảo JSON truyền đi không lỗi font
    public static final int BUFFER_SIZE = 4096;            // Dung lượng tối đa cho mỗi gói tin

    /* Timeout kết nối (ms) */
    public static final int SOCKET_TIMEOUT = 10000;        // 10 giây — tránh treo socket

    /* Cấu hình giao thức */
    public static final String TERMINATOR = "\n";          // Dấu kết thúc mỗi message TCP
    public static final String CONTENT_TYPE = "application/json";

    /*  Các khóa JSON chuẩn (để thống nhất cách đóng gói dữ liệu) */
    public static final String KEY_TYPE = "type";
    public static final String KEY_PAYLOAD = "payload";

    /*  Các endpoint logic — chỉ là gợi ý, dùng khi định danh hành động cụ thể */
    public static final String CMD_GET_LEADERBOARD = "GET_LEADERBOARD";
    public static final String CMD_UPDATE_RESULT = "UPDATE_RESULT";
    public static final String CMD_LOGIN = "LOGIN";

    private Protocol() {
        //  Ngăn việc tạo đối tượng Protocol — chỉ dùng static
    }
}
