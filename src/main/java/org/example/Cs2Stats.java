package org.example;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class Cs2Stats {

    @SerializedName("lifetime")
    private Map<String, Object> lifetime;

    @SerializedName("segments")
    public List<Segment> segments;

    private double parse(String key) {
        if (lifetime == null || !lifetime.containsKey(key)) return 0.0;
        try {
            Object value = lifetime.get(key);
            if (value == null) return 0.0;
            return Double.parseDouble(value.toString());
        } catch (Exception e) { return 0.0; }
    }

    public double getKd() { return parse("Average K/D Ratio"); }
    public double getAdr() { return parse("ADR"); }
    public double getWinRate() { return parse("Win Rate %"); }
    public double getHs() { return parse("Average Headshots %"); }
    public double getEntrySuccess() { return parse("Entry Success Rate"); }
    public double getSniperRate() { return parse("Sniper Kill Rate per Round"); }
    public double getClutch1v2() { return parse("1v2 Win Rate"); }
    public double getClutch1v1() { return parse("1v1 Win Rate"); }
    public double getUtilityDmg() { return parse("Utility Damage per Round"); }
    public double getFlashesPerRound() { return parse("Flashes per Round"); }
    public int getMatches() { return (int) parse("Matches"); }

    public int getTotalWins() { return (int) parse("Wins"); }
    public int getCurrentWinStreak() { return (int) parse("Current Win Streak"); }
    public int getLongestWinStreak() { return (int) parse("Longest Win Streak"); }
    public double getAvgKills() { return parse("Average Kills"); }
    public int getTotalKills() { return (int) parse("Kills"); }
    public int getTotalHeadshots() { return (int) parse("Headshots"); }
    public int getAces() { return (int) parse("Penta Kills"); }
    public int getQuadKills() { return (int) parse("Quadro Kills"); }
    public int getTripleKills() { return (int) parse("Triple Kills"); }
    public int getMvps() { return (int) parse("MVPs"); }

    public static class Segment {
        public String label;
        public Map<String, String> stats;

        public int getMatches() {
            try { return Integer.parseInt(stats.get("Matches")); } catch(Exception e){return 0;}
        }
        public double getWinRate() {
            try { return Double.parseDouble(stats.get("Win Rate %")); } catch(Exception e){return 0;}
        }
        public double getKd() {
            try { return Double.parseDouble(stats.get("Average K/D Ratio")); } catch(Exception e){return 0;}
        }
        public String getCleanName() {
            if (label == null) return "Unknown";
            String clean = label.replace("de_", "");
            return clean.substring(0, 1).toUpperCase() + clean.substring(1);
        }
    }
}