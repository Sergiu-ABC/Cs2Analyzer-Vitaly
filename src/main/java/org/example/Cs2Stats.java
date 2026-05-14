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

    private double sumFromSegments(String key) {
        if (segments == null) return 0.0;
        double total = 0;
        for (Segment seg : segments) {
            if (seg.stats != null && seg.stats.containsKey(key)) {
                try {
                    total += Double.parseDouble(seg.stats.get(key));
                } catch (Exception ignored) {}
            }
        }
        return total;
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

    // --- STREAKS (These usually still work in lifetime) ---
    public int getCurrentWinStreak() { return (int) parse("Current Win Streak"); }
    public int getLongestWinStreak() { return (int) parse("Longest Win Streak"); }

    // --- ADVANCED STATS (Now mathematically aggregated from maps) ---
    public int getTotalKills() {
        int sum = (int) sumFromSegments("Kills");
        return sum > 0 ? sum : (int) parse("Kills");
    }

    public int getTotalHeadshots() {
        int sum = (int) sumFromSegments("Headshots");
        return sum > 0 ? sum : (int) parse("Headshots");
    }

    public int getAces() {
        int sum = (int) sumFromSegments("Penta Kills");
        return sum > 0 ? sum : (int) parse("Penta Kills");
    }

    public int getQuadKills() {
        int sum = (int) sumFromSegments("Quadro Kills");
        return sum > 0 ? sum : (int) parse("Quadro Kills");
    }

    public int getTripleKills() {
        int sum = (int) sumFromSegments("Triple Kills");
        return sum > 0 ? sum : (int) parse("Triple Kills");
    }

    public int getMvps() {
        int sum = (int) sumFromSegments("MVPs");
        return sum > 0 ? sum : (int) parse("MVPs");
    }

    public double getAvgKills() {
        int matches = getMatches();
        if (matches > 0) {
            double avg = (double) getTotalKills() / matches;
            return Math.round(avg * 10.0) / 10.0;
        }
        return parse("Average Kills");
    }

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