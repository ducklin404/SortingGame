package group10.persistence;

import java.sql.*;

public class Database {
    private static final String URL = "jdbc:postgresql://localhost:5432/SortingGame";
    private static final String USER = "postgres";
    private static final String PASSWORD = "abc123";

    static {
        try { Class.forName("org.postgresql.Driver"); }
        catch (ClassNotFoundException e) { throw new RuntimeException(e); }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
