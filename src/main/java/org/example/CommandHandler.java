package org.example;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;

import java.util.ArrayList;
import java.util.List;

/*
  Implements every Discord command. Methods here block on FACEIT calls,
  so DiscordBot always invokes them on its worker pool, never on JDA's event thread.
 */
public class CommandHandler {

    private final FaceitApiClient faceitClient;
    private final RoleAnalyzer analyzer;
    private final PlayerRepository db;
    private final PeriodStatsService periodStats;
    private final Gson gson;

    public CommandHandler(FaceitApiClient faceitClient, RoleAnalyzer analyzer,
                          PlayerRepository db, PeriodStatsService periodStats, Gson gson) {
        this.faceitClient = faceitClient;
        this.analyzer     = analyzer;
        this.db           = db;
        this.periodStats  = periodStats;
        this.gson         = gson;
    }

    // A profile plus its lifetime stats, or null after an error embed was already sent.
    private record Player(FaceitProfile profile, Cs2Stats stats) {}

    private Player loadPlayer(MessageReceivedEvent event, String nickname) {
        FaceitProfile profile = faceitClient.getPlayerProfile(nickname);
        if (profile == null) { sendError(event, nickname); return null; }

        Cs2Stats stats = parseStats(faceitClient.getPlayerStats(profile.id));
        if (stats == null) { sendStatError(event, profile.nickname); return null; }
        return new Player(profile, stats);
    }

    private Cs2Stats parseStats(String rawJson) {
        if (rawJson == null) return null;
        try {
            return gson.fromJson(rawJson, Cs2Stats.class);
        } catch (JsonParseException e) {
            System.err.println("Stats parse error: " + e.getMessage());
            return null;
        }
    }

    public void handleHelp(MessageReceivedEvent event) {
        event.getChannel().sendMessageEmbeds(EmbedFactory.buildHelp().build()).queue();
    }

    public void handleInfo(MessageReceivedEvent event) {
        event.getChannel().sendMessageEmbeds(EmbedFactory.buildInfo().build()).queue();
    }

    public void handleLeaderboard(MessageReceivedEvent event) {
        List<PlayerRepository.PlayerRecord> topPlayers = db.getTopPlayers(10);
        if (topPlayers.isEmpty()) {
            event.getChannel().sendMessage("📭 The leaderboard is empty! Scan players with `!stats` or `!role` first.").queue();
            return;
        }
        event.getChannel().sendMessageEmbeds(EmbedFactory.buildLeaderboard(topPlayers).build()).queue();
    }

    public void handleStats(MessageReceivedEvent event, String nickname) {
        Player p = loadPlayer(event, nickname);
        if (p == null) return;
        String name = p.profile.nickname;
        db.savePlayer(name, p.profile.elo, p.stats.getKd(), p.stats.getWinRate());

        event.getChannel().sendMessageEmbeds(EmbedFactory.buildStats(name, p.profile, p.stats).build())
                .setComponents(ActionRow.of(EmbedFactory.buildPeriodMenu(name)))
                .queue();
    }

    public void handleRole(MessageReceivedEvent event, String nickname) {
        Player p = loadPlayer(event, nickname);
        if (p == null) return;
        String name = p.profile.nickname;
        String roleResult = analyzer.determineRole(p.stats);
        db.savePlayer(name, p.profile.elo, p.stats.getKd(), p.stats.getWinRate());

        event.getChannel().sendMessageEmbeds(
                EmbedFactory.buildRole(name, p.profile, p.stats, roleResult,
                        event.getAuthor().getName(), event.getAuthor().getAvatarUrl()).build()
        ).setComponents(ActionRow.of(EmbedFactory.buildPeriodMenu(name))).queue();
    }

    public void handleAdvanced(MessageReceivedEvent event, String nickname) {
        Player p = loadPlayer(event, nickname);
        if (p == null) return;
        event.getChannel().sendMessageEmbeds(
                EmbedFactory.buildAdvanced(p.profile.nickname, p.profile, p.stats).build()).queue();
    }

    public void handleMaps(MessageReceivedEvent event, String nickname) {
        Player p = loadPlayer(event, nickname);
        if (p == null) return;
        String name = p.profile.nickname;

        List<Cs2Stats.Segment> validMaps = new ArrayList<>();
        if (p.stats.segments != null) {
            for (Cs2Stats.Segment seg : p.stats.segments) {
                if (seg.getMatches() >= 5) validMaps.add(seg);
            }
        }
        if (validMaps.isEmpty()) {
            event.getChannel().sendMessageEmbeds(
                    EmbedFactory.buildError(name + " hasn't played 5+ matches on any map yet.").build()).queue();
            return;
        }

        validMaps.sort((a, b) -> Double.compare(b.getWinRate(), a.getWinRate()));
        event.getChannel().sendMessageEmbeds(EmbedFactory.buildMaps(name, p.profile, validMaps).build()).queue();
    }

    public void handleCompare(MessageReceivedEvent event, String p1Name, String p2Name) {
        Player p1 = loadPlayer(event, p1Name);
        if (p1 == null) return;
        Player p2 = loadPlayer(event, p2Name);
        if (p2 == null) return;

        db.savePlayer(p1.profile.nickname, p1.profile.elo, p1.stats.getKd(), p1.stats.getWinRate());
        db.savePlayer(p2.profile.nickname, p2.profile.elo, p2.stats.getKd(), p2.stats.getWinRate());

        event.getChannel().sendMessageEmbeds(
                EmbedFactory.buildCompare(p1.profile.nickname, p1.profile, p1.stats,
                        p2.profile.nickname, p2.profile, p2.stats).build()
        ).queue();
    }

    // !period <nickname>: advertised in !help but previously never wired up.
    public void handlePeriod(MessageReceivedEvent event, String nickname) {
        FaceitProfile profile = faceitClient.getPlayerProfile(nickname);
        if (profile == null) { sendError(event, nickname); return; }

        event.getChannel().sendMessageEmbeds(EmbedFactory.buildPeriodPrompt(profile.nickname).build())
                .setComponents(ActionRow.of(EmbedFactory.buildPeriodMenu(profile.nickname)))
                .queue();
    }

    /*
      Called after DiscordBot has already acknowledged the interaction (deferEdit),
      so this may take as long as the FACEIT calls need.
     */
    public void handlePeriodInteraction(StringSelectInteractionEvent event) {
        String nickname = event.getComponentId().substring("time_selector:".length());
        int days;
        try {
            days = Integer.parseInt(event.getValues().get(0));
        } catch (RuntimeException e) {
            return;
        }
        var hook = event.getHook();

        FaceitProfile profile = faceitClient.getPlayerProfile(nickname);
        if (profile == null) {
            hook.editOriginalEmbeds(EmbedFactory.buildPlayerNotFound(nickname).build()).setComponents().queue();
            return;
        }

        if (days == 0) {
            Cs2Stats stats = parseStats(faceitClient.getPlayerStats(profile.id));
            if (stats == null) {
                hook.editOriginalEmbeds(EmbedFactory.buildStatsUnavailable(profile.nickname).build()).queue();
                return;
            }
            hook.editOriginalEmbeds(EmbedFactory.buildStats(profile.nickname, profile, stats).build())
                    .setComponents(ActionRow.of(EmbedFactory.buildPeriodMenu(profile.nickname))).queue();
            return;
        }
        if (!PeriodStatsService.ALLOWED_DAYS.contains(days)) return;

        PeriodStats result = periodStats.aggregate(profile.id, days);
        if (result == null) {
            hook.editOriginalEmbeds(EmbedFactory.buildError("FACEIT didn't respond. Try again in a moment.").build())
                    .setComponents(ActionRow.of(EmbedFactory.buildPeriodMenu(profile.nickname))).queue();
            return;
        }
        if (result.maps() == 0) {
            hook.editOriginalEmbeds(EmbedFactory.buildError(
                    "No CS2 matches found for " + profile.nickname + " in the last " + days + " days.").build())
                    .setComponents(ActionRow.of(EmbedFactory.buildPeriodMenu(profile.nickname))).queue();
            return;
        }

        hook.editOriginalEmbeds(EmbedFactory.buildPeriodResult(profile.nickname, profile.avatarUrl, result).build())
                .setComponents(ActionRow.of(EmbedFactory.buildPeriodMenu(profile.nickname))).queue();
    }

    private void sendError(MessageReceivedEvent event, String nickname) {
        event.getChannel().sendMessageEmbeds(EmbedFactory.buildPlayerNotFound(nickname).build()).queue();
    }

    private void sendStatError(MessageReceivedEvent event, String nickname) {
        event.getChannel().sendMessageEmbeds(EmbedFactory.buildStatsUnavailable(nickname).build()).queue();
    }
}
