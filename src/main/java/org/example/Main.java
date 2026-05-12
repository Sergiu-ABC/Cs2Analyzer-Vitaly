package org.example;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import io.github.cdimascio.dotenv.Dotenv;

public class Main {
    public static void main(String[] args) {
        String discordToken = System.getenv("DISCORD_TOKEN") != null
                ? System.getenv("DISCORD_TOKEN")
                : Dotenv.load().get("DISCORD_TOKEN");

        int port = System.getenv("PORT") != null ? Integer.parseInt(System.getenv("PORT")) : 8080;

        WebDashboard dashboard = new WebDashboard();
        dashboard.startServer(port);

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