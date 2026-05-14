package org.example;

import com.google.gson.Gson;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CommandHandler {

    private final FaceitApiClient faceitClient;
    private final RoleAnalyzer analyzer;
    private final Data_Model db;
    private final Gson gson;

    public CommandHandler(FaceitApiClient faceitClient, RoleAnalyzer analyzer,
                          Data_Model db, Gson gson) {
        this.faceitClient = faceitClient;
        this.analyzer     = analyzer;
        this.db           = db;
        this.gson         = gson;
    }

    public void handleHelp(MessageReceivedEvent event) {
        event.getChannel().sendMessageEmbeds(EmbedFactory.buildHelp().build()).queue();
    }

    public void handleInfo(MessageReceivedEvent event) {
        event.getChannel().sendMessageEmbeds(EmbedFactory.buildInfo().build()).queue();
    }

    public void handleLeaderboard(MessageReceivedEvent event) {
        List<Data_Model.PlayerRecord> topPlayers = db.getTopPlayers(10);
        if (topPlayers.isEmpty()) {
            event.getChannel().sendMessage("📭 The leaderboard is empty! Scan players with `!stats` or `!role` first.").queue();
            return;
        }
        event.getChannel().sendMessageEmbeds(EmbedFactory.buildLeaderboard(topPlayers).build()).queue();
    }

    public void handleStats(MessageReceivedEvent event, String nickname) {
        FaceitProfile profile = faceitClient.getPlayerProfile(nickname);
        if (profile == null) { sendError(event, nickname); return; }

        String rawJson = faceitClient.getPlayerStats(profile.id);
        if (rawJson == null) { sendStatError(event, nickname); return; }

        Cs2Stats stats = gson.fromJson(rawJson, Cs2Stats.class);
        db.savePlayer(nickname, profile.elo, stats.getKd(), stats.getWinRate());

        event.getChannel().sendMessageEmbeds(EmbedFactory.buildStats(nickname, profile, stats).build())
                .setComponents(ActionRow.of(EmbedFactory.buildPeriodMenu(nickname)))
                .queue();
    }

    public void handleRole(MessageReceivedEvent event, String nickname) {
        FaceitProfile profile = faceitClient.getPlayerProfile(nickname);
        if (profile == null) { sendError(event, nickname); return; }

        String rawJson = faceitClient.getPlayerStats(profile.id);
        if (rawJson == null) { sendStatError(event, nickname); return; }

        Cs2Stats stats      = gson.fromJson(rawJson, Cs2Stats.class);
        String roleResult   = analyzer.determineRole(stats);
        db.savePlayer(nickname, profile.elo, stats.getKd(), stats.getWinRate());

        event.getChannel().sendMessageEmbeds(
                EmbedFactory.buildRole(nickname, profile, stats, roleResult,
                        event.getAuthor().getName(), event.getAuthor().getAvatarUrl()).build()
        ).setComponents(ActionRow.of(EmbedFactory.buildPeriodMenu(nickname))).queue();
    }

    public void handleAdvanced(MessageReceivedEvent event, String nickname) {
        FaceitProfile profile = faceitClient.getPlayerProfile(nickname);
        if (profile == null) { sendError(event, nickname); return; }

        String rawJson = faceitClient.getPlayerStats(profile.id);
        if (rawJson == null) { sendStatError(event, nickname); return; }

        Cs2Stats stats = gson.fromJson(rawJson, Cs2Stats.class);
        event.getChannel().sendMessageEmbeds(EmbedFactory.buildAdvanced(nickname, profile, stats).build()).queue();
    }

    public void handleMaps(MessageReceivedEvent event, String nickname) {
        FaceitProfile profile = faceitClient.getPlayerProfile(nickname);
        if (profile == null) { sendError(event, nickname); return; }

        String rawJson = faceitClient.getPlayerStats(profile.id);
        if (rawJson == null) { sendStatError(event, nickname); return; }

        Cs2Stats stats = gson.fromJson(rawJson, Cs2Stats.class);

        if (stats.segments == null || stats.segments.isEmpty()) {
            event.getChannel().sendMessage("❌ Could not find map data for " + nickname).queue();
            return;
        }

        List<Cs2Stats.Segment> validMaps = new ArrayList<>();
        for (Cs2Stats.Segment seg : stats.segments) {
            if (seg.getMatches() >= 5) validMaps.add(seg);
        }

        if (validMaps.isEmpty()) {
            event.getChannel().sendMessage("❌ " + nickname + " hasn't played enough maps yet.").queue();
            return;
        }

        validMaps.sort((a, b) -> Double.compare(b.getWinRate(), a.getWinRate()));
        event.getChannel().sendMessageEmbeds(EmbedFactory.buildMaps(nickname, profile, validMaps).build()).queue();
    }

    public void handleCompare(MessageReceivedEvent event, String p1Name, String p2Name) {
        FaceitProfile p1 = faceitClient.getPlayerProfile(p1Name);
        if (p1 == null) { sendError(event, p1Name); return; }

        String raw1 = faceitClient.getPlayerStats(p1.id);
        if (raw1 == null) { sendStatError(event, p1Name); return; }

        FaceitProfile p2 = faceitClient.getPlayerProfile(p2Name);
        if (p2 == null) { sendError(event, p2Name); return; }

        String raw2 = faceitClient.getPlayerStats(p2.id);
        if (raw2 == null) { sendStatError(event, p2Name); return; }

        Cs2Stats stats1 = gson.fromJson(raw1, Cs2Stats.class);
        Cs2Stats stats2 = gson.fromJson(raw2, Cs2Stats.class);

        db.savePlayer(p1Name, p1.elo, stats1.getKd(), stats1.getWinRate());
        db.savePlayer(p2Name, p2.elo, stats2.getKd(), stats2.getWinRate());

        event.getChannel().sendMessageEmbeds(
                EmbedFactory.buildCompare(p1Name, p1, stats1, p2Name, p2, stats2).build()
        ).queue();
    }

    public void handlePeriodInteraction(StringSelectInteractionEvent event) {
        if (!event.getComponentId().startsWith("time_selector:")) return;

        String nickname = event.getComponentId().split(":")[1];
        int days        = Integer.parseInt(event.getValues().get(0));

        event.deferEdit().queue(hook -> CompletableFuture.runAsync(() -> {
            FaceitProfile profile = faceitClient.getPlayerProfile(nickname);
            if (profile == null) {
                hook.editOriginal("").setEmbeds(EmbedFactory.buildPlayerNotFound(nickname).build()).setComponents().queue();
                return;
            }

            if (days == 0) {
                String rawJson = faceitClient.getPlayerStats(profile.id);
                if (rawJson == null) return;
                Cs2Stats stats = gson.fromJson(rawJson, Cs2Stats.class);
                hook.editOriginalEmbeds(EmbedFactory.buildStats(nickname, profile, stats).build())
                        .setComponents(ActionRow.of(EmbedFactory.buildPeriodMenu(nickname))).queue();
                return;
            }

            long toUnix   = Instant.now().getEpochSecond();
            long fromUnix = toUnix - (days * 86400L);

            com.google.gson.JsonArray history =
                    faceitClient.getPlayerMatchHistoryByDate(profile.id, fromUnix, toUnix);

            if (history == null || history.isEmpty()) {
                hook.editOriginal("").setEmbeds(EmbedFactory.buildStatsUnavailable(nickname).build()).setComponents().queue();
                return;
            }

            int totalKills = 0, totalDeaths = 0, totalHs = 0, wins = 0, validMatches = 0;

            for (com.google.gson.JsonElement el : history) {
                String matchId = el.getAsJsonObject().get("match_id").getAsString();
                com.google.gson.JsonObject matchStats = faceitClient.getMatchStats(matchId);

                if (matchStats == null || !matchStats.has("rounds")) continue;

                com.google.gson.JsonArray rounds = matchStats.getAsJsonArray("rounds");
                if (rounds.isEmpty()) continue;

                com.google.gson.JsonObject roundData = rounds.get(0).getAsJsonObject();
                com.google.gson.JsonArray teams      = roundData.getAsJsonArray("teams");

                boolean found = false;
                for (com.google.gson.JsonElement teamEl : teams) {
                    com.google.gson.JsonArray players = teamEl.getAsJsonObject().getAsJsonArray("players");

                    for (com.google.gson.JsonElement playerEl : players) {
                        com.google.gson.JsonObject p = playerEl.getAsJsonObject();
                        if (!p.get("player_id").getAsString().equals(profile.id)) continue;

                        com.google.gson.JsonObject pStats = p.getAsJsonObject("player_stats");
                        try {
                            totalKills  += Integer.parseInt(pStats.get("Kills").getAsString());
                            totalDeaths += Integer.parseInt(pStats.get("Deaths").getAsString());
                            totalHs     += Integer.parseInt(pStats.get("Headshots").getAsString());
                            if ("1".equals(pStats.get("Result").getAsString())) wins++;
                            validMatches++;
                        } catch (Exception ignored) {}
                        found = true;
                        break;
                    }
                    if (found) break;
                }
            }

            if (validMatches == 0) {
                hook.editOriginal("").setEmbeds(EmbedFactory.buildStatsUnavailable(nickname).build()).setComponents().queue();
                return;
            }

            double avgKd = totalDeaths > 0 ? (double) totalKills / totalDeaths : totalKills;
            double avgHs = totalKills  > 0 ? ((double) totalHs / totalKills) * 100 : 0;

            hook.editOriginalEmbeds(
                    EmbedFactory.buildPeriodResult(nickname, profile.avatarUrl,
                            days, validMatches, wins, totalKills, avgKd, avgHs).build()
            ).setComponents(ActionRow.of(EmbedFactory.buildPeriodMenu(nickname))).queue();
        }));
    }

    private void sendError(MessageReceivedEvent event, String nickname) {
        event.getChannel().sendMessageEmbeds(EmbedFactory.buildPlayerNotFound(nickname).build()).queue();
    }

    private void sendStatError(MessageReceivedEvent event, String nickname) {
        event.getChannel().sendMessageEmbeds(EmbedFactory.buildStatsUnavailable(nickname).build()).queue();
    }
}