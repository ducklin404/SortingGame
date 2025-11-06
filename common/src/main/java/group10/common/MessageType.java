package group10.common;

/**
 * Liệt kê tất cả các loại thông điệp (message type) mà client và server có thể trao đổi.
 * Giúp hệ thống phân biệt hành động cụ thể khi nhận được một gói tin.
 */
public enum MessageType {

    /* Đăng nhập / Phiên */
    LOGIN,              // Client gửi thông tin đăng nhập
    LOGIN_SUCCESS,      // Server phản hồi đăng nhập thành công
    LOGIN_FAILED,       // Server phản hồi đăng nhập thất bại
    LOGOUT,             // Client yêu cầu đăng xuất
    HEARTBEAT,          // Gói tin giữ kết nối (ping/pong)
    ONLINE_LIST,        // Server gửi danh sách người chơi đang online

    /*  Mời & Ghép cặp */
    INVITE,             // Người chơi gửi lời mời thách đấu
    INVITE_RESPONSE,    // Người chơi phản hồi OK/REJECT
    MATCH_START,        // Server thông báo bắt đầu trận
    MATCH_END,          // Server thông báo kết thúc trận

    /*  Game & Round */
    START_ROUND,        // Server gửi đề bài (chuỗi/số cần sắp xếp)
    SUBMIT,             // Client gửi kết quả nộp bài
    ROUND_RESULT,       // Server trả kết quả từng round
    FINAL_RESULT,       // Server trả tổng kết cả trận

    /*  Bảng xếp hạng */
    GET_LEADERBOARD,    // Client yêu cầu lấy BXH
    LEADERBOARD_DATA,   // Server phản hồi danh sách người chơi

    /*  Cập nhật kết quả */
    UPDATE_RESULT,      // Server lưu điểm mới vào DB

    /* ️ Báo lỗi / Thông báo hệ thống */
    ERROR,              // Server gửi lỗi (ví dụ: command không hợp lệ)
    INFO,               // Thông báo thông tin chung
    DISCONNECT          // Ngắt kết nối
}
