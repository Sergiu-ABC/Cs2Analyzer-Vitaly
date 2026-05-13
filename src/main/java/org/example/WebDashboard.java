package org.example;

import com.google.gson.Gson;
import io.javalin.Javalin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class WebDashboard {

    private final FaceitApiClient faceitClient = new FaceitApiClient();
    private final RoleAnalyzer analyzer        = new RoleAnalyzer();
    private final Data_Model db                = new Data_Model();
    private final Gson gson                    = new Gson();

    public void startServer(int port) {
        Javalin app = Javalin.create().start(port);
        System.out.println("Vitaly Web Engine ONLINE: http://localhost:" + port);

        app.get("/", ctx -> ctx.html(DashboardHtml.getLayout()));

        app.get("/api/leaderboard", ctx -> ctx.json(db.getTopPlayers(15)));

        app.get("/api/player/{nickname}", ctx -> {
            String nickname = ctx.pathParam("nickname");
            String daysParam = ctx.queryParam("days");
            int days = (daysParam != null && !daysParam.isEmpty()) ? Integer.parseInt(daysParam) : 0;

            FaceitProfile profile = faceitClient.getPlayerProfile(nickname);

            if (profile == null) {
                ctx.json(Map.of("success", false, "error", "Player not found."));
                return;
            }

            Cs2Stats finalStats;

            if (days == 0) {
                String rawJson = faceitClient.getPlayerStats(profile.id);
                if (rawJson == null) {
                    ctx.json(Map.of("success", false, "error", "Stats unavailable."));
                    return;
                }
                finalStats = gson.fromJson(rawJson, Cs2Stats.class);
            }
            // --- CUSTOM TIMEFRAME STATS (PARALLELIZED) ---
            else {
                long toUnix = System.currentTimeMillis() / 1000L;
                long fromUnix = toUnix - (days * 86400L);

                com.google.gson.JsonArray history = faceitClient.getPlayerMatchHistoryByDate(profile.id, fromUnix, toUnix);

                if (history == null || history.isEmpty()) {
                    ctx.json(Map.of("success", false, "error", "No matches found in the last " + days + " days."));
                    return;
                }


                AtomicInteger totalKills = new AtomicInteger(0);
                AtomicInteger totalDeaths = new AtomicInteger(0);
                AtomicInteger totalHs = new AtomicInteger(0);
                AtomicInteger wins = new AtomicInteger(0);
                AtomicInteger validMatches = new AtomicInteger(0);
                AtomicInteger totalMvps = new AtomicInteger(0);
                AtomicInteger total3k = new AtomicInteger(0);
                AtomicInteger total4k = new AtomicInteger(0);
                AtomicInteger total5k = new AtomicInteger(0);
                AtomicReference<Double> totalAdr = new AtomicReference<>(0.0);

                // Extract IDs first
                List<String> matchIds = new ArrayList<>();
                for (com.google.gson.JsonElement el : history) {
                    matchIds.add(el.getAsJsonObject().get("match_id").getAsString());
                }

                // Blast requests in parallel using all available CPU cores
                matchIds.parallelStream().forEach(matchId -> {
                    com.google.gson.JsonObject matchStats = faceitClient.getMatchStats(matchId);
                    if (matchStats == null || !matchStats.has("rounds")) return;

                    com.google.gson.JsonArray rounds = matchStats.getAsJsonArray("rounds");
                    if (rounds.isEmpty()) return;

                    com.google.gson.JsonObject roundData = rounds.get(0).getAsJsonObject();
                    com.google.gson.JsonArray teams = roundData.getAsJsonArray("teams");

                    for (com.google.gson.JsonElement teamEl : teams) {
                        com.google.gson.JsonArray players = teamEl.getAsJsonObject().getAsJsonArray("players");
                        boolean foundPlayerInTeam = false;

                        for (com.google.gson.JsonElement playerEl : players) {
                            com.google.gson.JsonObject p = playerEl.getAsJsonObject();
                            if (!p.get("player_id").getAsString().equals(profile.id)) continue;

                            com.google.gson.JsonObject pStats = p.getAsJsonObject("player_stats");
                            try {
                                totalKills.addAndGet(Integer.parseInt(pStats.get("Kills").getAsString()));
                                totalDeaths.addAndGet(Integer.parseInt(pStats.get("Deaths").getAsString()));
                                totalHs.addAndGet(Integer.parseInt(pStats.get("Headshots").getAsString()));
                                if (pStats.has("MVPs")) totalMvps.addAndGet(Integer.parseInt(pStats.get("MVPs").getAsString()));
                                if (pStats.has("Triple Kills")) total3k.addAndGet(Integer.parseInt(pStats.get("Triple Kills").getAsString()));
                                if (pStats.has("Quadro Kills")) total4k.addAndGet(Integer.parseInt(pStats.get("Quadro Kills").getAsString()));
                                if (pStats.has("Penta Kills")) total5k.addAndGet(Integer.parseInt(pStats.get("Penta Kills").getAsString()));
                                if (pStats.has("ADR")) {
                                    double adr = Double.parseDouble(pStats.get("ADR").getAsString());
                                    totalAdr.accumulateAndGet(adr, Double::sum);
                                }

                                if ("1".equals(pStats.get("Result").getAsString())) wins.incrementAndGet();
                                validMatches.incrementAndGet();
                            } catch (Exception ignored) {}

                            foundPlayerInTeam = true;
                            break;
                        }
                        if (foundPlayerInTeam) break;
                    }
                });

                if (validMatches.get() == 0) {
                    ctx.json(Map.of("success", false, "error", "Could not parse match scoreboards."));
                    return;
                }

                // Calculate Averages based on the Atomic counters
                int matches = validMatches.get();
                double avgKd = totalDeaths.get() > 0 ? (double) totalKills.get() / totalDeaths.get() : totalKills.get();
                double avgHs = totalKills.get() > 0 ? ((double) totalHs.get() / totalKills.get()) * 100 : 0;
                double winRate = ((double) wins.get() / matches) * 100;
                double avgAdr = totalAdr.get() > 0 ? (totalAdr.get() / matches) : 0;

                // Build synthetic JSON payload for RoleAnalyzer
                com.google.gson.JsonObject syntheticRoot = new com.google.gson.JsonObject();
                com.google.gson.JsonObject lifetime = new com.google.gson.JsonObject();

                lifetime.addProperty("Matches", String.valueOf(matches));
                lifetime.addProperty("Wins", String.valueOf(wins.get()));
                lifetime.addProperty("Win Rate %", String.format("%.2f", winRate));
                lifetime.addProperty("Average K/D Ratio", String.format("%.2f", avgKd));
                lifetime.addProperty("Average Headshots %", String.format("%.2f", avgHs));
                lifetime.addProperty("Kills", String.valueOf(totalKills.get()));
                lifetime.addProperty("Headshots", String.valueOf(totalHs.get()));
                lifetime.addProperty("MVPs", String.valueOf(totalMvps.get()));
                lifetime.addProperty("Triple Kills", String.valueOf(total3k.get()));
                lifetime.addProperty("Quadro Kills", String.valueOf(total4k.get()));
                lifetime.addProperty("Penta Kills", String.valueOf(total5k.get()));
                if (avgAdr > 0) lifetime.addProperty("ADR", String.format("%.2f", avgAdr));

                syntheticRoot.add("lifetime", lifetime);
                finalStats = gson.fromJson(syntheticRoot.toString(), Cs2Stats.class);
            }

            // Determine role and save
            String roleResult = analyzer.determineRole(finalStats);
            db.savePlayer(nickname, profile.elo, finalStats.getKd(), finalStats.getWinRate());

            Map<String, Object> resp = buildPlayerResponse(nickname, profile, finalStats, roleResult);
            ctx.json(resp);
        });
    }

    private Map<String, Object> buildPlayerResponse(String nickname, FaceitProfile profile,
                                                    Cs2Stats stats, String roleResult) {
        Map<String, Object> resp = new HashMap<>();

        // Identity
        resp.put("success",  true);
        resp.put("nickname", nickname);
        resp.put("elo",      profile.elo);
        resp.put("level",    profile.level);
        resp.put("avatar",   profile.avatarUrl);
        resp.put("role",     roleResult);

        // Core stats
        resp.put("kd",        stats.getKd());
        resp.put("adr",       stats.getAdr());
        resp.put("hs",        stats.getHs());
        resp.put("winRate",   stats.getWinRate());
        resp.put("entry",     Math.round(stats.getEntrySuccess()  * 100));
        resp.put("clutch1v1", Math.round(stats.getClutch1v1()     * 100));
        resp.put("clutch1v2", Math.round(stats.getClutch1v2()     * 100));
        resp.put("sniper",    stats.getSniperRate());
        resp.put("utility",   stats.getUtilityDmg());
        resp.put("flashes",   stats.getFlashesPerRound());
        resp.put("matches",   stats.getMatches());

        // Advanced
        resp.put("wins",       stats.getTotalWins());
        resp.put("streak",     stats.getCurrentWinStreak());
        resp.put("bestStreak", stats.getLongestWinStreak());
        resp.put("avgKills",   stats.getAvgKills());
        resp.put("aces",       stats.getAces());
        resp.put("quads",      stats.getQuadKills());
        resp.put("triples",    stats.getTripleKills());
        resp.put("totalKills", stats.getTotalKills());
        resp.put("totalHs",    stats.getTotalHeadshots());
        resp.put("mvps",       stats.getMvps());

        // Maps
        List<Map<String, Object>> mapData = new ArrayList<>();
        if (stats.segments != null) {
            List<Cs2Stats.Segment> validMaps = new ArrayList<>();
            for (Cs2Stats.Segment s : stats.segments) {
                if (s.getMatches() >= 5) validMaps.add(s);
            }
            validMaps.sort((a, b) -> Double.compare(b.getWinRate(), a.getWinRate()));

            for (Cs2Stats.Segment s : validMaps) {
                mapData.add(Map.of(
                        "name",    s.getCleanName(),
                        "win",     s.getWinRate(),
                        "kd",      s.getKd(),
                        "matches", s.getMatches()
                ));
            }
        }
        resp.put("maps", mapData);

        return resp;
    }
}