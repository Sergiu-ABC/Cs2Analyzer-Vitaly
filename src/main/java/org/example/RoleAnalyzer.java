package org.example;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class RoleAnalyzer {
    private static class Role implements Comparable<Role> {
        String name;
        String emoji;
        String description;
        double score;

        public Role(String name, String emoji, String description, double score) {
            this.name = name;
            this.emoji = emoji;
            this.description = description;
            this.score = Math.min(100.0, Math.max(0.0, score));
        }

        @Override
        public int compareTo(Role other) {
            return Double.compare(other.score, this.score);
        }
    }

    private double calc(double actual, double target) {
        return Math.min(100.0, (actual / target) * 100.0);
    }

    public String determineRole(Cs2Stats stats) {
        double kd = stats.getKd();
        double adr = stats.getAdr();
        double hs = stats.getHs();
        double win = stats.getWinRate();
        double entry = stats.getEntrySuccess();
        double sniper = stats.getSniperRate();
        double clutch1v2 = stats.getClutch1v2();
        double clutch1v1 = stats.getClutch1v1();
        double util = stats.getUtilityDmg();
        double flashes = stats.getFlashesPerRound();
        int matches = stats.getMatches();

        List<Role> roles = new ArrayList<>();
        roles.add(new Role("Main AWPer", "🎯", "The dedicated sniper. You lock down map control and hold angles.", calc(sniper, 0.35)));
        roles.add(new Role("Aggressive AWPer", "🦅", "You buy the big gun and hold W. You entry frag with a sniper.", (calc(sniper, 0.25) * 0.5) + (calc(entry, 0.55) * 0.5)));
        roles.add(new Role("Supportive AWPer", "🔭", "You hold the flank and throw utility while sniping from afar.", (calc(sniper, 0.20) * 0.5) + (calc(flashes, 0.6) * 0.5)));
        roles.add(new Role("Hybrid AWPer", "🔫", "Comfortable with a rifle, but lethal when someone drops you the AWP.", (calc(sniper, 0.15) * 0.5) + (calc(adr, 80) * 0.5)));
        roles.add(new Role("Aggressive Entry", "💣", "First one in. You crack open bomb sites and create space.", (calc(entry, 0.55) * 0.5) + (calc(adr, 90) * 0.5)));
        roles.add(new Role("Space Creator", "🚀", "You run in first, take the bullets, and let your team trade you.", (calc(entry, 0.50) * 0.6) + Math.max(0, (1.1 - kd) * 100 * 0.4)));
        roles.add(new Role("First Blood Specialist", "🩸", "You always find the opening duel of the round, and you usually win it.", calc(entry, 0.60)));
        roles.add(new Role("Glass Cannon", "💥", "Massive damage, but you die a lot. High risk, high reward.", (calc(adr, 95) * 0.6) + Math.max(0, (1.0 - kd) * 100 * 0.4)));
        roles.add(new Role("Trade Fragger", "🔄", "The reliable second-man-in. You clean up the kills after the entry dies.", (calc(kd, 1.15) * 0.5) + (calc(adr, 85) * 0.5)));
        roles.add(new Role("Headshot Machine", "🤖", "Pure mechanical skill. You click heads faster than they can react.", calc(hs, 65.0)));
        roles.add(new Role("The Technician", "⚙️", "Perfect aim combined with perfect utility usage.", (calc(hs, 55.0) * 0.5) + (calc(util, 25) * 0.5)));
        roles.add(new Role("Site Anchor", "⚓", "Immovable object. You hold the site alone while teammates rotate.", (calc(kd, 1.1) * 0.4) + (calc(win, 53) * 0.3) + Math.max(0, (0.4 - entry) * 100 * 0.3)));
        roles.add(new Role("Lurker", "🐍", "Sneaky and isolated. You cut off rotations and hit them in the back.", (calc(kd, 1.15) * 0.6) + Math.max(0, (0.4 - entry) * 100 * 0.4)));
        roles.add(new Role("Clutch Minister", "🧊", "Ice in your veins. The round isn't over if you are still alive.", (calc(clutch1v1, 0.65) * 0.5) + (calc(clutch1v2, 0.25) * 0.5)));
        roles.add(new Role("The Closer", "🚪", "You clean up the mess. High clutch rate and solid survival.", (calc(clutch1v1, 0.60) * 0.5) + (calc(kd, 1.2) * 0.5)));
        roles.add(new Role("Retake Specialist", "🏰", "You thrive when the bomb is down and you have to retake the site.", (calc(clutch1v2, 0.20) * 0.5) + (calc(util, 25) * 0.5)));
        roles.add(new Role("Hard Support", "🩹", "The unsung hero. You throw the perfect flashes so your star can shine.", (calc(flashes, 0.8) * 0.5) + (calc(util, 25) * 0.5)));
        roles.add(new Role("Utility Mastermind", "🧠", "You know every lineup. You win rounds with grenades, not just bullets.", (calc(util, 30) * 0.6) + (calc(win, 54) * 0.4)));
        roles.add(new Role("Flashbang Demon", "😎", "Your enemies are constantly blind. You throw an absurd amount of flashes.", calc(flashes, 1.0)));


        roles.add(new Role("The IGL", "🗣️", "You might not top frag, but your teams always seem to win.", (calc(win, 55) * 0.6) + (calc(util, 20) * 0.4)));
        roles.add(new Role("1v9 Carry", "👑", "You put the entire team on your back and drag them to victory.", (calc(kd, 1.4) * 0.4) + (calc(adr, 95) * 0.3) + (calc(win, 58) * 0.3)));
        roles.add(new Role("Pug Stomper", "🦍", "You hold W and kill everyone. Pure statistical dominance.", (calc(kd, 1.35) * 0.5) + (calc(adr, 95) * 0.5)));
        roles.add(new Role("Seasoned Veteran", "👴", "You have seen it all. You win through sheer thousands of hours of experience.", calc(matches, 4000)));


        roles.add(new Role("Stat Padder", "📈", "You have great stats, but your win rate is surprisingly low. Empty calories.", (calc(kd, 1.2) * 0.6) + Math.max(0, (48 - win) * 100 * 0.4)));
        roles.add(new Role("Passive / Baiter", "🎣", "You survive a lot, but you let your teammates do the hard work.", (calc(kd, 1.15) * 0.5) + Math.max(0, (75 - adr) * 100 * 0.5)));
        roles.add(new Role("Eco Farmer", "🌾", "High K/D, lower ADR. You only get kills when the enemy has pistols.", (calc(kd, 1.2) * 0.6) + Math.max(0, (70 - adr) * 100 * 0.4)));
        roles.add(new Role("The Pacifist", "🕊️", "You barely do damage, but somehow your team wins. The ultimate good luck charm.", (calc(win, 54) * 0.5) + Math.max(0, (65 - adr) * 100 * 0.5)));
        roles.add(new Role("Cannon Fodder", "💀", "You run in, miss your shots, and die instantly.", Math.max(0, (0.9 - kd) * 100 * 0.5) + Math.max(0, (0.4 - entry) * 100 * 0.5)));
        roles.add(new Role("New Blood", "🌱", "You are just getting started on your FACEIT journey.", Math.max(0, (300 - matches) / 300.0 * 100)));

        roles.add(new Role("Versatile Flex", "🧩", "The glue of the team. You can pick up any gun and play any position.", (calc(kd, 1.05) * 0.25) + (calc(adr, 75) * 0.25) + (calc(win, 50) * 0.25) + (calc(hs, 50) * 0.25)));

        Collections.sort(roles);

        Role top1 = roles.get(0);
        Role top2 = roles.get(1);
        Role top3 = roles.get(2);

        StringBuilder result = new StringBuilder();

        if (top1.score > (top2.score + 12)) {
            result.append(top1.emoji).append(" **Dominant Role: ").append(top1.name).append("**\n");
            result.append(top1.description);
        } else {
            result.append(top1.emoji).append(" **Primary: ").append(top1.name).append("**\n");
            result.append(top1.description).append("\n\n");

            result.append(top2.emoji).append(" **Secondary: ").append(top2.name).append("**\n");
            result.append(top2.description);

            if (top2.score - top3.score < 8 && top3.score > 50) {
                result.append("\n\n").append(top3.emoji).append(" **Also Excels As: ").append(top3.name).append("**\n");
                result.append(top3.description);
            }
        }

        return result.toString();
    }

}