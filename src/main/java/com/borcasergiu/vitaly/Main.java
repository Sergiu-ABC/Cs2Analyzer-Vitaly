package com.borcasergiu.vitaly;

import com.google.gson.Gson;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

/*
  Composition root: builds every shared component once and hands the same instances
  to the Discord bot and the web dashboard (one FACEIT client + cache, one database).
 */
public class Main {
    public static void main(String[] args) {
        String faceitKey;
        try {
            faceitKey = Config.require("FACEIT_API_KEY");
        } catch (IllegalStateException e) {
            System.err.println("❌ " + e.getMessage());
            System.exit(1);
            return;
        }

        int port = Config.getInt("PORT", 8080);
        String dbPath = Config.get("DB_PATH") != null ? Config.get("DB_PATH") : "vitaly.db";
        boolean trustProxy = "true".equalsIgnoreCase(Config.get("TRUST_PROXY"));

        Gson gson = new Gson();
        FaceitApiClient faceit = new FaceitApiClient(faceitKey);
        RoleAnalyzer analyzer = new RoleAnalyzer();
        PlayerRepository db = new PlayerRepository(dbPath);
        PeriodStatsService periodStats = new PeriodStatsService(faceit);

        new WebDashboard(faceit, analyzer, db, periodStats, gson, trustProxy).startServer(port);

        // The dashboard is useful on its own, so a missing Discord token is a warning, not a crash.
        String discordToken = Config.get("DISCORD_TOKEN");
        if (discordToken == null) {
            System.out.println("⚠️ DISCORD_TOKEN not set: running the web dashboard only.");
            return;
        }

        CommandHandler commands = new CommandHandler(faceit, analyzer, db, periodStats, gson);
        try {
            JDABuilder.createDefault(discordToken)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(new DiscordBot(commands))
                    .build();
            System.out.println("✅ Vitaly Discord Bot is ONLINE!");
        } catch (Exception e) {
            System.err.println("❌ Could not start the Discord bot: " + e.getMessage());
        }
    }
}
