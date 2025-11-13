package group10.persistence;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {

    private static final String URL = "jdbc:postgresql://localhost:5432/SORTGAME";
    private static final String USER = "postgres";
    private static final String PASSWORD = "tienan2004"; // đổi bằng mật khẩu thật

    private static final Database INSTANCE = new Database();

    private Database() {
        try {
            Class.forName("org.postgresql.Driver"); // nạp driver PostgreSQL
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Không tìm thấy PostgreSQL JDBC Driver!", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static Database getInstance() {
        return INSTANCE;
    }
}

