package org.example;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import io.github.cdimascio.dotenv.Dotenv;

public class Main {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();
        String discordToken = dotenv.get("DISCORD_TOKEN");
        WebDashboard dashboard = new WebDashboard();
        dashboard.startServer(8080);
        try {
            JDABuilder.createDefault(discordToken)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(new DiscordBot())
                    .build();

            System.out.println("✅ Vitaly Discord Bot is ONLINE!");

        } catch (Exception e) {
            System.out.println("❌ Critical Error: Could not start the Discord Bot.");
            e.printStackTrace();
        }
    }
}