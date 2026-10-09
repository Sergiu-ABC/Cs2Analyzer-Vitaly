package com.borcasergiu.vitaly;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WebDashboardTest {

    private static final String PROFILE = """
            { "player_id": "id-1", "nickname": "s1mple", "avatar": "",
              "games": { "cs2": { "faceit_elo": 3200, "skill_level": 10 } } }
            """;
    private static final String STATS = """
            { "lifetime": { "Average K/D Ratio": "1.3", "ADR": "88", "Win Rate %": "56", "Matches": "1000",
                            "Entry Success Rate": "0.55", "1v1 Win Rate": "0.6" },
              "segments": [ { "label": "de_mirage", "stats": { "Matches": "40", "Win Rate %": "60", "Average K/D Ratio": "1.4" } } ] }
            """;
    private static final String HISTORY = "{ \"items\": [ { \"match_id\": \"m1\" } ] }";
    private static final String MATCH = "{ \"rounds\": [" +
            PeriodStatsAccumulatorTest.round("id-1", "25", "10", "10", "1", "95") + "] }";

    @TempDir Path dir;
    private FakeFaceit faceit;
    private Javalin app;
    private PlayerRepository db;
    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void start() throws Exception {
        faceit = new FakeFaceit(path -> {
            if (path.startsWith("/players?nickname=s1mple") || path.startsWith("/players?nickname=S1MPLE"))
                return new FakeFaceit.Reply(200, PROFILE);
            if (path.startsWith("/players?")) return new FakeFaceit.Reply(404, null);
            if (path.endsWith("/stats/cs2")) return new FakeFaceit.Reply(200, STATS);
            if (path.contains("/history")) return new FakeFaceit.Reply(200, HISTORY);
            if (path.startsWith("/matches/")) return new FakeFaceit.Reply(200, MATCH);
            return new FakeFaceit.Reply(404, null);
        });
        FaceitApiClient client = new FaceitApiClient("key", faceit.baseUrl());
        db = new PlayerRepository(dir.resolve("w.db").toString());
        app = new WebDashboard(client, new RoleAnalyzer(), db, new PeriodStatsService(client), new Gson(), false)
                .startServer(0);
    }

    @AfterEach
    void stop() {
        app.stop();
        faceit.close();
    }

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + app.port() + path)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void lifetimeLookupReturnsStatsAndSavesCanonicalName() throws Exception {
        HttpResponse<String> res = get("/api/player/S1MPLE");
        assertEquals(200, res.statusCode());
        JsonObject body = JsonParser.parseString(res.body()).getAsJsonObject();
        assertTrue(body.get("success").getAsBoolean());
        assertEquals("s1mple", body.get("nickname").getAsString());
        assertEquals(1.3, body.get("kd").getAsDouble(), 1e-9);
        assertEquals("s1mple", db.getTopPlayers(10).get(0).nickname());
    }

    @Test
    void periodLookupUsesMatchDataAndDoesNotTouchLeaderboard() throws Exception {
        HttpResponse<String> res = get("/api/player/s1mple?days=30");
        assertEquals(200, res.statusCode());
        JsonObject body = JsonParser.parseString(res.body()).getAsJsonObject();
        assertEquals(2.5, body.get("kd").getAsDouble(), 1e-9);
        assertEquals(1, body.get("matches").getAsInt());
        assertEquals(55, body.get("entry").getAsInt(), "lifetime-only stat comes from lifetime data");
        assertTrue(db.getTopPlayers(10).isEmpty());
    }

    @Test
    void rejectsBadInput() throws Exception {
        assertEquals(400, get("/api/player/s1mple?days=abc").statusCode());   // used to be an unhandled 500
        assertEquals(400, get("/api/player/s1mple?days=100000").statusCode());
        assertEquals(400, get("/api/player/%3Cscript%3E").statusCode());
    }

    @Test
    void unknownPlayerIs404() throws Exception {
        assertEquals(404, get("/api/player/nobody").statusCode());
    }

    @Test
    void rateLimitsHeavyClients() throws Exception {
        int last = 0;
        for (int i = 0; i < 21; i++) last = get("/api/player/s1mple").statusCode();
        assertEquals(429, last);
    }

    @Test
    void servesDashboardAndLeaderboard() throws Exception {
        assertTrue(get("/").body().contains("<html"));
        assertEquals(200, get("/api/leaderboard").statusCode());
    }
}
