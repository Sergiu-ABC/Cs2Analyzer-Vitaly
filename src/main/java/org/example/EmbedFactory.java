package org.example;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.EmbedBuilder;
import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EmbedFactory {

    private static final Random random = new Random();

    // -------------------------------------------------------------------------
    // !stats
    // -------------------------------------------------------------------------
    public static EmbedBuilder buildStats(String nickname, FaceitProfile profile, Cs2Stats stats) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(EmbedUtils.levelColor(profile.level));
        embed.setAuthor("📊 Combat Dashboard — " + nickname,
                "https://www.faceit.com/en/players/" + nickname, null);
        embed.setTitle(EmbedUtils.levelBadge(profile.level) + " • " + profile.elo + " ELO");

        if (profile.avatarUrl != null && !profile.avatarUrl.isEmpty())
            embed.setThumbnail(profile.avatarUrl);

        embed.addField("​",
                "```\n  Matches : " + stats.getMatches() +
                        "   Win Rate : " + stats.getWinRate() + "%\n```", false);

        embed.addField("🔫 AIM & FRAGGING",
                EmbedUtils.dot(stats.getKd(), 1.0, 1.3) +
                        " **K/D** `" + String.format("%-5.2f", stats.getKd()) + "` " +
                        EmbedUtils.bar(stats.getKd(), 2.0) + "\n" +

                        EmbedUtils.dot(stats.getAdr(), 70, 90) +
                        " **ADR** `" + String.format("%-5.1f", stats.getAdr()) + "` " +
                        EmbedUtils.bar(stats.getAdr(), 120.0) + "\n" +

                        EmbedUtils.dot(stats.getHs(), 45, 60) +
                        " **Headshot** `" + String.format("%-4.1f", stats.getHs()) + "%` " +
                        EmbedUtils.bar(stats.getHs(), 80.0), false);

        embed.setFooter("🟢 Great 🟡 Average 🔴 Below Average • FACEIT API", null);
        embed.setTimestamp(Instant.now());
        return embed;
    }

    // -------------------------------------------------------------------------
    // !role
    // -------------------------------------------------------------------------
    public static EmbedBuilder buildRole(String nickname, FaceitProfile profile,
                                         Cs2Stats stats, String roleResult,
                                         String requesterName, String requesterAvatar) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(EmbedUtils.levelColor(profile.level));
        embed.setAuthor("🎯 Role Analysis — " + nickname,
                "https://www.faceit.com/en/players/" + nickname, null);
        embed.setTitle(EmbedUtils.levelBadge(profile.level) + " • " + profile.elo + " ELO");

        if (profile.avatarUrl != null && !profile.avatarUrl.isEmpty())
            embed.setThumbnail(profile.avatarUrl);

        embed.addField("​",
                "```ini\n[ K/D: " + stats.getKd() +
                        " ] [ ADR: " + stats.getAdr() +
                        " ] [ Win: " + stats.getWinRate() +
                        "% ] [ HS: " + stats.getHs() + "% ]\n```", false);

        embed.addField("🧠 AI PLAYSTYLE VERDICT", roleResult, false);

        embed.addField("🧬 PLAYER FINGERPRINT",
                "```\n" +
                        " Aggression  " + EmbedUtils.bar(stats.getEntrySuccess(), 0.65) + "\n" +
                        " Accuracy    " + EmbedUtils.bar(stats.getHs(), 80.0) + "\n" +
                        " Utility IQ  " + EmbedUtils.bar(stats.getUtilityDmg(), 40.0) + "\n" +
                        " Clutch      " + EmbedUtils.bar(stats.getClutch1v1(), 0.80) + "\n" +
                        " Dominance   " + EmbedUtils.bar(stats.getKd(), 2.0) + "\n" +
                        "```", false);

        embed.setImage(EmbedUtils.getBannerForRole(roleResult));
        embed.setFooter("Requested by " + requesterName + " • Vitaly AI", requesterAvatar);
        embed.setTimestamp(Instant.now());
        return embed;
    }

    public static StringSelectMenu buildPeriodMenu(String nickname) {
        return StringSelectMenu.create("time_selector:" + nickname)
                .setPlaceholder("Select a time period to analyze...")
                .addOption("Lifetime",      "0",   "Overall lifetime performance")
                .addOption("Last 30 Days",  "30",  "Recent form")
                .addOption("Last 90 Days",  "90",  "Quarterly performance")
                .addOption("Last 6 Months", "180", "Half-year aggregate")
                .addOption("Last Year",     "365", "Full year aggregate")
                .build();
    }
    // -------------------------------------------------------------------------
    // !advanced
    // -------------------------------------------------------------------------
    public static EmbedBuilder buildAdvanced(String nickname, FaceitProfile profile, Cs2Stats stats) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(new Color(138, 43, 226));
        embed.setAuthor("🔬 Deep Career Scan — " + nickname,
                "https://www.faceit.com/en/players/" + nickname, null);
        embed.setTitle(EmbedUtils.levelBadge(profile.level) + " • " + profile.elo + " ELO");

        if (profile.avatarUrl != null && !profile.avatarUrl.isEmpty())
            embed.setThumbnail(profile.avatarUrl);

        embed.addField("🔥 WIN STREAKS & AVERAGES",
                "```yaml\n" +
                        " Current Streak : " + stats.getCurrentWinStreak() + "\n" +
                        " Best Streak    : " + stats.getLongestWinStreak() + "\n" +
                        " Avg Kills/Game : " + stats.getAvgKills() + "\n```", false);

        embed.addField("☠️ MULTI-KILL HIGHLIGHTS",
                "🔴 **Aces (5K)** — `" + stats.getAces() + "`\n" +
                        "🟠 **Quad (4K)** — `" + stats.getQuadKills() + "`\n" +
                        "🟡 **Triple (3K)** — `" + stats.getTripleKills() + "`", true);

        embed.addField("📈 LIFETIME TOTALS",
                "💀 **Kills** — `" + stats.getTotalKills() + "`\n" +
                        "🎯 **Headshots** — `" + stats.getTotalHeadshots() + "`\n" +
                        "⭐ **MVPs** — `" + stats.getMvps() + "`", true);

        embed.setFooter("Vitaly • Deep Scan • FACEIT API", null);
        embed.setTimestamp(Instant.now());
        return embed;
    }

    // -------------------------------------------------------------------------
    // !maps
    // -------------------------------------------------------------------------
    public static EmbedBuilder buildMaps(String nickname, FaceitProfile profile,
                                         List<Cs2Stats.Segment> validMaps) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(new Color(0, 200, 100));
        embed.setAuthor("🗺️ Map Mastery — " + nickname,
                "https://www.faceit.com/en/players/" + nickname, null);

        if (profile.avatarUrl != null && !profile.avatarUrl.isEmpty())
            embed.setThumbnail(profile.avatarUrl);

        Cs2Stats.Segment best1 = validMaps.get(0);
        embed.addField("👑 Best Map: " + best1.getCleanName(),
                "**Win Rate:** " + best1.getWinRate() + "%\n" +
                        "**K/D Ratio:** " + best1.getKd() + "\n" +
                        "**Matches:** " + best1.getMatches(), true);

        if (validMaps.size() > 1) {
            Cs2Stats.Segment best2 = validMaps.get(1);
            embed.addField("🥈 Second Best: " + best2.getCleanName(),
                    "**Win Rate:** " + best2.getWinRate() + "%\n" +
                            "**K/D Ratio:** " + best2.getKd() + "\n" +
                            "**Matches:** " + best2.getMatches(), true);
        }

        embed.addField("​", "━━━━━━━━━━━━━━━━━━━━", false);

        Cs2Stats.Segment worst1 = validMaps.get(validMaps.size() - 1);
        embed.addField("🗑️ Auto-Veto: " + worst1.getCleanName(),
                "**Win Rate:** " + worst1.getWinRate() + "%\n" +
                        "**K/D Ratio:** " + worst1.getKd() + "\n" +
                        "**Matches:** " + worst1.getMatches(), true);

        if (validMaps.size() > 2) {
            Cs2Stats.Segment worst2 = validMaps.get(validMaps.size() - 2);
            embed.addField("⚠️ Weak Link: " + worst2.getCleanName(),
                    "**Win Rate:** " + worst2.getWinRate() + "%\n" +
                            "**K/D Ratio:** " + worst2.getKd() + "\n" +
                            "**Matches:** " + worst2.getMatches(), true);
        }

        embed.setFooter("Only maps with 5+ matches are shown", null);
        return embed;
    }

    // -------------------------------------------------------------------------
    // !compare
    // -------------------------------------------------------------------------
    public static EmbedBuilder buildCompare(String p1Name, FaceitProfile p1, Cs2Stats stats1,
                                            String p2Name, FaceitProfile p2, Cs2Stats stats2) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(new Color(255, 20, 147));
        embed.setTitle("🥊 HEAD-TO-HEAD: " + p1Name + " vs " + p2Name);

        int eloDiff = Math.abs(p1.elo - p2.elo);
        if (eloDiff < 50) {
            embed.setDescription("An incredibly even matchup.");
        } else {
            String favorite = p1.elo > p2.elo ? p1Name : p2Name;
            embed.setDescription("**" + favorite + "** is the favorite on paper (+" + eloDiff + " ELO).");
        }

        embed.addField("🔵 " + p1Name, buildCompareBlock(p1, stats1, p2, stats2), true);
        embed.addField("🔴 " + p2Name, buildCompareBlock(p2, stats2, p1, stats1), true);
        embed.setFooter("Vitaly • The crown 👑 goes to the higher stat", null);
        return embed;
    }

    private static String buildCompareBlock(FaceitProfile pA, Cs2Stats sA,
                                            FaceitProfile pB, Cs2Stats sB) {
        return "**Level:** " + pA.level + " (" + pA.elo + ")\n\n" +
                "**K/D:** "     + EmbedUtils.crown(sA.getKd(), sB.getKd(), false) + "\n" +
                "**ADR:** "     + EmbedUtils.crown(sA.getAdr(), sB.getAdr(), false) + "\n" +
                "**Win %:** "   + EmbedUtils.crown(sA.getWinRate(), sB.getWinRate(), true) + "\n" +
                "**Headshot:** "+ EmbedUtils.crown(sA.getHs(), sB.getHs(), true) + "\n\n" +
                "**Entry %:** " + EmbedUtils.crown(Math.round(sA.getEntrySuccess() * 100),
                Math.round(sB.getEntrySuccess() * 100), true) + "\n" +
                "**Clutch %:** "+ EmbedUtils.crown(Math.round(sA.getClutch1v1() * 100),
                Math.round(sB.getClutch1v1() * 100), true) + "\n" +
                "**Matches:** " + sA.getMatches();
    }

    // -------------------------------------------------------------------------
    // !help
    // -------------------------------------------------------------------------
    public static EmbedBuilder buildHelp() {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("⚡ VITALY — CS2 Intelligence Bot");
        embed.setColor(new Color(255, 165, 0));
        embed.setDescription("Your personal CS2 analyst. Drop a nickname and get a full breakdown.");

        embed.addField("🎯 `!role <nickname>`",   "AI-powered playstyle analysis across 30 unique archetypes.", false);
        embed.addField("📊 `!stats <nickname>`",  "Full combat dashboard — K/D, ADR, headshots, clutches.", false);
        embed.addField("🗺️ `!maps <nickname>`",   "Map mastery breakdown — exposes best maps and auto-vetoes.", false);
        embed.addField("🥊 `!compare <p1> <p2>`", "Head-to-head comparison to prove who is better.", false);
        embed.addField("🔬 `!advanced <nickname>`","Deep career scan — aces, streaks, lifetime kills, MVPs.", false);
        embed.addField("🕒 `!period <nickname>`", "Analyze performance over a custom time window.", false);
        embed.addField("🏆 `!leaderboard`",        "View the server leaderboard ranked by ELO.", false);
        embed.addField("ℹ️ `!info`",               "Meet Vitaly and hear what the bot is all about.", false);

        embed.setFooter("Vitaly • CS2 Role Analyzer", null);
        embed.setTimestamp(Instant.now());
        return embed;
    }

    // -------------------------------------------------------------------------
    // !info
    // -------------------------------------------------------------------------
    public static EmbedBuilder buildInfo() {
        String[] intros = {
                "I don't rush B... I rush FACEIT APIs.",
                "Some players watch demos. I stalk statistics.",
                "I don't need wallhack. I already know your K/D.",
                "Bomb has been planted... and so has your ELO.",
                "I analyze more players than HLTV analysts on caffeine.",
                "Your crosshair placement matters. Your stats matter more.",
                "I don't tilt. I just return worse statistics.",
                "CS2 isn't about luck. It's about numbers."
        };

        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(new Color(255, 120, 0));
        embed.setTitle("🎮 Meet VITALY");
        embed.setDescription("```fix\n" + intros[random.nextInt(intros.length)] + "\n```");

        embed.addField("🤖 WHAT I DO",
                """
                • Fetch FACEIT player statistics instantly
                • Analyze playstyles with AI role detection
                • Compare players head-to-head
                • Expose map weaknesses and comfort picks
                • Track server leaderboard rankings
                """, false);

        embed.addField("⚡ MOST USED COMMANDS",
                """
                `!stats nickname`
                `!role nickname`
                `!compare player1 player2`
                `!maps nickname`
                """, false);

        embed.addField("💀 REAL TALK",
                """
                Bottom fragging?
                I will know.

                20% winrate?
                I will expose it.

                5-stack boosted Level 10?
                Statistical investigation ongoing.
                """, false);

        embed.setFooter("Vitaly • Powered by FACEIT data and bad decisions on Mirage", null);
        embed.setImage("https://media1.tenor.com/m/FwGv-2Vp4P8AAAAC/cs2-counter-strike-2.gif");
        embed.setTimestamp(Instant.now());
        return embed;
    }

    // -------------------------------------------------------------------------
    // !leaderboard
    // -------------------------------------------------------------------------
    public static EmbedBuilder buildLeaderboard(java.util.List<Data_Model.PlayerRecord> topPlayers) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🏆 Server ELO Leaderboard");
        embed.setColor(new Color(255, 215, 0));

        StringBuilder board = new StringBuilder();
        int rank = 1;
        for (Data_Model.PlayerRecord p : topPlayers) {
            String medal = switch (rank) {
                case 1  -> "🥇";
                case 2  -> "🥈";
                case 3  -> "🥉";
                default -> "🔹";
            };
            board.append(medal).append(" **").append(p.nickname).append("**\n")
                    .append("└ **ELO:** ").append(p.elo)
                    .append(" | **K/D:** ").append(p.kd)
                    .append(" | **Win:** ").append(p.winRate).append("%\n\n");
            rank++;
        }

        embed.setDescription(board.toString());
        embed.setFooter("Scanned players are automatically added.", null);
        return embed;
    }

    // -------------------------------------------------------------------------
    // !period result
    // -------------------------------------------------------------------------
    public static EmbedBuilder buildPeriodResult(String nickname, String avatarUrl,
                                                 int days, int validMatches,
                                                 int wins, int totalKills,
                                                 double avgKd, double avgHs) {
        int winRate = (int) Math.round(((double) wins / validMatches) * 100);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(new Color(0, 255, 128));
        embed.setAuthor("🕒 " + days + "-Day Performance — " + nickname, null, avatarUrl);
        embed.setTitle("Aggregated over " + validMatches + " Matches");
        embed.addField("📊 Period Stats",
                "```yaml\n" +
                        " Win Rate : " + winRate + "% (" + wins + "W - " + (validMatches - wins) + "L)\n" +
                        " K/D Ratio: " + String.format("%.2f", avgKd) + "\n" +
                        " HS %     : " + String.format("%.1f", avgHs) + "%\n" +
                        " Avg Kills: " + String.format("%.1f", (double) totalKills / validMatches) + "\n" +
                        "```", false);
        embed.setFooter("Vitaly • Custom Match Aggregator", null);
        embed.setTimestamp(Instant.now());
        return embed;
    }

    // -------------------------------------------------------------------------
    // Error embeds
    // -------------------------------------------------------------------------
    public static EmbedBuilder buildPlayerNotFound(String nickname) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(new Color(220, 50, 50));
        embed.setTitle("❌ Player Not Found");
        embed.setDescription("Could not find **" + nickname + "** on FACEIT.");
        return embed;
    }

    public static EmbedBuilder buildStatsUnavailable(String nickname) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(new Color(220, 50, 50));
        embed.setTitle("❌ Stats Unavailable");
        embed.setDescription("Found **" + nickname + "** but couldn't retrieve their CS2 stats.");
        return embed;
    }
}