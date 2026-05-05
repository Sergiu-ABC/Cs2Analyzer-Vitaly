# ⚡ Vitaly — CS2 Intelligence Bot & Web Dashboard

Vitaly is a full-stack, dual-engine Java application that acts as a personal Counter-Strike 2 analyst. It simultaneously runs an event-driven Discord Bot and a local RESTful web server, consuming the FACEIT API to provide deep, algorithmic insights into a player's performance.

## ✨ Key Features

### 🤖 Discord Bot Commands
* **`!role <nickname>`** — AI-powered playstyle analysis. Uses a custom algorithm to score players against 30+ unique CS2 archetypes (e.g., "Aggressive Entry", "Clutch Minister", "Supportive AWPer").
* **`!compare <player1> <player2>`** — Head-to-head boxing match embed that mathematically proves who is better by awarding stat crowns 👑.
* **`!maps <nickname>`** — Analyzes individual map segments to instantly expose a player's best maps and their "auto-veto" weaknesses.
* **`!stats <nickname>`** — Full combat dashboard displaying K/D, ADR, Headshot %, Utility Damage, and Clutch Win Rates.
* **`!advanced <nickname>`** — Deep career scan showing lifetime kills, multi-kill highlights (Aces/Quads), win streaks, and MVPs.
* **`!leaderboard`** — Automatically saves scanned players to a local SQLite database and ranks the server by ELO.

### 🌐 Web Dashboard (Local API)
* Runs a local HTTP server using **Javalin** (`http://localhost:8080`).
* Provides an interactive UI to search for players.
* Renders a dynamic "Player DNA" Radar Chart using **Chart.js** based on Win Rate, Headshot %, Entry Success, and 1v1 Clutch stats.

## 🛠️ Tech Stack
* **Java 17+**
* **JDA (Java Discord API):** For event-driven Discord integrations.
* **Javalin:** Lightweight web framework for REST API routing and static HTML serving.
* **Gson:** Parsing complex, nested JSON responses from the FACEIT API.
* **SQLite (JDBC):** Local, persistent database for the server leaderboard.
* **Dotenv:** Secure environment variable management.
* **Chart.js:** Frontend data visualization.

## 🚀 Setup & Installation

### 1. Prerequisites
* [Java Development Kit (JDK) 17+](https://adoptium.net/)
* Maven installed
* A Discord Bot Token (from the [Discord Developer Portal](https://discord.com/developers/applications))
* A FACEIT Server API Key (from the [FACEIT Developer Portal](https://developers.faceit.com/))

### 2. Clone the Repository
```bash
git clone [https://github.com/YOUR-USERNAME/Vitaly-CS2Analyzer.git](https://github.com/YOUR-USERNAME/Vitaly-CS2Analyzer.git)
cd Vitaly-CS2Analyzer
