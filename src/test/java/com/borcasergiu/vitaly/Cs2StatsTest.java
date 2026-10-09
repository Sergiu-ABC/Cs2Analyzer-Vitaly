package com.borcasergiu.vitaly;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Cs2StatsTest {

    private static final String JSON = """
            {
              "lifetime": { "Average K/D Ratio": "1.12", "ADR": "81.4", "Matches": "200", "Win Rate %": "53" },
              "segments": [
                { "label": "de_mirage", "stats": { "Matches": "120", "Kills": "2000", "Penta Kills": "2", "Win Rate %": "55" } },
                { "label": "de_inferno", "stats": { "Matches": "80",  "Kills": "1400", "Penta Kills": "1", "Win Rate %": "50" } },
                { "label": "", "stats": { "Matches": "1" } }
              ]
            }
            """;

    @Test
    void parsesFaceitStringNumbers() {
        Cs2Stats s = new Gson().fromJson(JSON, Cs2Stats.class);
        assertEquals(1.12, s.getKd(), 1e-9);
        assertEquals(81.4, s.getAdr(), 1e-9);
        assertEquals(200, s.getMatches());
    }

    @Test
    void aggregatesAcrossMapSegments() {
        Cs2Stats s = new Gson().fromJson(JSON, Cs2Stats.class);
        assertEquals(3400, s.getTotalKills());
        assertEquals(3, s.getAces());
        assertEquals(17.0, s.getAvgKills(), 1e-9);
    }

    @Test
    void cleanMapNames() {
        Cs2Stats s = new Gson().fromJson(JSON, Cs2Stats.class);
        assertEquals("Mirage", s.segments.get(0).getCleanName());
        // Used to throw StringIndexOutOfBoundsException on an empty label
        assertEquals("Unknown", s.segments.get(2).getCleanName());
    }

    @Test
    void missingLifetimeBlockReadsAsZero() {
        Cs2Stats s = new Gson().fromJson("{}", Cs2Stats.class);
        assertEquals(0, s.getKd());
        assertEquals(0, s.getTotalKills());
    }
}
