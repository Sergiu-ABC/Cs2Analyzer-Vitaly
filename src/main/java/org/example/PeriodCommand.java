package org.example;

import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class PeriodCommand {

    private final FaceitApiClient faceitClient;

    public PeriodCommand(FaceitApiClient faceitClient) {
        this.faceitClient = faceitClient;
    }

    public void handlePeriodSelector(MessageReceivedEvent event, String nickname) {
        StringSelectMenu menu = StringSelectMenu.create("time_selector:" + nickname)
                .setPlaceholder("Select a time period...")
                .addOption("Last 30 Days",  "30",  "Recent form")
                .addOption("Last 90 Days",  "90",  "Quarterly performance")
                .addOption("Last 6 Months", "180", "Half-year aggregate")
                .addOption("Last Year",     "365", "Full year aggregate")
                .build();

        event.getChannel()
                .sendMessage("📅 Select the time period to analyze **" + nickname + "**:")
                .setComponents(ActionRow.of(menu))
                .queue();
    }

    public void handlePeriodInteraction(StringSelectInteractionEvent event) {
        if (!event.getComponentId().startsWith("time_selector:")) return;

        String nickname = event.getComponentId().split(":")[1];
        int days        = Integer.parseInt(event.getValues().get(0));

        event.deferReply().queue(hook -> CompletableFuture.runAsync(() -> {
            FaceitProfile profile = faceitClient.getPlayerProfile(nickname);
            if (profile == null) {
                hook.sendMessage("❌ Player **" + nickname + "** not found.").queue();
                return;
            }

            long toUnix   = Instant.now().getEpochSecond();
            long fromUnix = toUnix - (days * 86400L);

            com.google.gson.JsonArray history =
                    faceitClient.getPlayerMatchHistoryByDate(profile.id, fromUnix, toUnix);

            if (history == null || history.isEmpty()) {
                hook.sendMessage("❌ No matches found for **" + nickname + "** in the last " + days + " days.").queue();
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
                hook.sendMessage("❌ Could not parse match stats for **" + nickname + "**").queue();
                return;
            }

            double avgKd = totalDeaths > 0 ? (double) totalKills / totalDeaths : totalKills;
            double avgHs = totalKills  > 0 ? ((double) totalHs / totalKills) * 100 : 0;

            hook.sendMessageEmbeds(
                    EmbedFactory.buildPeriodResult(nickname, profile.avatarUrl,
                            days, validMatches, wins, totalKills, avgKd, avgHs).build()
            ).queue();
        }));
    }
}