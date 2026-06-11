package database;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

public class DatabaseManager {
    private static final String DB_PATH = System.getProperty("user.home") + "/billiard_data_v14.db";
    private static final String URL = "jdbc:sqlite:" + DB_PATH;

    public static void initializeDatabase() {
        String sqlActiveShift = "CREATE TABLE IF NOT EXISTS active_shift ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "table_id TEXT,"
                + "client_name TEXT,"
                + "start_timestamp TEXT,"
                + "end_timestamp TEXT,"
                + "duration_minutes REAL,"
                + "final_charge REAL"
                + ");";

        String sqlHistoricalArchives = "CREATE TABLE IF NOT EXISTS historical_archives ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "table_id TEXT,"
                + "client_name TEXT,"
                + "start_timestamp TEXT,"
                + "end_timestamp TEXT,"
                + "duration_minutes REAL,"
                + "final_charge REAL"
                + ");";

        String sqlSettings = "CREATE TABLE IF NOT EXISTS settings ("
                + "id INTEGER PRIMARY KEY CHECK (id = 1),"
                + "pin TEXT,"
                + "currency_string TEXT,"
                + "vip_min_toggle INTEGER,"
                + "client_name_toggle INTEGER,"
                + "dark_mode_toggle INTEGER,"
                + "std_count INTEGER,"
                + "vip_count INTEGER,"
                + "std_rate REAL,"
                + "vip_rate REAL,"
                + "vip_min REAL"
                + ");";

        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlActiveShift);
            stmt.execute(sqlHistoricalArchives);
            stmt.execute(sqlSettings);

            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM settings");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO settings (id, pin, currency_string, vip_min_toggle, client_name_toggle, dark_mode_toggle, std_count, vip_count, std_rate, vip_rate, vip_min) "
                        + "VALUES (1, '0000', 'LE', 0, 0, 1, 10, 5, 0.0, 0.0, 0.0)");
            }
        } catch (SQLException e) {}
    }

    public static void performAutoMaintenance() {
        String twelveMonthsAgo = LocalDateTime.now().minusMonths(12).toString();
        String selectSql = "SELECT * FROM historical_archives WHERE start_timestamp < ?";
        String deleteSql = "DELETE FROM historical_archives WHERE start_timestamp < ?";
        
        File csvFile = new File(System.getProperty("user.home") + "/billiard_archive_export.csv");

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement selectStmt = conn.prepareStatement(selectSql);
             PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {

            selectStmt.setString(1, twelveMonthsAgo);
            ResultSet rs = selectStmt.executeQuery();

            boolean hasData = false;
            try (FileWriter fw = new FileWriter(csvFile, true)) {
                while (rs.next()) {
                    hasData = true;
                    fw.append(rs.getString("id")).append(",")
                      .append(rs.getString("table_id")).append(",")
                      .append(rs.getString("client_name")).append(",")
                      .append(rs.getString("start_timestamp")).append(",")
                      .append(rs.getString("end_timestamp")).append(",")
                      .append(String.valueOf(rs.getDouble("duration_minutes"))).append(",")
                      .append(String.valueOf(rs.getDouble("final_charge"))).append("\n");
                }
            } catch (IOException e) {}

            if (hasData) {
                deleteStmt.setString(1, twelveMonthsAgo);
                deleteStmt.executeUpdate();
            }
        } catch (SQLException e) {}
    }
}