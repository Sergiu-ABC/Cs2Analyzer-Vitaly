package com.borcasergiu.vitaly;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
  Handles all HTTP communication with the FACEIT Data API (v4).
  Responsible for fetching player profiles, lifetime stats, and match history.

  - Every user-supplied value is URL-encoded before it goes into a URL.
  - Requests have connect and read timeouts, so a slow FACEIT never hangs a bot thread.
  - 429 / 5xx responses are retried with backoff (honouring Retry-After).
  - Successful responses are cached for a short TTL. Match scoreboards never change,
    so they are cached much longer; this is what keeps period queries cheap.
 */
public class FaceitApiClient {

    public static final String DEFAULT_BASE_URL = "https://open.faceit.com/data/v4";

    private static final Duration PROFILE_TTL = Duration.ofMinutes(5);
    private static final Duration MATCH_TTL   = Duration.ofHours(24);
    private static final int MAX_CACHE_ENTRIES = 5_000;
    private static final int MAX_RETRIES = 2;
    private static final int HISTORY_PAGE_SIZE = 100; // FACEIT's maximum page size

    private final String apiKey;
    private final String baseUrl;
    private final HttpClient client;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private record CacheEntry(String body, long expiresAtMillis) {}

    public FaceitApiClient(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL);
    }

    public FaceitApiClient(String apiKey, String baseUrl) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("FACEIT API key is required");
        }
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    // Fetches basic profile data (Player ID, Avatar, CS2 ELO, and Skill Level) using a FACEIT nickname
    public FaceitProfile getPlayerProfile(String nickname) {
        if (nickname == null || nickname.isBlank()) return null;
        String response = get("/players?nickname=" + encode(nickname.trim()), PROFILE_TTL);
        if (response == null) return null;

        try {
            JsonObject root = JsonParser.parseString(response).getAsJsonObject();
            FaceitProfile profile = new FaceitProfile();
            profile.id = root.get("player_id").getAsString();
            profile.nickname = root.has("nickname") ? root.get("nickname").getAsString() : nickname.trim();
            profile.avatarUrl = optString(root, "avatar");
            profile.level = 1;

            if (root.has("games") && root.getAsJsonObject("games").has("cs2")) {
                JsonObject cs2 = root.getAsJsonObject("games").getAsJsonObject("cs2");
                profile.elo = cs2.has("faceit_elo") ? cs2.get("faceit_elo").getAsInt() : 0;
                profile.level = cs2.has("skill_level") ? cs2.get("skill_level").getAsInt() : 1;
            }
            return profile;
        } catch (RuntimeException e) {
            System.err.println("Parsing error in getPlayerProfile: " + e.getMessage());
            return null;
        }
    }

    // Retrieves the lifetime CS2 statistics (K/D, Win Rate, Headshots, etc.) for a given Player ID.
    // Falls back to CS:GO only when the player has no CS2 stats at all, not on a network error.
    public String getPlayerStats(String playerId) {
        ApiResponse cs2 = request("/players/" + encode(playerId) + "/stats/cs2", PROFILE_TTL);
        if (cs2.status == 200) return cs2.body;
        if (cs2.status == 404) {
            ApiResponse csgo = request("/players/" + encode(playerId) + "/stats/csgo", PROFILE_TTL);
            if (csgo.status == 200) return csgo.body;
        }
        return null;
    }

    /*
      Pulls every match between two UNIX timestamps, following pagination,
      up to maxMatches. Returns null on an API failure, an empty array if there were no matches.
     */
    public JsonArray getPlayerMatchHistoryByDate(String playerId, long fromUnix, long toUnix, int maxMatches) {
        JsonArray all = new JsonArray();
        int offset = 0;

        while (all.size() < maxMatches) {
            int limit = Math.min(HISTORY_PAGE_SIZE, maxMatches - all.size());
            String path = "/players/" + encode(playerId) + "/history?game=cs2"
                    + "&from=" + fromUnix + "&to=" + toUnix
                    + "&offset=" + offset + "&limit=" + limit;
            String response = get(path, PROFILE_TTL);
            if (response == null) return offset == 0 ? null : all;

            JsonArray page;
            try {
                page = JsonParser.parseString(response).getAsJsonObject().getAsJsonArray("items");
            } catch (RuntimeException e) {
                System.err.println("History parse error: " + e.getMessage());
                return offset == 0 ? null : all;
            }
            if (page == null || page.isEmpty()) break;

            for (JsonElement el : page) all.add(el);
            if (page.size() < limit) break;
            offset += page.size();
        }
        return all;
    }

    // Retrieves the detailed scoreboard for a specific match ID
    public JsonObject getMatchStats(String matchId) {
        String response = get("/matches/" + encode(matchId) + "/stats", MATCH_TTL);
        if (response == null) return null;
        try {
            return JsonParser.parseString(response).getAsJsonObject();
        } catch (RuntimeException e) {
            System.err.println("Match stats parse error: " + e.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------------ internals

    private record ApiResponse(int status, String body) {}

    private String get(String path, Duration ttl) {
        ApiResponse r = request(path, ttl);
        return r.status == 200 ? r.body : null;
    }

    private ApiResponse request(String path, Duration ttl) {
        String url = baseUrl + path;

        CacheEntry cached = cache.get(url);
        if (cached != null && cached.expiresAtMillis > System.currentTimeMillis()) {
            return new ApiResponse(200, cached.body);
        }

        for (int attempt = 0; ; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Accept", "application/json")
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();

                if (status == 200) {
                    putInCache(url, response.body(), ttl);
                    return new ApiResponse(200, response.body());
                }
                boolean retryable = status == 429 || status >= 500;
                if (!retryable || attempt >= MAX_RETRIES) {
                    if (status != 404) System.err.println("FACEIT " + status + " for " + path);
                    return new ApiResponse(status, null);
                }
                sleep(backoffMillis(response, attempt));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new ApiResponse(-1, null);
            } catch (Exception e) {
                if (attempt >= MAX_RETRIES) {
                    System.err.println("FACEIT connection error for " + path + ": " + e.getMessage());
                    return new ApiResponse(-1, null);
                }
                sleep(backoffMillis(null, attempt));
            }
        }
    }

    private static long backoffMillis(HttpResponse<?> response, int attempt) {
        if (response != null) {
            String retryAfter = response.headers().firstValue("Retry-After").orElse(null);
            if (retryAfter != null) {
                try {
                    return Math.min(10_000, Long.parseLong(retryAfter.trim()) * 1000);
                } catch (NumberFormatException ignored) {}
            }
        }
        return 500L * (1L << attempt); // 500ms, 1s, 2s
    }

    private void putInCache(String url, String body, Duration ttl) {
        long now = System.currentTimeMillis();
        if (cache.size() >= MAX_CACHE_ENTRIES) {
            cache.values().removeIf(e -> e.expiresAtMillis <= now);
            if (cache.size() >= MAX_CACHE_ENTRIES) cache.clear();
        }
        cache.put(url, new CacheEntry(body, now + ttl.toMillis()));
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String optString(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el == null || el.isJsonNull()) ? "" : el.getAsString();
    }

    static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
