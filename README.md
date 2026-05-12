# ⚡ Vitaly — CS2 Intelligence Engine & Web Dashboard

Vitaly is a high-performance, dual-engine Java application that serves as a professional Counter-Strike 2 analyst. It simultaneously operates an event-driven Discord Bot and a real-time RESTful web dashboard, leveraging the FACEIT API to provide deep, algorithmic insights into player performance and playstyle archetypes.

## ✨ Key Features

### 🤖 Discord Bot Commands
* **`!role <nickname>`** — Advanced playstyle analysis. Uses a custom classification engine to score players against 30+ unique CS2 archetypes (e.g., "Aggressive Entry", "Clutch Minister", "Supportive AWPer").
* **`!compare <p1> <p2>`** — Head-to-head "Boxing Match" comparison. Mathematically determines the superior player by awarding crowns 👑 to higher-tier stats.
* **`!maps <nickname>`** — Analyzes map-specific performance to expose a player's mastery levels and "auto-veto" weaknesses.
* **`!stats <nickname>`** — Combat dashboard displaying K/D, ADR, Headshot %, Utility Damage, and Clutch success rates.
* **`!advanced <nickname>`** — Deep career scan utilizing custom data aggregation for multi-kill highlights (Aces/Quads), win streaks, and MVPs.
* **`!leaderboard`** — Persistent server ranking system pulled from a local SQLite database, ranked by ELO.

### 🌐 Web Dashboard (Single Page Application)
* **Live Analytics:** Interactive UI built with **Javalin** to search and analyze players globally.
* **Player DNA:** Renders a normalized Hexagon Radar Chart using **Chart.js**. The chart uses a custom mathematical normalizer to scale disparate stats (like K/D vs ADR) into a consistent skill footprint.
* **Tabbed Interface:** Dedicated views for Combat Stats, Map Mastery, and Head-to-Head comparisons.

## 🛠️ Tech Stack
* **Core:** Java 17+ (OOP focused architecture).
* **Bot Engine:** JDA (Java Discord API) for asynchronous event handling.
* **Web Engine:** Javalin Lightweight REST framework.
* **Data Layer:** SQLite (JDBC) with a singleton pattern for thread-safe persistent storage.
* **Data Processing:** Google Gson for parsing complex, nested JSON responses.
* **Frontend:** HTML5, CSS3 (Esports aesthetic), and Chart.js for data visualization.

## 🧠 Software Engineering Highlights
* **Dependency Injection:** Centralized `Data_Model` shared across both the Bot and Web engines to ensure thread safety and resource efficiency.
* **Data Aggregation:** Built a custom aggregator to calculate "Advanced Stats" (Aces, MVPs, etc.) by iterating through map-specific API segments, bypassing limitations in the standard FACEIT lifetime data blocks.
* **Normalized Scoring:** Implemented a normalization algorithm in JavaScript to ensure radar charts accurately reflect skill variance relative to professional averages.

## 🚀 Deployment & Hosting
* **Platform:** Deployed 24/7 on **Railway.app**.
* **Persistence:** Configured with Railway Volumes to ensure the `vitaly.db` file survives cloud container restarts.
* **Dynamic Networking:** Implemented environment-based port binding (`System.getenv("PORT")`) for seamless local and cloud transitions.

## ⚙️ Setup & Installation

### 1. Prerequisites
* **JDK 17+** and **Maven** installed.
* **Discord Bot Token** ([Developer Portal](https://discord.com/developers/applications)).
* **FACEIT API Key** ([Developer Portal](https://developers.faceit.com/)).

### 2. Execution
Clone the repository and set up your `.env` file:
```env
DISCORD_TOKEN=your_token_here
FACEIT_API_KEY=your_key_here
PORT=8080
