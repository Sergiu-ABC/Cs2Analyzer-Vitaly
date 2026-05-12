package org.example;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.cdimascio.dotenv.Dotenv;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class FaceitApiClient {

    private static String getApiKey() {
        String key = System.getenv("FACEIT_API_KEY");
        if (key == null) {
            try {
                key = Dotenv.load().get("FACEIT_API_KEY");
            } catch (Exception e) {
                System.out.println("❌ CRITICAL ERROR: FACEIT_API_KEY is missing! Please add it to Railway Variables.");
            }
        }
        return key;
    }

    private static final String API_KEY = getApiKey();
    private final HttpClient client = HttpClient.newHttpClient();

    public FaceitProfile getPlayerProfile(String nickname) {
        String url = "https://open.faceit.com/data/v4/players?nickname=" + nickname;
        String response = makeRequest(url);

        if (response != null) {
            try {
                JsonObject root = JsonParser.parseString(response).getAsJsonObject();
                FaceitProfile profile = new FaceitProfile();

                profile.id = root.get("player_id").getAsString();
                profile.avatarUrl = root.has("avatar") ? root.get("avatar").getAsString() : "";

                if (root.has("games") && root.getAsJsonObject("games").has("cs2")) {
                    JsonObject cs2 = root.getAsJsonObject("games").getAsJsonObject("cs2");
                    profile.elo = cs2.has("faceit_elo") ? cs2.get("faceit_elo").getAsInt() : 0;
                    profile.level = cs2.has("skill_level") ? cs2.get("skill_level").getAsInt() : 1;
                }
                return profile;

            } catch (Exception e) {
                System.out.println(" Parsing error in getPlayerProfile: " + e.getMessage());
            }
        }
        return null;
    }

    public String getPlayerStats(String playerId) {
        String cs2Url = "https://open.faceit.com/data/v4/players/" + playerId + "/stats/cs2";
        String response = makeRequest(cs2Url);

        if (response == null) {
            String csgoUrl = "https://open.faceit.com/data/v4/players/" + playerId + "/stats/csgo";
            response = makeRequest(csgoUrl);
        }
        return response;
    }

    private String makeRequest(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                return null;
            }
        } catch (Exception e) {
            System.out.println(" Connection Error: " + e.getMessage());
            return null;
        }
    }
}