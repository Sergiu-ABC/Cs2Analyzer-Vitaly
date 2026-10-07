package org.example;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PeriodStatsAccumulatorTest {

    static String round(String me, String kills, String deaths, String hs, String result, String adr) {
        return """
                { "teams": [
                  { "players": [ { "player_id": "other", "player_stats": { "Kills": "30", "Deaths": "5", "Headshots": "20", "Result": "1" } } ] },
                  { "players": [ { "player_id": "%s", "player_stats": {
                        "Kills": "%s", "Deaths": "%s", "Headshots": "%s", "Result": "%s", "ADR": "%s", "MVPs": "3", "Triple Kills": "1" } } ] }
                ] }
                """.formatted(me, kills, deaths, hs, result, adr);
    }

    static JsonObject match(String... rounds) {
        return JsonParser.parseString("{ \"rounds\": [" + String.join(",", rounds) + "] }").getAsJsonObject();
    }

    @Test
    void sumsOnlyTheRequestedPlayer() {
        var acc = new PeriodStatsService.Accumulator();
        acc.addMatch(match(round("me", "20", "10", "10", "1", "90.0")), "me");
        acc.addMatch(match(round("me", "10", "20", "5", "0", "60.0")), "me");
        PeriodStats p = acc.build(30, false);

        assertEquals(2, p.maps());
        assertEquals(1, p.wins());
        assertEquals(30, p.kills());
        assertEquals(1.0, p.kd(), 1e-9);
        assertEquals(50.0, p.hsPercent(), 1e-9);
        assertEquals(75.0, p.avgAdr(), 1e-9);
        assertEquals(6, p.mvps());
    }

    @Test
    void countsEveryMapOfABestOfThree() {
        // Regression: the old code only read rounds[0], dropping maps 2 and 3 of a BO3.
        var acc = new PeriodStatsService.Accumulator();
        acc.addMatch(match(
                round("me", "20", "10", "10", "1", "80"),
                round("me", "15", "15", "5", "0", "70"),
                round("me", "25", "12", "12", "1", "100")), "me");
        PeriodStats p = acc.build(90, false);
        assertEquals(3, p.maps());
        assertEquals(60, p.kills());
        assertEquals(2, p.wins());
    }

    @Test
    void malformedScoreboardIsSkippedWholeNotHalfCounted() {
        // Regression: Kills was added, then Deaths failed to parse, so totals
        // included kills from a match that wasn't counted.
        var acc = new PeriodStatsService.Accumulator();
        acc.addMatch(match(round("me", "20", "oops", "10", "1", "80")), "me");
        acc.addMatch(null, "me");
        acc.addMatch(JsonParser.parseString("{}").getAsJsonObject(), "me");
        PeriodStats p = acc.build(30, false);
        assertEquals(0, p.maps());
        assertEquals(0, p.kills());
    }

    @Test
    void periodConvertsToCs2Stats() {
        var acc = new PeriodStatsService.Accumulator();
        acc.addMatch(match(round("me", "20", "10", "10", "1", "90.0")), "me");
        Cs2Stats s = acc.build(30, false).toCs2Stats();
        assertEquals(2.0, s.getKd(), 1e-9);
        assertEquals(100.0, s.getWinRate(), 1e-9);
        assertEquals(90.0, s.getAdr(), 1e-9);
        assertEquals(1, s.getMatches());
    }
}
