package com.borcasergiu.vitaly;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinGson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;

/*
  REST API + single-page dashboard. Shares its FACEIT client, cache, database and
  period aggregator with the Discord bot (all injected from Main).
 */
public class WebDashboard {

    // FACEIT nicknames: letters, digits, '-' and '_' (3-12 chars today; allow some slack).
    private static final Pattern NICKNAME = Pattern.compile("[A-Za-z0-9_\\-]{2,32}");

    private final FaceitApiClient faceitClient;
    private final RoleAnalyzer analyzer;
    private final PlayerRepository db;
    private final PeriodStatsService periodStats;
    private final Gson gson;
    private final boolean trustProxy;

    private final RateLimiter playerLimiter = new RateLimiter(20, 60_000);
    private final Semaphore periodSlots = new Semaphore(3); // concurrent period aggregations

    public WebDashboard(FaceitApiClient faceitClient, RoleAnalyzer analyzer, PlayerRepository db,
                        PeriodStatsService periodStats, Gson gson, boolean trustProxy) {
        this.faceitClient = faceitClient;
        this.analyzer = analyzer;
        this.db = db;
        this.periodStats = periodStats;
        this.gson = gson;
        this.trustProxy = trustProxy;
    }

    public Javalin startServer(int port) {
        Javalin app = Javalin.create(cfg -> cfg.jsonMapper(new JavalinGson(gson, false)));

        app.get("/", ctx -> ctx.html(DashboardHtml.getLayout()));
        app.get("/health", ctx -> ctx.result("ok"));
        app.get("/api/leaderboard", ctx -> ctx.json(db.getTopPlayers(15)));
        app.get("/api/player/{nickname}", this::handlePlayer);

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            error(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error.");
        });

        app.start(port);
        System.out.println("Vitaly Web Engine ONLINE: http://localhost:" + app.port());
        return app;
    }

    private void handlePlayer(Context ctx) {
        String nickname = ctx.pathParam("nickname");
        if (!NICKNAME.matcher(nickname).matches()) {
            error(ctx, HttpStatus.BAD_REQUEST, "Invalid nickname.");
            return;
        }

        int days;
        try {
            String daysParam = ctx.queryParam("days");
            days = (daysParam == null || daysParam.isEmpty()) ? 0 : Integer.parseInt(daysParam);
        } catch (NumberFormatException e) {
            days = -1;
        }
        if (days != 0 && !PeriodStatsService.ALLOWED_DAYS.contains(days)) {
            error(ctx, HttpStatus.BAD_REQUEST, "days must be one of 0, 30, 90, 180, 365.");
            return;
        }

        if (!playerLimiter.tryAcquire(clientKey(ctx))) {
            error(ctx, HttpStatus.TOO_MANY_REQUESTS, "Too many lookups. Wait a minute and try again.");
            return;
        }

        FaceitProfile profile = faceitClient.getPlayerProfile(nickname);
        if (profile == null) {
            error(ctx, HttpStatus.NOT_FOUND, "Player not found.");
            return;
        }

        // Lifetime stats are always needed: the role and the entry/clutch/utility numbers
        // only exist as lifetime aggregates on FACEIT.
        Cs2Stats lifetime = parseStats(faceitClient.getPlayerStats(profile.id));
        if (lifetime == null) {
            error(ctx, HttpStatus.BAD_GATEWAY, "Stats unavailable.");
            return;
        }

        Cs2Stats shown = lifetime;
        PeriodStats period = null;
        if (days > 0) {
            if (!periodSlots.tryAcquire()) {
                error(ctx, HttpStatus.SERVICE_UNAVAILABLE, "Server busy. Try again in a few seconds.");
                return;
            }
            try {
                period = periodStats.aggregate(profile.id, days);
            } finally {
                periodSlots.release();
            }
            if (period == null) {
                error(ctx, HttpStatus.BAD_GATEWAY, "FACEIT didn't respond. Try again in a moment.");
                return;
            }
            if (period.maps() == 0) {
                error(ctx, HttpStatus.NOT_FOUND, "No matches found in the last " + days + " days.");
                return;
            }
            shown = period.toCs2Stats();
        } else {
            // Only lifetime numbers go on the leaderboard; a hot 30-day streak shouldn't overwrite them.
            db.savePlayer(profile.nickname, profile.elo, lifetime.getKd(), lifetime.getWinRate());
        }

        String roleResult = analyzer.determineRole(lifetime);
        ctx.json(buildPlayerResponse(profile, shown, lifetime, roleResult, days, period));
    }

    private Cs2Stats parseStats(String rawJson) {
        if (rawJson == null) return null;
        try {
            return gson.fromJson(rawJson, Cs2Stats.class);
        } catch (JsonParseException e) {
            return null;
        }
    }

    private String clientKey(Context ctx) {
        if (trustProxy) {
            String forwarded = ctx.header("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        }
        return ctx.ip();
    }

    private static void error(Context ctx, HttpStatus status, String message) {
        ctx.status(status).json(Map.of("success", false, "error", message));
    }

    private Map<String, Object> buildPlayerResponse(FaceitProfile profile, Cs2Stats stats, Cs2Stats lifetime,
                                                    String roleResult, int days, PeriodStats period) {
        Map<String, Object> resp = new HashMap<>();

        resp.put("success",  true);
        resp.put("nickname", profile.nickname);
        resp.put("elo",      profile.elo);
        resp.put("level",    profile.level);
        resp.put("avatar",   profile.avatarUrl);
        resp.put("role",     roleResult);
        resp.put("days",     days);
        resp.put("truncated", period != null && period.truncated());

        resp.put("kd",        stats.getKd());
        resp.put("adr",       stats.getAdr());
        resp.put("hs",        stats.getHs());
        resp.put("winRate",   stats.getWinRate());
        resp.put("matches",   stats.getMatches());

        // Lifetime-only metrics (FACEIT doesn't expose them per match)
        resp.put("entry",     Math.round(lifetime.getEntrySuccess() * 100));
        resp.put("clutch1v1", Math.round(lifetime.getClutch1v1()    * 100));
        resp.put("clutch1v2", Math.round(lifetime.getClutch1v2()    * 100));
        resp.put("sniper",    lifetime.getSniperRate());
        resp.put("utility",   lifetime.getUtilityDmg());
        resp.put("flashes",   lifetime.getFlashesPerRound());

        resp.put("wins",       stats.getTotalWins());
        resp.put("streak",     lifetime.getCurrentWinStreak());
        resp.put("bestStreak", lifetime.getLongestWinStreak());
        resp.put("avgKills",   stats.getAvgKills());
        resp.put("aces",       stats.getAces());
        resp.put("quads",      stats.getQuadKills());
        resp.put("triples",    stats.getTripleKills());
        resp.put("totalKills", stats.getTotalKills());
        resp.put("totalHs",    stats.getTotalHeadshots());
        resp.put("mvps",       stats.getMvps());

        List<Map<String, Object>> mapData = new ArrayList<>();
        if (lifetime.segments != null) {
            List<Cs2Stats.Segment> validMaps = new ArrayList<>();
            for (Cs2Stats.Segment s : lifetime.segments) {
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
