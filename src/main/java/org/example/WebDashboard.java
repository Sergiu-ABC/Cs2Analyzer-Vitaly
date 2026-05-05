package org.example;

import io.javalin.Javalin;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class WebDashboard {
    private final FaceitApiClient faceitClient = new FaceitApiClient();
    private final RoleAnalyzer analyzer = new RoleAnalyzer();
    private final Gson gson = new Gson();


    private final ConcurrentHashMap<String, Long> lastRequestTime = new ConcurrentHashMap<>();
    private static final long RATE_LIMIT_MS = TimeUnit.SECONDS.toMillis(5);

    public void startServer(int port) {
        Javalin app = Javalin.create().start(port);
        System.out.println("✅ Web Dashboard running at: http://localhost:" + port);

        app.get("/", ctx -> {
            ctx.html(getHtmlLayout());
        });


        app.get("/api/analyze/{nickname}", ctx -> {
            String ip = ctx.ip();
            long now = System.currentTimeMillis();
            Long last = lastRequestTime.get(ip);

            if (last != null && (now - last) < RATE_LIMIT_MS) {
                ctx.status(429).json(Map.of("success", false, "error", "Too many requests. Please wait a few seconds."));
                return;
            }
            lastRequestTime.put(ip, now);

            String nickname = ctx.pathParam("nickname");
            FaceitProfile profile = faceitClient.getPlayerProfile(nickname);
            if (profile != null) {
                String rawJson = faceitClient.getPlayerStats(profile.id);
                if (rawJson != null) {
                    Cs2Stats stats = gson.fromJson(rawJson, Cs2Stats.class);
                    String roleResult = analyzer.determineRole(stats);

                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("success", true);
                    responseData.put("nickname", nickname);
                    responseData.put("role", roleResult);

                    responseData.put("kd", stats.getKd());
                    responseData.put("adr", stats.getAdr());
                    responseData.put("hs", stats.getHs());
                    responseData.put("winRate", stats.getWinRate());

                    responseData.put("matches", stats.getMatches());
                    responseData.put("wins", stats.getTotalWins());
                    responseData.put("entry", Math.round(stats.getEntrySuccess() * 100));
                    responseData.put("utility", stats.getUtilityDmg());
                    responseData.put("clutch1v2", Math.round(stats.getClutch1v2() * 100));
                    responseData.put("clutch1v1", Math.round(stats.getClutch1v1() * 100));
                     ctx.json(responseData);
                } else {
                    ctx.json(Map.of("success", false, "error", "Could not fetch CS2 stats."));
                }
            } else {
                ctx.json(Map.of("success", false, "error", "Player not found on FACEIT."));
            }
        });
    }

    private String getHtmlLayout() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>CS2 Role Analyzer</title>
                <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #121212; color: #ffffff; text-align: center; padding: 40px; margin: 0; }
                    .container { background-color: #1e1e24; padding: 40px; border-radius: 15px; display: inline-block; width: 100%; max-width: 600px; box-shadow: 0 10px 30px rgba(0,0,0,0.8); }
                    h1 { color: #ff4c4c; margin-top: 0; }
                    input { padding: 12px; font-size: 16px; border-radius: 8px; border: 1px solid #333; outline: none; background: #2b2b36; color: white; width: 60%; }
                    button.search-btn { padding: 12px 24px; font-size: 16px; cursor: pointer; background-color: #ff4c4c; color: white; border: none; border-radius: 8px; transition: 0.3s; font-weight: bold; }
                    button.search-btn:hover { background-color: #ff1c1c; }
                    
                    #playerCard { display: none; margin-top: 30px; background: #25252d; padding: 20px; border-radius: 10px; border-left: 5px solid #ff4c4c; text-align: left; }
                    .role-title { font-size: 22px; font-weight: bold; color: #ffcc00; margin-bottom: 15px; border-bottom: 1px solid #444; padding-bottom: 10px; }
                    
                    .stat-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 20px; }
                    .stat-box { background: #1a1a1f; padding: 15px; border-radius: 8px; text-align: center; }
                    .stat-value { font-size: 24px; font-weight: bold; color: #4CAF50; }
                    .stat-label { font-size: 12px; color: #aaa; text-transform: uppercase; letter-spacing: 1px; }
                    
                    .chart-container { width: 100%; max-width: 350px; margin: 20px auto; display: block; }
                    
                    #errorMsg { color: #ff4c4c; margin-top: 20px; font-weight: bold; display: none; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>CS2 AI Analyzer</h1>
                    <p>Enter a FACEIT Nickname to reveal their playstyle.</p>
                    <input type="text" id="nickname" placeholder="e.g. Sonaldo30">
                    <button class="search-btn" onclick="analyzePlayer()">Analyze</button>
                    
                    <div id="errorMsg"></div>

                    <div id="playerCard">
                        <div class="role-title" id="roleText"></div>
                        
                        <div class="stat-grid">
                            <div class="stat-box">
                                <div class="stat-value" id="valKD"></div>
                                <div class="stat-label">K/D Ratio</div>
                            </div>
                            <div class="stat-box">
                                <div class="stat-value" id="valADR"></div>
                                <div class="stat-label">Avg Damage</div>
                            </div>
                        </div>

                        <div class="chart-container">
                            <canvas id="radarChart"></canvas>
                        </div>
                        
                    </div>
                </div>
            
                <script>
                    let myRadarChart = null;

                    async function analyzePlayer() {
                        const nickname = document.getElementById('nickname').value;
                        const errorMsg = document.getElementById('errorMsg');
                        const playerCard = document.getElementById('playerCard');
                        
                        if(!nickname) return;
                        
                        errorMsg.style.display = 'none';
                        playerCard.style.display = 'none';
                        document.getElementById('roleText').innerText = "Scanning...";
                        playerCard.style.display = 'block';
            
                        const response = await fetch('/api/analyze/' + nickname);
                        const data = await response.json();
                        
                        if(data.success) {
                            document.getElementById('roleText').innerText = data.role;
                            document.getElementById('valKD').innerText = data.kd;
                            document.getElementById('valADR').innerText = data.adr;
                            
                            drawChart(data);
                        } else {
                            playerCard.style.display = 'none';
                            errorMsg.style.display = 'block';
                            errorMsg.innerText = data.error;
                        }
                    }

                    function drawChart(data) {
                        const ctx = document.getElementById('radarChart').getContext('2d');
                        
                        if (myRadarChart != null) {
                            myRadarChart.destroy();
                        }

                        myRadarChart = new Chart(ctx, {
                            type: 'radar',
                            data: {
                                labels: ['Win Rate %', 'Headshot %', 'Entry Success %', '1v1 Clutch %'],
                                datasets: [{
                                    label: 'Player DNA',
                                    data: [data.winRate, data.hs, data.entry, data.clutch1v1],
                                    backgroundColor: 'rgba(255, 76, 76, 0.3)',
                                    borderColor: 'rgba(255, 76, 76, 1)',
                                    pointBackgroundColor: '#ffcc00',
                                    borderWidth: 2
                                }]
                            },
                            options: {
                                scales: {
                                    r: {
                                        angleLines: { color: 'rgba(255, 255, 255, 0.1)' },
                                        grid: { color: 'rgba(255, 255, 255, 0.1)' },
                                        pointLabels: { color: '#ffffff', font: { size: 13, weight: 'bold' } },
                                        ticks: { display: false, min: 0, max: 100 }
                                    }
                                },
                                plugins: { legend: { display: false } }
                            }
                        });
                    }
                </script>
            </body>
            </html>
            """;
    }
}