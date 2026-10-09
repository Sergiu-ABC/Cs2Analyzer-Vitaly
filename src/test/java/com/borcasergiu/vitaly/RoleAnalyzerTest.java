package com.borcasergiu.vitaly;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RoleAnalyzerTest {

    private final RoleAnalyzer analyzer = new RoleAnalyzer();

    private static Cs2Stats stats(double kd, double adr, double hs, double win, double entry,
                                  double sniper, double util, double flashes, int matches) {
        Map<String, Object> m = new HashMap<>();
        m.put("Average K/D Ratio", kd);
        m.put("ADR", adr);
        m.put("Average Headshots %", hs);
        m.put("Win Rate %", win);
        m.put("Entry Success Rate", entry);
        m.put("Sniper Kill Rate per Round", sniper);
        m.put("Utility Damage per Round", util);
        m.put("Flashes per Round", flashes);
        m.put("1v1 Win Rate", 0.5);
        m.put("1v2 Win Rate", 0.15);
        m.put("Matches", matches);
        return new Cs2Stats(m);
    }

    @Test
    void everyScoreStaysWithin0And100() {
        Cs2Stats extreme = stats(0.2, 10, 5, 10, 0.0, 0.0, 0, 0, 0);
        for (RoleAnalyzer.Role r : analyzer.rank(extreme)) {
            assertTrue(r.score >= 0 && r.score <= 100, r.name + " = " + r.score);
        }
    }

    @Test
    void averagePlayerWithSlightlyLowAdrIsNotAutomaticallyABaiter() {
        // Regression: (75 - adr) * 100 turned a 5-point ADR gap into 250 points,
        // so nearly everyone under 75 ADR got "Passive / Baiter" as a dominant role.
        Cs2Stats average = stats(1.0, 72, 48, 50, 0.48, 0.05, 8, 0.4, 900);
        List<RoleAnalyzer.Role> ranked = analyzer.rank(average);
        RoleAnalyzer.Role baiter = ranked.stream().filter(r -> r.name.equals("Passive / Baiter")).findFirst().orElseThrow();
        assertTrue(baiter.score < 70, "Baiter scored " + baiter.score);
        assertNotEquals("Passive / Baiter", ranked.get(0).name);
    }

    @Test
    void winRateJustUnderThresholdGivesSmallStatPadderPenalty() {
        Cs2Stats s = stats(1.0, 80, 48, 47, 0.5, 0.05, 8, 0.4, 900);
        RoleAnalyzer.Role padder = analyzer.rank(s).stream()
                .filter(r -> r.name.equals("Stat Padder")).findFirst().orElseThrow();
        // calc(kd 1.0 / 1.2) * 0.6 = 50, plus 1 point under 48% -> 10 * 0.4 = 4
        assertEquals(54.0, padder.score, 0.5);
    }

    @Test
    void dedicatedSniperIsRecognised() {
        Cs2Stats awper = stats(1.2, 80, 30, 52, 0.45, 0.40, 5, 0.3, 1500);
        assertEquals("Main AWPer", analyzer.rank(awper).get(0).name);
        assertTrue(analyzer.determineRole(awper).contains("Main AWPer"));
    }

    @Test
    void missingStatsDoNotCrash() {
        assertNotNull(analyzer.determineRole(new Cs2Stats()));
    }
}
