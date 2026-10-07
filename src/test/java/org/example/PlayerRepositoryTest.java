package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayerRepositoryTest {

    @TempDir Path dir;

    @Test
    void upsertsAndOrdersByElo() {
        PlayerRepository repo = new PlayerRepository(dir.resolve("t.db").toString());
        repo.savePlayer("low", 1000, 0.9, 45);
        repo.savePlayer("high", 3000, 1.4, 60);
        repo.savePlayer("low", 1200, 1.0, 50);

        List<PlayerRepository.PlayerRecord> top = repo.getTopPlayers(10);
        assertEquals(2, top.size());
        assertEquals("high", top.get(0).nickname());
        assertEquals(1200, top.get(1).elo());
    }

    @Test
    void differentCapitalisationOfTheSamePlayerCollapsesToOneRow() {
        PlayerRepository repo = new PlayerRepository(dir.resolve("t.db").toString());
        repo.savePlayer("S1MPLE", 3000, 1.3, 55);   // legacy row: whatever a user typed
        repo.savePlayer("s1mple", 3100, 1.3, 55);   // canonical FACEIT spelling

        List<PlayerRepository.PlayerRecord> top = repo.getTopPlayers(10);
        assertEquals(1, top.size());
        assertEquals("s1mple", top.get(0).nickname());
        assertEquals(3100, top.get(0).elo());
    }

    @Test
    void concurrentWritesDontLoseRows() throws Exception {
        PlayerRepository repo = new PlayerRepository(dir.resolve("t.db").toString());
        Thread[] threads = new Thread[8];
        for (int i = 0; i < threads.length; i++) {
            int n = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) repo.savePlayer("p" + n + "_" + j, n * 100 + j, 1, 50);
            });
            threads[i].start();
        }
        for (Thread t : threads) t.join();
        assertEquals(80, repo.getTopPlayers(1000).size());
    }
}
