package com.borcasergiu.vitaly;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class FaceitApiClientTest {

    private static final String PROFILE = """
            { "player_id": "id-1", "nickname": "s1mple", "avatar": "a.png",
              "games": { "cs2": { "faceit_elo": 3200, "skill_level": 10 } } }
            """;

    @Test
    void nicknameIsUrlEncodedAndCanonicalised() throws Exception {
        try (FakeFaceit api = new FakeFaceit(path -> new FakeFaceit.Reply(200, PROFILE))) {
            FaceitApiClient client = new FaceitApiClient("key", api.baseUrl());
            FaceitProfile p = client.getPlayerProfile("S1MPLE&game=x");

            assertEquals("s1mple", p.nickname);
            assertEquals(3200, p.elo);
            // Regression: the raw nickname used to be concatenated into the query string.
            assertEquals("/players?nickname=S1MPLE%26game%3Dx", api.requests.get(0));
        }
    }

    @Test
    void retriesOn429ThenSucceeds() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        try (FakeFaceit api = new FakeFaceit(path ->
                calls.incrementAndGet() == 1 ? new FakeFaceit.Reply(429, null) : new FakeFaceit.Reply(200, PROFILE))) {
            FaceitApiClient client = new FaceitApiClient("key", api.baseUrl());
            assertNotNull(client.getPlayerProfile("s1mple"));
            assertEquals(2, calls.get());
        }
    }

    @Test
    void cachesSuccessfulResponses() throws Exception {
        try (FakeFaceit api = new FakeFaceit(path -> new FakeFaceit.Reply(200, PROFILE))) {
            FaceitApiClient client = new FaceitApiClient("key", api.baseUrl());
            client.getPlayerProfile("s1mple");
            client.getPlayerProfile("s1mple");
            assertEquals(1, api.requests.size());
        }
    }

    @Test
    void fallsBackToCsgoOnlyWhenCs2StatsDoNotExist() throws Exception {
        try (FakeFaceit api = new FakeFaceit(path -> path.endsWith("/cs2")
                ? new FakeFaceit.Reply(404, null) : new FakeFaceit.Reply(200, "{\"csgo\":true}"))) {
            assertEquals("{\"csgo\":true}", new FaceitApiClient("key", api.baseUrl()).getPlayerStats("id-1"));
        }
        try (FakeFaceit api = new FakeFaceit(path -> path.endsWith("/cs2")
                ? new FakeFaceit.Reply(401, null) : new FakeFaceit.Reply(200, "{\"csgo\":true}"))) {
            // An auth/network failure must not silently serve stats from a different game.
            assertNull(new FaceitApiClient("key", api.baseUrl()).getPlayerStats("id-1"));
        }
    }

    @Test
    void historyFollowsPagination() throws Exception {
        try (FakeFaceit api = new FakeFaceit(path -> {
            int offset = Integer.parseInt(path.replaceAll(".*offset=(\\d+).*", "$1"));
            int count = offset == 0 ? 100 : 30;
            StringBuilder items = new StringBuilder();
            for (int i = 0; i < count; i++) {
                if (i > 0) items.append(',');
                items.append("{\"match_id\":\"m").append(offset + i).append("\"}");
            }
            return new FakeFaceit.Reply(200, "{\"items\":[" + items + "]}");
        })) {
            FaceitApiClient client = new FaceitApiClient("key", api.baseUrl());
            // Regression: the old client asked for limit=50 once, so "last year" was silently 50 matches.
            assertEquals(130, client.getPlayerMatchHistoryByDate("id-1", 0, 1, 500).size());
            assertEquals(2, api.requests.size());
        }
    }

    @Test
    void refusesToStartWithoutAnApiKey() {
        assertThrows(IllegalArgumentException.class, () -> new FaceitApiClient(null));
    }
}
