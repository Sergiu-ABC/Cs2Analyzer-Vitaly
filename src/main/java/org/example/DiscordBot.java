package org.example;

import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
  Routes Discord messages to CommandHandler.

  JDA delivers events on a single thread. Every command makes blocking HTTP calls
  (a period query can make 100+), so running them inline froze the whole bot for every
  server until the slowest command finished. Commands now run on a small worker pool.
 */
public class DiscordBot extends ListenerAdapter {

    private static final int MAX_NICKNAME_LENGTH = 32;

    private final CommandHandler commands;
    private final ExecutorService workers = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "discord-command");
        t.setDaemon(true);
        return t;
    });

    public DiscordBot(CommandHandler commands) {
        this.commands = commands;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String msg = event.getMessage().getContentRaw().trim();
        if (!msg.startsWith("!")) return;

        String[] parts = msg.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1].trim() : "";

        Runnable task = switch (command) {
            case "!help"        -> () -> commands.handleHelp(event);
            case "!info"        -> () -> commands.handleInfo(event);
            case "!leaderboard" -> () -> commands.handleLeaderboard(event);
            case "!stats"       -> withNickname(event, arg, n -> commands.handleStats(event, n));
            case "!role"        -> withNickname(event, arg, n -> commands.handleRole(event, n));
            case "!advanced"    -> withNickname(event, arg, n -> commands.handleAdvanced(event, n));
            case "!maps"        -> withNickname(event, arg, n -> commands.handleMaps(event, n));
            case "!period"      -> withNickname(event, arg, n -> commands.handlePeriod(event, n));
            case "!compare"     -> {
                String[] names = arg.split("\\s+");
                if (names.length != 2 || !isValidNickname(names[0]) || !isValidNickname(names[1])) {
                    yield () -> event.getChannel().sendMessage("Use the format: `!compare [player1] [player2]`").queue();
                }
                yield () -> commands.handleCompare(event, names[0], names[1]);
            }
            default -> null;
        };
        if (task != null) runSafely(event, task);
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (!event.getComponentId().startsWith("time_selector:")) return;
        // Acknowledge within Discord's 3 second window, then do the slow work off-thread.
        event.deferEdit().queue();
        workers.execute(() -> {
            try {
                commands.handlePeriodInteraction(event);
            } catch (Exception e) {
                e.printStackTrace();
                event.getHook().editOriginalEmbeds(
                        EmbedFactory.buildError("Unexpected error while analysing this period.").build()).queue();
            }
        });
    }

    private Runnable withNickname(MessageReceivedEvent event, String arg,
                                  java.util.function.Consumer<String> action) {
        if (!isValidNickname(arg)) {
            return () -> event.getChannel().sendMessage("Please give a valid FACEIT nickname, e.g. `!stats s1mple`").queue();
        }
        return () -> action.accept(arg);
    }

    static boolean isValidNickname(String s) {
        return s != null && !s.isBlank() && s.length() <= MAX_NICKNAME_LENGTH && !s.contains(" ");
    }

    private void runSafely(MessageReceivedEvent event, Runnable task) {
        workers.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                e.printStackTrace();
                event.getChannel().sendMessageEmbeds(
                        EmbedFactory.buildError("Unexpected error. Please try again.").build()).queue();
            }
        });
    }
}
