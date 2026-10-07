package org.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/*
  Builds time-window stats ("last 30 days" etc.) by downloading each match scoreboard
  and summing the player's numbers. This used to be copy-pasted three times
  (bot, unused PeriodCommand, web dashboard) with slightly different bugs in each copy.

  Scoreboards are fetched on a small dedicated pool, not the JVM-wide common pool,
  so one heavy request can't starve the rest of the app or burst past FACEIT's rate limit.
 */
public class PeriodStatsService {

    public static final Set<Integer> ALLOWED_DAYS = Set.of(30, 90, 180, 365);
    public static final int MAX_MATCHES = 100;

    private final FaceitApiClient faceit;
    private final ExecutorService pool;

    public PeriodStatsService(FaceitApiClient faceit) {
        this.faceit = faceit;
        this.pool = Executors.newFixedThreadPool(6, r -> {
            Thread t = new Thread(r, "faceit-match-fetcher");
            t.setDaemon(true);
            return t;
        });
    }

    /*
      Returns null if FACEIT could not be reached, otherwise the aggregate
      (with maps() == 0 when the player has no parsable matches in the window).
     */
    public PeriodStats aggregate(String playerId, int days) {
        if (!ALLOWED_DAYS.contains(days)) {
            throw new IllegalArgumentException("Unsupported period: " + days);
        }
        long toUnix = Instant.now().getEpochSecond();
        long fromUnix = toUnix - days * 86_400L;

        // Ask for one extra match so we can tell the caller the window was truncated.
        JsonArray history = faceit.getPlayerMatchHistoryByDate(playerId, fromUnix, toUnix, MAX_MATCHES + 1);
        if (history == null) return null;

        boolean truncated = history.size() > MAX_MATCHES;
        List<Future<JsonObject>> futures = new ArrayList<>();
        for (int i = 0; i < Math.min(history.size(), MAX_MATCHES); i++) {
            JsonElement matchId = history.get(i).getAsJsonObject().get("match_id");
            if (matchId == null) continue;
            String id = matchId.getAsString();
            futures.add(pool.submit(() -> faceit.getMatchStats(id)));
        }

        Accumulator acc = new Accumulator();
        for (Future<JsonObject> f : futures) {
            try {
                acc.addMatch(f.get(), playerId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                System.err.println("Match fetch failed: " + e.getMessage());
            }
        }
        return acc.build(days, truncated);
    }

    /*
      Sums one player's numbers across scoreboards. Package-private so it can be unit-tested
      against JSON fixtures without the network.
     */
    static final class Accumulator {
        int maps, wins, kills, deaths, headshots, mvps, triples, quads, aces, adrMaps;
        double adrSum;

        void addMatch(JsonObject matchStats, String playerId) {
            if (matchStats == null || !matchStats.has("rounds")) return;

            // A BO3 has one entry per map in "rounds"; count every map, not just the first.
            for (JsonElement roundEl : matchStats.getAsJsonArray("rounds")) {
                JsonObject playerStats = findPlayer(roundEl.getAsJsonObject(), playerId);
                if (playerStats != null) addMap(playerStats);
            }
        }

        private static JsonObject findPlayer(JsonObject round, String playerId) {
            JsonArray teams = round.getAsJsonArray("teams");
            if (teams == null) return null;
            for (JsonElement teamEl : teams) {
                JsonArray players = teamEl.getAsJsonObject().getAsJsonArray("players");
                if (players == null) continue;
                for (JsonElement playerEl : players) {
                    JsonObject p = playerEl.getAsJsonObject();
                    JsonElement id = p.get("player_id");
                    if (id != null && playerId.equals(id.getAsString())) {
                        return p.getAsJsonObject("player_stats");
                    }
                }
            }
            return null;
        }

        private void addMap(JsonObject s) {
            // Parse everything first, then commit, so a malformed scoreboard
            // can't leave the totals half-updated.
            try {
                int k = intStat(s, "Kills");
                int d = intStat(s, "Deaths");
                int hs = intStat(s, "Headshots");
                boolean won = "1".equals(s.get("Result").getAsString());
                int mvp = optIntStat(s, "MVPs");
                int k3 = optIntStat(s, "Triple Kills");
                int k4 = optIntStat(s, "Quadro Kills");
                int k5 = optIntStat(s, "Penta Kills");
                Double adr = s.has("ADR") ? Double.parseDouble(s.get("ADR").getAsString()) : null;

                kills += k; deaths += d; headshots += hs;
                mvps += mvp; triples += k3; quads += k4; aces += k5;
                if (won) wins++;
                if (adr != null) { adrSum += adr; adrMaps++; }
                maps++;
            } catch (RuntimeException ignored) {
                // Scoreboard missing core fields: skip this map entirely.
            }
        }

        private static int intStat(JsonObject s, String key) {
            return Integer.parseInt(s.get(key).getAsString());
        }

        private static int optIntStat(JsonObject s, String key) {
            return s.has(key) ? intStat(s, key) : 0;
        }

        PeriodStats build(int days, boolean truncated) {
            return new PeriodStats(days, maps, wins, kills, deaths, headshots,
                    mvps, triples, quads, aces, adrSum, adrMaps, truncated);
        }
    }
}
