package org.example;

public class DashboardHtml {

    public static String getLayout() {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Vitaly | CS2 Intelligence</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <link href="https://fonts.googleapis.com/css2?family=Rajdhani:wght@400;600;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg-main: #0d0d12;
            --bg-panel: rgba(20, 20, 25, 0.8);
            --accent: #ff4c4c;
            --accent-hover: #cc0000;
            --text-main: #ffffff;
            --text-muted: #888888;
            --border: rgba(255, 255, 255, 0.1);
        }
        * { box-sizing: border-box; }
        body {
            font-family: 'Rajdhani', sans-serif;
            background: radial-gradient(circle at top center, #1a1a24 0%, var(--bg-main) 100%);
            color: var(--text-main);
            margin: 0;
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            align-items: center;
        }
        .navbar {
            width: 100%;
            background: rgba(0, 0, 0, 0.5);
            border-bottom: 1px solid var(--border);
            padding: 15px 40px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            backdrop-filter: blur(10px);
            position: sticky;
            top: 0;
            z-index: 100;
        }
        .brand {
            font-size: 2rem;
            font-weight: 700;
            color: var(--accent);
            text-shadow: 0 0 10px rgba(255, 76, 76, 0.4);
            letter-spacing: 2px;
            text-transform: uppercase;
        }
        .nav-links {
            display: flex;
            gap: 20px;
        }
        .nav-btn {
            background: none;
            border: none;
            color: var(--text-muted);
            font-family: 'Rajdhani', sans-serif;
            font-size: 1.1rem;
            font-weight: 600;
            text-transform: uppercase;
            cursor: pointer;
            padding: 10px 15px;
            border-bottom: 2px solid transparent;
            transition: 0.3s;
        }
        .nav-btn:hover, .nav-btn.active {
            color: var(--text-main);
            border-bottom: 2px solid var(--accent);
        }
        .content-wrapper {
            width: 100%;
            max-width: 1200px;
            padding: 40px 20px;
            display: flex;
            flex-direction: column;
            align-items: center;
        }
        .search-box {
            display: flex;
            gap: 10px;
            width: 100%;
            max-width: 800px;
            margin-bottom: 40px;
        }
        input {
            flex: 1;
            padding: 15px 20px;
            font-size: 1.2rem;
            font-family: 'Rajdhani', sans-serif;
            font-weight: 600;
            border-radius: 4px;
            border: 1px solid #333;
            outline: none;
            background: rgba(255, 255, 255, 0.05);
            color: white;
            transition: border-color 0.3s;
        }
        input:focus { border-color: var(--accent); }
        
        select#timePeriod {
            padding: 15px; 
            font-size: 1.1rem; 
            font-family: 'Rajdhani'; 
            font-weight: 600; 
            background: rgba(255, 255, 255, 0.05); 
            color: white; 
            border: 1px solid #333; 
            border-radius: 4px; 
            outline: none; 
            cursor: pointer;
            transition: border-color 0.3s;
        }
        select#timePeriod:focus { border-color: var(--accent); }
        select#timePeriod option { background: var(--bg-main); color: white; }

        button.action-btn {
            padding: 15px 30px;
            font-size: 1.2rem;
            font-family: 'Rajdhani', sans-serif;
            font-weight: 700;
            cursor: pointer;
            background: linear-gradient(45deg, var(--accent), var(--accent-hover));
            color: white;
            border: none;
            border-radius: 4px;
            text-transform: uppercase;
            transition: 0.2s;
        }
        button.action-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(255, 76, 76, 0.4);
        }
        #errorMsg { color: var(--accent); font-size: 1.2rem; font-weight: bold; margin-bottom: 20px; display: none; }

        .tab-content {
            display: none;
            width: 100%;
            background: var(--bg-panel);
            border: 1px solid var(--border);
            border-radius: 12px;
            padding: 40px;
            backdrop-filter: blur(10px);
            animation: fadeIn 0.3s ease-in-out;
        }
        .tab-content.active { display: block; }
        @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

        .player-header {
            display: flex;
            align-items: center;
            gap: 20px;
            margin-bottom: 30px;
            padding-bottom: 20px;
            border-bottom: 1px solid var(--border);
        }
        .avatar { width: 80px; height: 80px; border-radius: 50%; border: 2px solid var(--accent); }
        .player-info h2 { margin: 0; font-size: 2.5rem; color: #ffcc00; }
        .player-info p { margin: 5px 0 0 0; font-size: 1.2rem; color: var(--text-muted); font-weight: 600; }

        .esports-layout { display: flex; justify-content: space-between; align-items: center; gap: 30px; }
        .stat-column { display: flex; flex-direction: column; gap: 20px; width: 30%; }
        .stat-box { background: rgba(0, 0, 0, 0.4); border-left: 4px solid var(--accent); padding: 20px; border-radius: 4px; }
        .stat-box.blue { border-color: #00ccff; }
        .stat-box.purple { border-color: #9d00ff; }
        .stat-value { font-size: 2.5rem; font-weight: 700; line-height: 1; margin-bottom: 5px; }
        .stat-label { font-size: 1rem; color: var(--text-muted); text-transform: uppercase; font-weight: 600; }
        
        .chart-center { width: 40%; max-width: 400px; height: 350px; position: relative; }

        .combat-grid, .advanced-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
        .bar-container { margin-top: 10px; width: 100%; background: rgba(255,255,255,0.1); height: 6px; border-radius: 3px; overflow: hidden; }
        .bar-fill { height: 100%; background: var(--accent); transition: width 1s ease-in-out; }
        .bar-fill.blue { background: #00ccff; }

        .maps-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; }
        .map-card { background: rgba(0,0,0,0.4); padding: 20px; border-radius: 8px; border: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center; }
        .map-card.best { border-color: #ffcc00; background: rgba(255,204,0,0.05); }
        .map-card.worst { border-color: var(--accent); background: rgba(255,76,76,0.05); }

        .compare-layout { display: flex; gap: 20px; margin-bottom: 30px; justify-content: center; }
        .cmp-table { width: 100%; background: rgba(0,0,0,0.4); border-radius: 8px; overflow: hidden; }
        .cmp-row { display: flex; border-bottom: 1px solid var(--border); }
        .cmp-row:last-child { border-bottom: none; }
        .cmp-val { flex: 1; padding: 15px; text-align: center; font-size: 1.5rem; font-weight: bold; }
        .cmp-val.win { color: #ffcc00; background: rgba(255,204,0,0.1); }
        .cmp-label { flex: 1; padding: 15px; text-align: center; font-size: 1.1rem; color: var(--text-muted); text-transform: uppercase; background: rgba(255,255,255,0.02); }

        .leaderboard-table { width: 100%; border-collapse: collapse; }
        .leaderboard-table th, .leaderboard-table td { padding: 15px; text-align: left; border-bottom: 1px solid var(--border); font-size: 1.2rem; }
        .leaderboard-table th { color: var(--text-muted); text-transform: uppercase; }
        .leaderboard-table tr:hover { background: rgba(255,255,255,0.05); }
        .rank-1 { color: #ffcc00; font-weight: bold; font-size: 1.5rem; }
        .rank-2 { color: #c0c0c0; font-weight: bold; font-size: 1.5rem; }
        .rank-3 { color: #cd7f32; font-weight: bold; font-size: 1.5rem; }

        .role-banner {
            font-size: 1.3rem;
            line-height: 1.6;
            color: #ddd;
            padding: 15px;
            background: rgba(0, 0, 0, 0.3);
            border-radius: 6px;
            border-left: 4px solid #ffcc00;
            margin-bottom: 20px;
        }
    </style>
</head>
<body>
    <nav class="navbar">
        <div class="brand">VITALY ENGINE</div>
        <div class="nav-links">
            <button class="nav-btn active" onclick="switchTab('tab-dna', this)">DNA</button>
            <button class="nav-btn" onclick="switchTab('tab-combat', this)">Combat</button>
            <button class="nav-btn" onclick="switchTab('tab-advanced', this)">Advanced</button>
            <button class="nav-btn" onclick="switchTab('tab-maps', this)">Maps</button>
            <button class="nav-btn" onclick="switchTab('tab-compare', this)">Compare</button>
            <button class="nav-btn" onclick="switchTab('tab-leaderboard', this)">Leaderboard</button>
        </div>
    </nav>

    <div class="content-wrapper">
        <div id="errorMsg"></div>

        <div id="tab-dna" class="tab-content active">
            <div class="search-box">
                <input type="text" id="searchInput" placeholder="ENTER FACEIT NAME..." onkeypress="if(event.key === 'Enter') loadPlayer()">
                <select id="timePeriod">
                    <option value="0">Lifetime</option>
                    <option value="30">Last 30 Days</option>
                    <option value="90">Last 90 Days</option>
                    <option value="180">Last 6 Months</option>
                </select>
                <button class="action-btn" onclick="loadPlayer()">SCAN</button>
            </div>

            <div id="dna-container" style="display:none;">
                <div class="player-header">
                    <img id="p-avatar" class="avatar" src="" alt="avatar">
                    <div class="player-info">
                        <h2 id="p-name"></h2>
                        <p>Level <span id="p-level"></span> • <span id="p-elo"></span> ELO</p>
                    </div>
                </div>

                <div class="role-banner" id="roleText"></div>

                <div class="esports-layout">
                    <div class="stat-column">
                        <div class="stat-box"><div class="stat-value" id="valKD"></div><div class="stat-label">Kill/Death Ratio</div></div>
                        <div class="stat-box"><div class="stat-value" id="valADR"></div><div class="stat-label">Average Damage</div></div>
                        <div class="stat-box"><div class="stat-value" id="valHS"></div><div class="stat-label">Headshot %</div></div>
                    </div>
                    <div class="chart-center"><canvas id="radarChart"></canvas></div>
                    <div class="stat-column">
                        <div class="stat-box blue"><div class="stat-value" id="valWin"></div><div class="stat-label">Win Rate %</div></div>
                        <div class="stat-box blue"><div class="stat-value" id="valEntry"></div><div class="stat-label">Entry Success %</div></div>
                        <div class="stat-box blue"><div class="stat-value" id="valClutch"></div><div class="stat-label">Clutch 1v1 %</div></div>
                    </div>
                </div>
            </div>
        </div>

        <div id="tab-combat" class="tab-content">
            <h2 style="color:var(--accent); margin-top:0;">COMBAT DASHBOARD</h2>
            <div class="combat-grid" id="combat-container"></div>
        </div>

        <div id="tab-advanced" class="tab-content">
            <h2 style="color:#9d00ff; margin-top:0;">CAREER DEEP SCAN</h2>
            <div class="advanced-grid" id="advanced-container"></div>
        </div>

        <div id="tab-maps" class="tab-content">
            <h2 style="color:#00ccff; margin-top:0;">MAP MASTERY</h2>
            <div class="maps-grid" id="maps-container"></div>
        </div>

        <div id="tab-compare" class="tab-content">
            <div class="compare-layout">
                <input type="text" id="cmp1" placeholder="Player 1">
                <h2 style="margin:0; padding:10px; color:#555;">VS</h2>
                <input type="text" id="cmp2" placeholder="Player 2">
                <button class="action-btn" onclick="loadCompare()">FIGHT</button>
            </div>
            <div id="compare-container"></div>
        </div>

        <div id="tab-leaderboard" class="tab-content">
            <h2 style="color:#ffcc00; margin-top:0;">SERVER LEADERBOARD</h2>
            <table class="leaderboard-table">
                <thead><tr><th>Rank</th><th>Player</th><th>ELO</th><th>K/D Ratio</th><th>Win Rate</th></tr></thead>
                <tbody id="leaderboard-body"></tbody>
            </table>
        </div>
    </div>

    <script>
        let currentData = null;
        let myRadarChart = null;

        function switchTab(tabId, btn) {
            document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
            document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
            document.getElementById(tabId).classList.add('active');
            btn.classList.add('active');
            document.getElementById('errorMsg').style.display = 'none';

            if (tabId === 'tab-leaderboard') fetchLeaderboard();
            else if (currentData && tabId !== 'tab-compare') populateTabs();
        }

        function showError(msg) {
            const err = document.getElementById('errorMsg');
            err.innerText = msg;
            err.style.display = 'block';
        }

        async function fetchAPI(url) {
            try {
                const res = await fetch(url);
                return await res.json();
            } catch (e) { return { success: false, error: "Network Error" }; }
        }
async function loadPlayer() {
const name = document.getElementById('searchInput').value;
const days = document.getElementById('timePeriod').value;\s
if (!name) return;
      document.getElementById('errorMsg').style.display = 'none';
        // --- NEW UI LOCK ---
          const btn = document.querySelector('.action-btn');
                  const originalText = btn.innerText;
         btn.innerText = "SCANNING...";
        btn.disabled = true;
         btn.style.opacity = "0.5";
         btn.style.cursor = "not-allowed"; 
const data = await fetchAPI('/api/player/' + name + '?days=' + days);
                                                           
btn.innerText = originalText;
      btn.disabled = false;
            btn.style.opacity = "1";
                 btn.style.cursor = "pointer";
                
         if (data.success) {
                   currentData = data;
            document.getElementById('dna-container').style.display = 'block';
               populateTabs();
           } else {
           showError(data.error);
                                                           }
              }

        function formatDiscordText(text) {
            return text.replace(/\\*\\*(.*?)\\*\\*/g, '<strong style="color: #ffcc00;">$1</strong>')
                       .replace(/\\n/g, '<br>');
        }

        function populateTabs() {
            if (!currentData) return;
            const d = currentData;

            document.getElementById('p-avatar').src = d.avatar || 'https://via.placeholder.com/80';
            document.getElementById('p-name').innerText = d.nickname;
            document.getElementById('p-level').innerText = d.level;
            document.getElementById('p-elo').innerText = d.elo;

            document.getElementById('valKD').innerText = d.kd.toFixed(2);
            document.getElementById('valADR').innerText = d.adr.toFixed(1);
            document.getElementById('valHS').innerText = d.hs + '%';
            document.getElementById('valWin').innerText = d.winRate + '%';
            document.getElementById('valEntry').innerText = d.entry + '%';
            document.getElementById('valClutch').innerText = d.clutch1v1 + '%';

            document.getElementById('roleText').innerHTML = formatDiscordText(d.role);

            drawChart(d);

            const cHtml = `
                ${makeBar('Entry Success', d.entry + '%', d.entry, 100, '')}
                ${makeBar('1v1 Clutch', d.clutch1v1 + '%', d.clutch1v1, 100, 'blue')}
                ${makeBar('1v2 Clutch', d.clutch1v2 + '%', d.clutch1v2, 100, 'blue')}
                ${makeBar('Utility Damage', d.utility, d.utility, 35, '')}
                ${makeBar('Flashes / Rnd', d.flashes, d.flashes, 1.2, '')}
                ${makeBar('Sniper KPR', d.sniper, d.sniper, 0.4, 'blue')}
            `;
            document.getElementById('combat-container').innerHTML = cHtml;

            const aHtml = `
                <div class="stat-box purple"><div class="stat-value">${d.streak}</div><div class="stat-label">Current Win Streak</div></div>
                <div class="stat-box purple"><div class="stat-value">${d.bestStreak}</div><div class="stat-label">Best Win Streak</div></div>
                <div class="stat-box purple"><div class="stat-value">${d.avgKills}</div><div class="stat-label">Avg Kills / Match</div></div>
                <div class="stat-box purple"><div class="stat-value">${d.totalKills}</div><div class="stat-label">Total Kills</div></div>
                <div class="stat-box purple"><div class="stat-value">${d.totalHs}</div><div class="stat-label">Total Headshots</div></div>
                <div class="stat-box purple"><div class="stat-value">${d.mvps}</div><div class="stat-label">Total MVPs</div></div>
                <div class="stat-box purple"><div class="stat-value">${d.aces}</div><div class="stat-label">Aces (5K)</div></div>
                <div class="stat-box purple"><div class="stat-value">${d.quads}</div><div class="stat-label">Quad Kills (4K)</div></div>
                <div class="stat-box purple"><div class="stat-value">${d.triples}</div><div class="stat-label">Triple Kills (3K)</div></div>
            `;
            document.getElementById('advanced-container').innerHTML = aHtml;

            let mHtml = '';
            if (d.maps && d.maps.length > 0) {
                d.maps.forEach((m, idx) => {
                    let cls = idx === 0 ? 'best' : (idx === d.maps.length - 1 ? 'worst' : '');
                    let icon = idx === 0 ? '👑' : (idx === d.maps.length - 1 ? '🗑️' : '🗺️');
                    mHtml += `
                        <div class="map-card ${cls}">
                            <div><div class="stat-value" style="font-size:1.8rem;">${icon} ${m.name}</div><div class="stat-label">${m.matches} Matches</div></div>
                            <div style="text-align:right;"><div class="stat-value" style="color:#fff;">${m.win}%</div><div class="stat-label">K/D: ${m.kd}</div></div>
                        </div>
                    `;
                });
            } else {
                mHtml = '<p>Not enough map data found.</p>';
            }
            document.getElementById('maps-container').innerHTML = mHtml;
        }

        function makeBar(label, valTxt, valNum, maxNum, colorCls) {
            let pct = Math.min(100, (valNum / maxNum) * 100);
            return `
                <div class="stat-box ${colorCls}">
                    <div style="display:flex; justify-content:space-between;">
                        <div class="stat-label">${label}</div>
                        <div class="stat-value" style="font-size:1.2rem;">${valTxt}</div>
                    </div>
                    <div class="bar-container"><div class="bar-fill ${colorCls}" style="width: ${pct}%"></div></div>
                </div>
            `;
        }

        function drawChart(d) {
            const ctx = document.getElementById('radarChart').getContext('2d');
            if (myRadarChart != null) myRadarChart.destroy();

            const norm = (v, min, max) => Math.max(0, Math.min(100, ((v - min) / (max - min)) * 100));

            const sKD = norm(d.kd, 0.7, 1.4);
            const sADR = norm(d.adr, 60, 100);
            const sHS = norm(d.hs, 35, 65);
            const sClutch = norm(d.clutch1v1, 35, 75);
            const sEntry = norm(d.entry, 35, 65);
            const sWin = norm(d.winRate, 45, 60);

            myRadarChart = new Chart(ctx, {
                type: 'radar',
                data: {
                    labels: ['K/D', 'ADR', 'Headshot %', 'Clutch %', 'Entry %', 'Win Rate %'],
                    datasets: [{
                        data: [sKD, sADR, sHS, sClutch, sEntry, sWin],
                        backgroundColor: 'rgba(255, 76, 76, 0.2)',
                        borderColor: 'rgba(255, 76, 76, 1)',
                        pointBackgroundColor: '#fff',
                        borderWidth: 2,
                        pointRadius: 4
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false, /* FIXED: This allows the chart to stretch to fit the container */
                    scales: { r: { angleLines: { color: 'rgba(255,255,255,0.1)' }, grid: { color: 'rgba(255,255,255,0.1)' }, pointLabels: { color: '#aaa', font: { family: 'Rajdhani', size: 14, weight: '600' } }, ticks: { display: false, min: 0, max: 100 } } },
                    plugins: { legend: { display: false }, tooltip: { enabled: false } }
                }
            });
        }

        async function loadCompare() {
            const n1 = document.getElementById('cmp1').value;
            const n2 = document.getElementById('cmp2').value;
            if (!n1 || !n2) return;

            document.getElementById('compare-container').innerHTML = '<h3 style="text-align:center;">Fetching Data...</h3>';

            const [d1, d2] = await Promise.all([ fetchAPI('/api/player/' + n1), fetchAPI('/api/player/' + n2) ]);
            if (!d1.success || !d2.success) {
                document.getElementById('compare-container').innerHTML = '<h3 style="color:var(--accent); text-align:center;">Error fetching one or both players.</h3>';
                return;
            }

            let html = `
                <div style="display:flex; justify-content:space-between; margin-bottom: 20px;">
                    <div style="text-align:center; flex:1;"><h2 style="margin:0; color:var(--accent);">${d1.nickname}</h2><p style="margin:0; color:var(--text-muted);">Level ${d1.level} (${d1.elo})</p></div>
                    <div style="text-align:center; flex:1;"><h2 style="margin:0; color:#00ccff;">${d2.nickname}</h2><p style="margin:0; color:var(--text-muted);">Level ${d2.level} (${d2.elo})</p></div>
                </div>
                <div class="cmp-table">
                    ${cmpRow('K/D Ratio', d1.kd, d2.kd, false)}
                    ${cmpRow('Win Rate %', d1.winRate, d2.winRate, false)}
                    ${cmpRow('ADR', d1.adr, d2.adr, false)}
                    ${cmpRow('Headshot %', d1.hs, d2.hs, false)}
                    ${cmpRow('Entry Success %', d1.entry, d2.entry, false)}
                    ${cmpRow('Clutch 1v1 %', d1.clutch1v1, d2.clutch1v1, false)}
                    ${cmpRow('Total Matches', d1.matches, d2.matches, false)}
                </div>
            `;
            document.getElementById('compare-container').innerHTML = html;
        }

        function cmpRow(label, v1, v2, invert) {
            let w1 = invert ? v1 < v2 : v1 > v2;
            let w2 = invert ? v2 < v1 : v2 > v1;
            let t1 = w1 ? `👑 ${v1}` : `${v1}`;
            let t2 = w2 ? `👑 ${v2}` : `${v2}`;
            return `
                <div class="cmp-row">
                    <div class="cmp-val ${w1?'win':''}">${t1}</div>
                    <div class="cmp-label">${label}</div>
                    <div class="cmp-val ${w2?'win':''}">${t2}</div>
                </div>
            `;
        }

        async function fetchLeaderboard() {
            const data = await fetchAPI('/api/leaderboard');
            let html = '';
            if (data && data.length > 0) {
                data.forEach((p, i) => {
                    let rCls = i === 0 ? 'rank-1' : (i === 1 ? 'rank-2' : (i === 2 ? 'rank-3' : ''));
                    let icon = i === 0 ? '🥇' : (i === 1 ? '🥈' : (i === 2 ? '🥉' : (i+1)));
                    html += `<tr><td class="${rCls}">${icon}</td><td><strong>${p.nickname}</strong></td><td>${p.elo}</td><td>${p.kd}</td><td>${p.winRate}%</td></tr>`;
                });
            } else {
                html = '<tr><td colspan="5" style="text-align:center;">Leaderboard is empty. Scan some players!</td></tr>';
            }
            document.getElementById('leaderboard-body').innerHTML = html;
        }
    </script>
</body>
</html>
""";
    }
}