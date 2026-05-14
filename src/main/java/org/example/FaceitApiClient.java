package org.example;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.cdimascio.dotenv.Dotenv;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/*
  Handles all HTTP communication with the FACEIT Data API (v4).
  Responsible for fetching player profiles, lifetime stats, and match history.
 */
public class FaceitApiClient {

    // Retrieves the API key securely from system environment variables or the local .env file
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

    // Fetches basic profile data (Player ID, Avatar, CS2 ELO, and Skill Level) using a FACEIT nickname
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

    // Retrieves the lifetime CS2 statistics (K/D, Win Rate, Headshots, etc.) for a given Player ID
    public String getPlayerStats(String playerId) {
        String cs2Url = "https://open.faceit.com/data/v4/players/" + playerId + "/stats/cs2";
        String response = makeRequest(cs2Url);

        // Fallback to CS:GO stats if CS2 stats are completely unavailable
        if (response == null) {
            String csgoUrl = "https://open.faceit.com/data/v4/players/" + playerId + "/stats/csgo";
            response = makeRequest(csgoUrl);
        }
        return response;
    }

    // Internal utility method to execute authorized GET requests to the FACEIT API
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

    // Pulls a chunk of match history for a player strictly between two UNIX timestamps
    public com.google.gson.JsonArray getPlayerMatchHistoryByDate(String playerId, long fromUnix, long toUnix) {
        String url = "https://open.faceit.com/data/v4/players/" + playerId + "/history?game=cs2&from=" + fromUnix + "&to=" + toUnix + "&limit=50";
        String response = makeRequest(url);
        if (response != null) {
            try {
                return JsonParser.parseString(response).getAsJsonObject().getAsJsonArray("items");
            } catch (Exception e) {
                System.out.println("History Date Error: " + e.getMessage());
            }
        }
        return null;
    }

    // Retrieves the detailed scoreboard and round-by-round performance data for a specific match ID
    public com.google.gson.JsonObject getMatchStats(String matchId) {
        String url = "https://open.faceit.com/data/v4/matches/" + matchId + "/stats";
        String response = makeRequest(url);
        if (response != null) {
            try {
                return JsonParser.parseString(response).getAsJsonObject();
            } catch (Exception e) {
                System.out.println("Match Stats Error: " + e.getMessage());
            }
        }
        return null;
    }
}