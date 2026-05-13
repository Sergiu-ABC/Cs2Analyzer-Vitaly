package org.example;

import com.google.gson.Gson;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordBot extends ListenerAdapter {

    private final CommandHandler commands;

    public DiscordBot() {
        this.commands = new CommandHandler(
                new FaceitApiClient(),
                new RoleAnalyzer(),
                new Data_Model(),
                new Gson()
        );
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String msg = event.getMessage().getContentRaw();

        if (msg.equalsIgnoreCase("!help")) {
            commands.handleHelp(event);
        } else if (msg.equalsIgnoreCase("!info")) {
            commands.handleInfo(event);
        } else if (msg.equalsIgnoreCase("!leaderboard")) {
            commands.handleLeaderboard(event);
        } else if (msg.startsWith("!stats ")) {
            commands.handleStats(event, msg.substring(7).trim());
        } else if (msg.startsWith("!role ")) {
            commands.handleRole(event, msg.substring(6).trim());
        } else if (msg.startsWith("!advanced ")) {
            commands.handleAdvanced(event, msg.substring(10).trim());
        } else if (msg.startsWith("!maps ")) {
            commands.handleMaps(event, msg.substring(6).trim());
        } else if (msg.startsWith("!compare ")) {
            String[] parts = msg.substring(9).trim().split("\\s+");
            if (parts.length == 2) {
                commands.handleCompare(event, parts[0], parts[1]);
            } else {
                event.getChannel().sendMessage("Use the format: `!compare [player1] [player2]`").queue();
            }
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        commands.handlePeriodInteraction(event);
    }
}