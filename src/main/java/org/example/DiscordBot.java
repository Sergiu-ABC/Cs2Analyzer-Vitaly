package org.example;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import com.google.gson.Gson;
import java.awt.Color;
import java.time.Instant;
import java.util.List;

public class DiscordBot extends ListenerAdapter {
    private final FaceitApiClient faceitClient = new FaceitApiClient();
    private final RoleAnalyzer analyzer = new RoleAnalyzer();
    private final Data_Model db = new Data_Model(); // <-- THE NEW DATABASE ENGINE
    private final Gson gson = new Gson();


    private String bar(double value, double max) {
        int filled = (int) Math.round((value / max) * 10);
        filled = Math.max(0, Math.min(10, filled));
        return "█".repeat(filled) + "░".repeat(10 - filled);
    }

    private String dot(double value, double good, double great) {
        if (value >= great) return "🟢";
        if (value >= good)  return "🟡";
        return "🔴";
    }

    private String levelBadge(int level) {
        return switch (level) {
            case 10 -> "💎 Level 10";
            case 9  -> "🔴 Level 9";
            case 8  -> "🟠 Level 8";
            case 7  -> "🟡 Level 7";
            case 6  -> "🟢 Level 6";
            case 5  -> "🔵 Level 5";
            case 4  -> "🟣 Level 4";
            case 3  -> "⚪ Level 3";
            case 2  -> "⚫ Level 2";
            default -> "🩶 Level 1";
        };
    }

    private Color levelColor(int level) {
        if (level == 10)      return new Color(0, 230, 255);
        else if (level >= 8)  return new Color(255, 60, 60);
        else if (level >= 6)  return new Color(255, 140, 0);
        else if (level >= 4)  return new Color(255, 220, 0);
        else                  return new Color(120, 120, 140);
    }

    private String crown(double thisVal, double otherVal, boolean isPercentage) {
        String suffix = isPercentage ? "%" : "";
        if (thisVal > otherVal) return "👑 **" + thisVal + suffix + "**";
        if (thisVal == otherVal) return "🤝 **" + thisVal + suffix + "**";
        return thisVal + suffix;
    }



    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        String message = event.getMessage().getContentRaw();

        if (message.equalsIgnoreCase("!help")) {
            sendHelp(event);
        } else if (message.equalsIgnoreCase("!leaderboard")) {
            sendLeaderboard(event);
        } else if (message.startsWith("!role ")) {
            String nickname = message.substring(6).trim();
            sendRole(event, nickname);
        } else if (message.startsWith("!stats ")) {
            String nickname = message.substring(7).trim();
            sendStats(event, nickname);
        } else if (message.startsWith("!advanced ")) {
            String nickname = message.substring(10).trim();
            sendAdvancedStats(event, nickname);
        } else if (message.startsWith("!maps ")) {
            String nickname = message.substring(6).trim();
            sendMaps(event, nickname);
        } else if (message.startsWith("!compare ")) {
            String[] parts = message.substring(9).trim().split("\\s+");
            if (parts.length == 2) {
                sendCompare(event, parts[0], parts[1]);
            } else {
                event.getChannel().sendMessage("❌ Use the format: `!compare [player1] [player2]`").queue();
            }
        }
    }


    private void sendHelp(MessageReceivedEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("⚡  VITALY  —  CS2 Intelligence Bot");
        embed.setColor(new Color(255, 165, 0));
        embed.setDescription("Your personal CS2 analyst. Drop a nickname and get a full breakdown.");

        embed.addField("🎯  `!role <nickname>`", "AI-powered playstyle analysis across 30 unique archetypes.", false);
        embed.addField("📊  `!stats <nickname>`", "Full combat dashboard — K/D, ADR, headshots, clutches.", false);
        embed.addField("🗺️  `!maps <nickname>`", "Map mastery breakdown — Instantly exposes best maps and auto-vetoes.", false);
        embed.addField("🥊  `!compare <p1> <p2>`", "Head-to-head comparison to mathematically prove who is better.", false);
        embed.addField("🔬  `!advanced <nickname>`", "Deep career scan — aces, streaks, lifetime kills, MVPs.", false);
        embed.addField("🏆  `!leaderboard`", "View the Server Leaderboard (Ranked by ELO).", false);

        embed.setFooter("Vitaly • CS2 Role Analyzer", null);
        embed.setTimestamp(Instant.now());
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }


    private void sendLeaderboard(MessageReceivedEvent event) {
        List<Data_Model.PlayerRecord> topPlayers = db.getTopPlayers(10); // Grab top 10

        if (topPlayers.isEmpty()) {
            event.getChannel().sendMessage("📭 The leaderboard is empty! Scan some players with `!stats` or `!role` to add them to the database.").queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🏆 Server ELO Leaderboard");
        embed.setColor(new Color(255, 215, 0)); // Gold

        StringBuilder board = new StringBuilder();
        int rank = 1;
        for (Data_Model.PlayerRecord p : topPlayers) {
            String medal = switch (rank) {
                case 1 -> "🥇";
                case 2 -> "🥈";
                case 3 -> "🥉";
                default -> "🔹";
            };

            board.append(medal).append(" **").append(p.nickname).append("**\n")
                    .append("└ **ELO:** ").append(p.elo)
                    .append(" | **K/D:** ").append(p.kd)
                    .append(" | **Win:** ").append(p.winRate).append("%\n\n");
            rank++;
        }

        embed.setDescription(board.toString());
        embed.setFooter("Scanned players are automatically added to the board.", null);
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }


    private void sendCompare(MessageReceivedEvent event, String p1Name, String p2Name) {
        FaceitProfile p1 = faceitClient.getPlayerProfile(p1Name);
        if (p1 == null) { sendError(event, p1Name); return; }
        String raw1 = faceitClient.getPlayerStats(p1.id);
        if (raw1 == null) { sendStatError(event, p1Name); return; }
        Cs2Stats stats1 = gson.fromJson(raw1, Cs2Stats.class);

        FaceitProfile p2 = faceitClient.getPlayerProfile(p2Name);
        if (p2 == null) { sendError(event, p2Name); return; }
        String raw2 = faceitClient.getPlayerStats(p2.id);
        if (raw2 == null) { sendStatError(event, p2Name); return; }
        Cs2Stats stats2 = gson.fromJson(raw2, Cs2Stats.class);

        db.savePlayer(p1Name, p1.elo, stats1.getKd(), stats1.getWinRate());
        db.savePlayer(p2Name, p2.elo, stats2.getKd(), stats2.getWinRate());

        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(new Color(255, 20, 147));
        embed.setTitle("🥊 HEAD-TO-HEAD: " + p1Name + " vs " + p2Name);

        int eloDiff = Math.abs(p1.elo - p2.elo);
        if (eloDiff < 50) {
            embed.setDescription("An incredibly even matchup! Let's look at the stats.");
        } else {
            String favorite = p1.elo > p2.elo ? p1Name : p2Name;
            embed.setDescription("**" + favorite + "** is the clear favorite on paper (+ " + eloDiff + " ELO).");
        }

        String p1Block =
                "**Level:** " + p1.level + " (" + p1.elo + ")\n\n" +
                        "**K/D:** " + crown(stats1.getKd(), stats2.getKd(), false) + "\n" +
                        "**ADR:** " + crown(stats1.getAdr(), stats2.getAdr(), false) + "\n" +
                        "**Win %:** " + crown(stats1.getWinRate(), stats2.getWinRate(), true) + "\n" +
                        "**Headshot:** " + crown(stats1.getHs(), stats2.getHs(), true) + "\n\n" +
                        "**Entry %:** " + crown(Math.round(stats1.getEntrySuccess() * 100), Math.round(stats2.getEntrySuccess() * 100), true) + "\n" +
                        "**Clutch %:** " + crown(Math.round(stats1.getClutch1v1() * 100), Math.round(stats2.getClutch1v1() * 100), true) + "\n" +
                        "**Matches:** " + stats1.getMatches();

        String p2Block =
                "**Level:** " + p2.level + " (" + p2.elo + ")\n\n" +
                        "**K/D:** " + crown(stats2.getKd(), stats1.getKd(), false) + "\n" +
                        "**ADR:** " + crown(stats2.getAdr(), stats1.getAdr(), false) + "\n" +
                        "**Win %:** " + crown(stats2.getWinRate(), stats1.getWinRate(), true) + "\n" +
                        "**Headshot:** " + crown(stats2.getHs(), stats1.getHs(), true) + "\n\n" +
                        "**Entry %:** " + crown(Math.round(stats2.getEntrySuccess() * 100), Math.round(stats1.getEntrySuccess() * 100), true) + "\n" +
                        "**Clutch %:** " + crown(Math.round(stats2.getClutch1v1() * 100), Math.round(stats1.getClutch1v1() * 100), true) + "\n" +
                        "**Matches:** " + stats2.getMatches();

        embed.addField("🔵 " + p1Name, p1Block, true);
        embed.addField("🔴 " + p2Name, p2Block, true);

        embed.setFooter("Vitaly • The crown 👑 goes to the higher stat", null);
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }


    private void sendStats(MessageReceivedEvent event, String nickname) {
        FaceitProfile profile = faceitClient.getPlayerProfile(nickname);
        if (profile == null) { sendError(event, nickname); return; }

        String rawJson = faceitClient.getPlayerStats(profile.id);
        if (rawJson == null) { sendStatError(event, nickname); return; }

        Cs2Stats stats = gson.fromJson(rawJson, Cs2Stats.class);

        db.savePlayer(nickname, profile.elo, stats.getKd(), stats.getWinRate());

        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(levelColor(profile.level));
        embed.setAuthor("📊  Combat Dashboard  —  " + nickname, "https://www.faceit.com/en/players/" + nickname, null);
        embed.setTitle(levelBadge(profile.level) + "  •  " + profile.elo + " ELO");

        if (profile.avatarUrl != null && !profile.avatarUrl.isEmpty()) embed.setThumbnail(profile.avatarUrl);

        embed.addField("​", "```yaml\n  Matches : " + stats.getMatches() + "   Win Rate : " + stats.getWinRate() + "%\n ", false);

                embed.addField("🔫  AIM & FRAGGING",
                        dot(stats.getKd(), 1.0, 1.3)  + "  **K/D** `" + String.format("%-5.2f", stats.getKd())  + "`  " + bar(stats.getKd(), 2.0)   + "\n" +
                                dot(stats.getAdr(), 70, 90)    + "  **ADR** `" + String.format("%-5.1f", stats.getAdr()) + "`  " + bar(stats.getAdr(), 120.0) + "\n" +
                                dot(stats.getHs(), 45, 60)     + "  **Headshot** `" + String.format("%-4.1f", stats.getHs())  + "%`  " + bar(stats.getHs(), 80.0), false);

        embed.addField("💣  AGGRESSION & UTILITY",
                dot(stats.getEntrySuccess(), 0.45, 0.55)   + "  **Entry Win** `" + Math.round(stats.getEntrySuccess() * 100)  + "%`  " + bar(stats.getEntrySuccess(), 0.7)    + "\n" +
                        dot(stats.getUtilityDmg(), 15, 25)          + "  **Utility Dmg** `" + stats.getUtilityDmg() + "`  " + bar(stats.getUtilityDmg(), 40.0)     + "\n" +
                        dot(stats.getFlashesPerRound(), 0.4, 0.7)   + "  **Flashes/Rnd** `" + stats.getFlashesPerRound() + "`  " + bar(stats.getFlashesPerRound(), 1.0), false);

        embed.addField("🧊  CLUTCH & SURVIVAL",
                dot(stats.getClutch1v1(), 0.45, 0.60)  + "  **1v1 Win** `" + Math.round(stats.getClutch1v1() * 100)  + "%`  " + bar(stats.getClutch1v1(), 0.8)  + "\n" +
                        dot(stats.getClutch1v2(), 0.20, 0.35)  + "  **1v2 Win** `" + Math.round(stats.getClutch1v2() * 100)  + "%`  " + bar(stats.getClutch1v2(), 0.5)  + "\n" +
                        dot(stats.getSniperRate(), 0.10, 0.25) + "  **Sniper KPR** `" + stats.getSniperRate() + "`  " + bar(stats.getSniperRate(), 0.4), false);

        embed.setFooter("🟢 Great  🟡 Average  🔴 Below Average  •  FACEIT API", null);
        embed.setTimestamp(Instant.now());
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }
    private void sendRole(MessageReceivedEvent event, String nickname) {
        FaceitProfile profile = faceitClient.getPlayerProfile(nickname);
        if (profile == null) { sendError(event, nickname); return; }

        String rawJson = faceitClient.getPlayerStats(profile.id);
        if (rawJson == null) { sendStatError(event, nickname); return; }

        Cs2Stats stats = gson.fromJson(rawJson, Cs2Stats.class);
        String roleResult = analyzer.determineRole(stats);

        db.savePlayer(nickname, profile.elo, stats.getKd(), stats.getWinRate());

        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(levelColor(profile.level));
        embed.setAuthor("🎯  Role Analysis  —  " + nickname, "https://www.faceit.com/en/players/" + nickname, null);
        embed.setTitle(levelBadge(profile.level) + "  •  " + profile.elo + " ELO");

        if (profile.avatarUrl != null && !profile.avatarUrl.isEmpty()) embed.setThumbnail(profile.avatarUrl);

        embed.addField("​", "```ini\n[ K/D: " + stats.getKd() + " ]  [ ADR: " + stats.getAdr() + " ]  [ Win: " + stats.getWinRate() + "% ]  [ HS: " + stats.getHs() + "% ]\n```", false);
        embed.addField("🧠  AI PLAYSTYLE VERDICT", roleResult, false);
        embed.addField("🧬  PLAYER FINGERPRINT", "```\n  Aggression  " + bar(stats.getEntrySuccess(), 0.65) + "\n  Accuracy    " + bar(stats.getHs(), 80.0) + "\n  Utility IQ  " + bar(stats.getUtilityDmg(), 40.0) + "\n  Clutch      " + bar(stats.getClutch1v1(), 0.80) + "\n  Dominance   " + bar(stats.getKd(), 2.0) + "\n ", false);
                embed.setImage(getBannerForRole(roleResult));
        embed.setFooter("Requested by " + event.getAuthor().getName() + "  •  Vitaly AI", event.getAuthor().getAvatarUrl());
        embed.setTimestamp(Instant.now());
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    private void sendAdvancedStats(MessageReceivedEvent event, String nickname) {
        FaceitProfile profile = faceitClient.getPlayerProfile(nickname);
        if (profile == null) { sendError(event, nickname); return; }

        String rawJson = faceitClient.getPlayerStats(profile.id);
        if (rawJson == null) { sendStatError(event, nickname); return; }

        Cs2Stats stats = gson.fromJson(rawJson, Cs2Stats.class);
        EmbedBuilder embed = new EmbedBuilder();

        embed.setColor(new Color(138, 43, 226));
        embed.setAuthor("🔬  Deep Career Scan  —  " + nickname, "https://www.faceit.com/en/players/" + nickname, null);
        embed.setTitle(levelBadge(profile.level) + "  •  " + profile.elo + " ELO");

        if (profile.avatarUrl != null && !profile.avatarUrl.isEmpty()) embed.setThumbnail(profile.avatarUrl);

        embed.addField("🔥  WIN STREAKS & AVERAGES", "```yaml\n  Current Streak : " + stats.getCurrentWinStreak() + "\n  Best Streak    : " + stats.getLongestWinStreak() + "\n  Avg Kills/Game : " + stats.getAvgKills() + "\n```", false);
        embed.addField("☠️  MULTI-KILL HIGHLIGHTS", "🔴  **Aces (5K)** — `" + stats.getAces() + "`\n🟠  **Quad (4K)** — `" + stats.getQuadKills() + "`\n🟡  **Triple (3K)** — `" + stats.getTripleKills() + "`", true);
        embed.addField("📈  LIFETIME TOTALS", "💀  **Kills** — `" + stats.getTotalKills() + "`\n🎯  **Headshots** — `" + stats.getTotalHeadshots() + "`\n⭐  **MVPs** — `" + stats.getMvps() + "`", true);

        embed.setFooter("Vitaly  •  Deep Scan  •  FACEIT API", null);
        embed.setTimestamp(Instant.now());
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    private void sendMaps(MessageReceivedEvent event, String nickname) {
        FaceitProfile profile = faceitClient.getPlayerProfile(nickname);
        if (profile == null) { sendError(event, nickname); return; }

        String rawJson = faceitClient.getPlayerStats(profile.id);
        if (rawJson == null) { sendStatError(event, nickname); return; }

        Cs2Stats stats = gson.fromJson(rawJson, Cs2Stats.class);
        if (stats.segments == null || stats.segments.isEmpty()) { event.getChannel().sendMessage("❌ Could not find map data for " + nickname).queue(); return; }

        java.util.List<Cs2Stats.Segment> validMaps = new java.util.ArrayList<>();
        for (Cs2Stats.Segment seg : stats.segments) if (seg.getMatches() >= 5) validMaps.add(seg);
        if (validMaps.isEmpty()) { event.getChannel().sendMessage("❌ " + nickname + " hasn't played enough maps yet.").queue(); return; }

        validMaps.sort((a, b) -> Double.compare(b.getWinRate(), a.getWinRate()));

        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(new Color(0, 200, 100));
        embed.setAuthor("🗺️  Map Mastery  —  " + nickname, "https://www.faceit.com/en/players/" + nickname, null);
        if (profile.avatarUrl != null && !profile.avatarUrl.isEmpty()) embed.setThumbnail(profile.avatarUrl);

        Cs2Stats.Segment best1 = validMaps.get(0);
        embed.addField("👑  Best Map: " + best1.getCleanName(), "**Win Rate:** " + best1.getWinRate() + "%\n**K/D Ratio:** " + best1.getKd() + "\n**Matches:** " + best1.getMatches(), true);
        if (validMaps.size() > 1) {
            Cs2Stats.Segment best2 = validMaps.get(1);
            embed.addField("🥈  Second Best: " + best2.getCleanName(), "**Win Rate:** " + best2.getWinRate() + "%\n**K/D Ratio:** " + best2.getKd() + "\n**Matches:** " + best2.getMatches(), true);
        }

        embed.addField("​", "━━━━━━━━━━━━━━━━━━━━", false);

        Cs2Stats.Segment worst1 = validMaps.get(validMaps.size() - 1);
        embed.addField("🗑️  Auto-Veto: " + worst1.getCleanName(), "**Win Rate:** " + worst1.getWinRate() + "%\n**K/D Ratio:** " + worst1.getKd() + "\n**Matches:** " + worst1.getMatches(), true);
        if (validMaps.size() > 2) {
            Cs2Stats.Segment worst2 = validMaps.get(validMaps.size() - 2);
            embed.addField("⚠️  Weak Link: " + worst2.getCleanName(), "**Win Rate:** " + worst2.getWinRate() + "%\n**K/D Ratio:** " + worst2.getKd() + "\n**Matches:** " + worst2.getMatches(), true);
        }

        embed.setFooter("Only maps with 5+ matches are shown", null);
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    private void sendError(MessageReceivedEvent event, String nickname) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(new Color(220, 50, 50));
        embed.setTitle("❌  Player Not Found");
        embed.setDescription("Could not find **" + nickname + "** on FACEIT.");
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    private void sendStatError(MessageReceivedEvent event, String nickname) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(new Color(220, 50, 50));
        embed.setTitle("❌  Stats Unavailable");
        embed.setDescription("Found **" + nickname + "** but couldn't retrieve their CS2 stats.");
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    private String getBannerForRole(String roleResult) {
        String firstLine = roleResult.split("\n")[0].toUpperCase();
        if (firstLine.contains("AWP") || firstLine.contains("SNIPER")) return "https://media1.tenor.com/m/Yw_D4DqPqLAAAAAd/s1mple-awp.gif";
        if (firstLine.contains("ENTRY") || firstLine.contains("SPACE") || firstLine.contains("BLOOD") || firstLine.contains("CANNON")) return "https://media1.tenor.com/m/r-4jK-U57vUAAAAd/csgo-entry.gif";
        if (firstLine.contains("LURK") || firstLine.contains("CLUTCH") || firstLine.contains("RETAKE") || firstLine.contains("CLOSER")) return "https://media1.tenor.com/m/b_JGEB3B48AAAAAC/csgo-smoke.gif";
        if (firstLine.contains("SUPPORT") || firstLine.contains("UTILITY") || firstLine.contains("FLASH")) return "https://media1.tenor.com/m/Xf1l7uXzMuoAAAAd/csgo-flash.gif";
        return "https://media1.tenor.com/m/FwGv-2Vp4P8AAAAC/cs2-counter-strike-2.gif";
    }
}