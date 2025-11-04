package group10.persistence;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    private static final String URL = "jdbc:postgresql://localhost:5432/SORTGAME";
    private static final String USER = "postgres";
    private static final String PASSWORD = "HIEU15904"; // đổi bằng mật khẩu thật

    static {
        try {
            Class.forName("org.postgresql.Driver"); // nạp driver PostgreSQL
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Không tìm thấy PostgreSQL JDBC Driver!", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
