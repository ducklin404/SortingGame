package group10.persistence;

import java.sql.Connection;
import java.sql.SQLException;

public class TestConnection {
    public static void main(String[] args) {
        try (Connection conn = Database.getConnection()) {
            System.out.println("Kết nối thành công tới database SORTGAME!");
        } catch (SQLException e) {
            System.out.println("Kết nối thất bại:");
            e.printStackTrace();
        }
    }
}
