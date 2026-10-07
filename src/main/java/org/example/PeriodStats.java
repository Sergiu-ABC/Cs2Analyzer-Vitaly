package org.example;

import java.util.HashMap;
import java.util.Map;

/*
  Totals for one player over a time window, aggregated from individual match scoreboards.
  Immutable; built by PeriodStatsService.
 */
public record PeriodStats(int days, int maps, int wins, int kills, int deaths, int headshots,
                          int mvps, int triples, int quads, int aces,
                          double adrSum, int adrMaps, boolean truncated) {

    public double kd()        { return deaths > 0 ? (double) kills / deaths : kills; }
    public double hsPercent() { return kills > 0 ? (double) headshots / kills * 100 : 0; }
    public double winRate()   { return maps > 0 ? (double) wins / maps * 100 : 0; }
    public double avgAdr()    { return adrMaps > 0 ? adrSum / adrMaps : 0; }
    public double avgKills()  { return maps > 0 ? (double) kills / maps : 0; }

    /*
      Exposes the window through the same Cs2Stats API the lifetime views use.
      Stats FACEIT only provides as lifetime aggregates (entry, clutch, utility, sniper)
      are deliberately absent and read as 0.
     */
    public Cs2Stats toCs2Stats() {
        Map<String, Object> lifetime = new HashMap<>();
        lifetime.put("Matches", maps);
        lifetime.put("Wins", wins);
        lifetime.put("Win Rate %", round(winRate()));
        lifetime.put("Average K/D Ratio", round(kd()));
        lifetime.put("Average Headshots %", round(hsPercent()));
        lifetime.put("Kills", kills);
        lifetime.put("Headshots", headshots);
        lifetime.put("MVPs", mvps);
        lifetime.put("Triple Kills", triples);
        lifetime.put("Quadro Kills", quads);
        lifetime.put("Penta Kills", aces);
        if (adrMaps > 0) lifetime.put("ADR", round(avgAdr()));
        return new Cs2Stats(lifetime);
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
