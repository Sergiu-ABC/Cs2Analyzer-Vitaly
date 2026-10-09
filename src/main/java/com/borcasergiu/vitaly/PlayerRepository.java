package com.borcasergiu.vitaly;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/*
  SQLite-backed leaderboard storage.

  One instance is created in Main and shared by the Discord bot and the web dashboard.
  Writes are serialized because SQLite allows a single writer; without this, a bot command
  and a web request saving at the same moment could fail with "database is locked".
 */
public class PlayerRepository {

    private final String url;

    public PlayerRepository(String dbPath) {
        this.url = "jdbc:sqlite:" + dbPath;
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS players (" +
                    "nickname TEXT PRIMARY KEY," +
                    "elo INTEGER," +
                    "kd REAL," +
                    "win_rate REAL" +
                    ");");
        } catch (SQLException e) {
            throw new IllegalStateException("Could not open database at " + dbPath, e);
        }
    }

    private Connection connect() throws SQLException {
        Connection conn = DriverManager.getConnection(url);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA busy_timeout = 5000");
        }
        return conn;
    }

    /*
      Upserts a player under FACEIT's canonical nickname. Older rows saved under a different
      capitalisation of the same name (the old code stored whatever the user typed) are removed
      so one player can't appear on the leaderboard several times.
     */
    public synchronized void savePlayer(String nickname, int elo, double kd, double winRate) {
        String dedupe = "DELETE FROM players WHERE nickname = ? COLLATE NOCASE AND nickname <> ?";
        String upsert = "INSERT INTO players (nickname, elo, kd, win_rate) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT(nickname) DO UPDATE SET " +
                "elo=excluded.elo, kd=excluded.kd, win_rate=excluded.win_rate";

        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            try (PreparedStatement del = conn.prepareStatement(dedupe);
                 PreparedStatement ins = conn.prepareStatement(upsert)) {
                del.setString(1, nickname);
                del.setString(2, nickname);
                del.executeUpdate();

                ins.setString(1, nickname);
                ins.setInt(2, elo);
                ins.setDouble(3, kd);
                ins.setDouble(4, winRate);
                ins.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("DB save error: " + e.getMessage());
        }
    }

    public List<PlayerRecord> getTopPlayers(int limit) {
        List<PlayerRecord> topPlayers = new ArrayList<>();
        String sql = "SELECT nickname, elo, kd, win_rate FROM players ORDER BY elo DESC LIMIT ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    topPlayers.add(new PlayerRecord(
                            rs.getString("nickname"),
                            rs.getInt("elo"),
                            rs.getDouble("kd"),
                            rs.getDouble("win_rate")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("DB fetch error: " + e.getMessage());
        }
        return topPlayers;
    }

    public record PlayerRecord(String nickname, int elo, double kd, double winRate) {}
}
