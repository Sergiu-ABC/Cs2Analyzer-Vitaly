package org.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Data_Model {
    private static final String URL = "jdbc:sqlite:vitaly.db";

    public Data_Model() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {

            String sql = "CREATE TABLE IF NOT EXISTS players (" +
                    "nickname TEXT PRIMARY KEY," +
                    "elo INTEGER," +
                    "kd REAL," +
                    "win_rate REAL" +
                    ");";
            stmt.execute(sql);
            System.out.println("🗄️ Database hooked up successfully!");

        } catch (SQLException e) {
            System.out.println("❌ DB Init Error: " + e.getMessage());
        }
    }

    public void savePlayer(String nickname, int elo, double kd, double winRate) {
        String sql = "INSERT INTO players (nickname, elo, kd, win_rate) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT(nickname) DO UPDATE SET " +
                "elo=excluded.elo, kd=excluded.kd, win_rate=excluded.win_rate;";

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nickname);
            pstmt.setInt(2, elo);
            pstmt.setDouble(3, kd);
            pstmt.setDouble(4, winRate);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("❌ DB Save Error: " + e.getMessage());
        }
    }


    public List<PlayerRecord> getTopPlayers(int limit) {
        List<PlayerRecord> topPlayers = new ArrayList<>();
        String sql = "SELECT * FROM players ORDER BY elo DESC LIMIT ?";

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                topPlayers.add(new PlayerRecord(
                        rs.getString("nickname"),
                        rs.getInt("elo"),
                        rs.getDouble("kd"),
                        rs.getDouble("win_rate")
                ));
            }
        } catch (SQLException e) {
            System.out.println("❌ DB Fetch Error: " + e.getMessage());
        }
        return topPlayers;
    }

    public static class PlayerRecord {
        public String nickname;
        public int elo;
        public double kd;
        public double winRate;

        public PlayerRecord(String nickname, int elo, double kd, double winRate) {
            this.nickname = nickname;
            this.elo = elo;
            this.kd = kd;
            this.winRate = winRate;
        }
    }
}