> **[📖 English](README.md)**
> **[📖 中文](README.zh-cn.md)**

[![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/VincentZyuApps/lcon)
[![Gitee](https://img.shields.io/badge/Gitee-C71D23?style=for-the-badge&logo=gitee&logoColor=white)](https://gitee.com/vincent-zyu/lcon)

![lcon](https://socialify.git.ci/VincentZyuApps/lcon/image?custom_description=Enables+a+WebSocket+Server+on+the+Minecraft+client+for+remote+command+execution+and+chat+control&custom_language=Java&description=1&font=JetBrains+Mono&forks=1&issues=1&language=1&logo=https%3A%2F%2Fgithub.com%2FVincentZyuApps%2Flcon%2Fblob%2Fmaster%2Fdocs%2Fimages%2Fminecraft-forge-solid.png%3Fraw%3Dtrue&name=1&owner=1&pulls=1&stargazers=1&theme=Auto)

# 🎮 LCon — WebSocket remote control for Minecraft client

> 🧩 A Forge mod that runs a WebSocket server on the Minecraft **client** (single-player / LAN), allowing external applications to execute commands and interact with the game in real time.
<p align="center">
  <img src="logo.png" width="200" alt="LCon logo" />
</p>

>
> 💡 **How it works** — When you play single-player or open to LAN, your client runs an **integrated server** underneath — the same command engine, world ticking, and gameplay loop as a dedicated server. 

> 
> ☝️🤓 In Minecraft, "single-player", "multiplayer", "LAN", and "server" all run the same server code — there's no essential difference. LCon taps into this integrated server and starts a WebSocket server alongside it, so external tools can control the game without needing a separate dedicated server.

[![Forge 1.20.1](https://img.shields.io/badge/Forge-1.20.1-FF6600?style=for-the-badge&logo=data:image/svg%2bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCA2NCA2NCI+PHBhdGggZD0iTTI3LDIwIEwyMywyMSBMOCwyMSBMMTMsMjYgTDE5LDMwIEwyOSwzMSBMMjgsMzYgTDI0LDM4IEwyMiw0MyBMMjIsNDQgTDI4LDQ0IEwyOSw0MyBMNDIsNDMgTDQzLDQ0IEw0OSw0NCBMNDgsMzkgTDQyLDM2IEw0MiwyOSBMNDcsMjUgTDUwLDIzIEw1NywyMSBMNTcsMjAgWiIgZmlsbD0iI2ZmZiIgdHJhbnNmb3JtPSJtYXRyaXgoMS40IDAgMCAxLjQgLTEzIC0xMikiLz48L3N2Zz4=)](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html)
[![Java 17](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=data:image/svg%2bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZD0iTTIwIDNINHYxMGMwIDIuMjEgMS43OSA0IDQgNGg2YzIuMjEgMCA0LTEuNzkgNC00di0zaDJjMS4xMSAwIDItLjg5IDItMlY1YzAtMS4xMS0uODktMi0yLTJ6bTAgNWgtMlY1aDJ2M3pNMiAyMWgxOHYtMkgydjJ6IiBmaWxsPSIjZmZmIi8+PC9zdmc+)](https://adoptium.net/temurin/releases/?version=17)
[![Gradle 8.1.1](https://img.shields.io/badge/Gradle-8.1.1-02303A?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org)

[![Python TUI Client](https://img.shields.io/badge/Python_TUI_Client-3776AB?style=for-the-badge&logo=python&logoColor=white)](https://github.com/VincentZyuApps/lcon/releases)

[![Last Commit](https://img.shields.io/github/last-commit/VincentZyuApps/lcon?style=plastic&label=Last%20Commit&color=6e7681&labelColor=181717)](https://github.com/VincentZyuApps/lcon/commits/master)
[![CI Status](https://img.shields.io/github/actions/workflow/status/VincentZyuApps/lcon/build.yml?style=plastic&logo=githubactions&logoColor=white&label=CI%20Status&labelColor=181717)](https://github.com/VincentZyuApps/lcon/actions)
## 🧩 What it does

LCon starts a **WebSocket server** inside your Minecraft client when you're in a world (single-player or LAN). You can connect to it from any WebSocket client — a Python script 🤖, a chatbot 💬, a web dashboard 📊 — and:

| 🏷️ Prefix | ⚡ Action |
|--------|--------|
| `[chat]<message>` | Send a chat message as the player |
| `[message]<message>` | Display a message to the player only |
| `[system]<message>` | Display a system message in chat |
| `[client]/<command>` | Execute a **client-side** command |
| `[server]/<command>` | Execute a **server-side** command |

✅ No Mixin, no coremod, no overwrites — purely event-driven, safe for any modpack.

## 🔌 How to connect

### 📦 Mod Installation

1. Download the latest `.jar` from [Releases](https://github.com/VincentZyuApps/lcon/releases)
2. Place it in your client's `mods/` folder
3. Launch Minecraft — `lcon-ws-server.toml` is auto-generated in `config/` on first load

> ⚠️ **`mods/` and `config/` must be at the same directory level.**  
> Typical structure: `.minecraft/mods/lcon-*.jar` + `.minecraft/config/lcon-ws-server.toml`

### 🐍 Using Python (uv)

```bash
uv venv --python 3.13
uv pip install websocket-client
uv run python -c "
import websocket; ws = websocket.create_connection('ws://localhost:58115?token=your_secret_token')
while True:
  msg = ws.recv(); print(msg)
  if '✅' in msg or 'ready' in msg: break
ws.send('[server]/say awa!'); print(ws.recv()); ws.close()
"
```

<p align="center">
  <img src="docs/images/previews/preview.uv.run.python.cli.ws.client.png" alt="Python uv CLI WebSocket client" width="800" />
</p>

### 🪢 Using wscat via npx

```bash
npx wscat -c ws://localhost:58115?token=your_secret_token
```

Once connected, you'll receive welcome messages from the server:

```log
< 🎉 200:Welcome to LCon! Have fun! Don't forget to use prefixes with every message you send to me.
< 📋 200:Valid prefixes:
< 💬 200:[chat] - send message to Minecraft chat.
< 📩 200:[message] - display message for player only.
< 🔔 200:[system] - display system message in chat (for player only).
< 🖥️ 200:[client] - execute client-side command.
< 🖧 200:[server] - execute server-side command.
< ✅ 201:ready.
```

<p align="center">
  <img src="docs/images/previews/preview.npx.wscat.cli.ws.client.png" alt="npx wscat CLI WebSocket client" width="800" />
</p>

Then send commands with prefixes (`> ` is what you type, `< ` is the server response):

```powershell
> [server]/say Hello everyone!
# (command executed — no response text, but the chat message appears in-game)

> [chat]Hello!
# (message sent as the player in chat)

> [server]/give @s diamond 64
# (command executed — diamonds appear in your inventory)

> unknown-qwq
< ❌ 400:Error! Send message prefix first! [chat], [message], [system], [client], [server] are valid prefixes.
```

### 🐍 Python Client (TUI)

A Textual-based terminal UI with tabbed interface (Console, Commands, Settings, About).

<p align="center">
  <img src="docs/images/previews/preview.textual.tui.ws.client.png" alt="LCon Python TUI Client Preview" width="800" />
</p>

#### 🚀 Quick Start

```bash
git clone https://github.com/VincentZyuApps/lcon.git
cd lcon

uv venv --python 3.13
uv pip install textual websocket-client

uv run python ./client/main.py
```

#### 🌐 Environment Variables

| 🏷️ Variable | 📄 Default | 📝 Description |
|----------|---------|-------------|
| `LCON_HOST` | `localhost` | WebSocket server address |
| `LCON_PORT` | `58115` | WebSocket server port |
| `LCON_TOKEN` | `your_secret_token` | Authentication token |
| `LCON_SOFT_WRAP` | `true` | Enable soft word wrap in console log (`true`/`false`) |
| `LCON_LOG_BUFFER` | `1000` | Maximum lines to keep in console log buffer |
| `LCON_AUTO_MODE` | `true` | Auto-prepend prefix before sending messages (`true`/`false`) |

### 📄 .env File (optional)

Copy the example and edit:

```bash
cp config/.env.example config/.env
```

Priority: environment variable → `config/.env` → hardcoded default.

**bash (Linux / macOS / WSL / Git Bash):**
```bash
LCON_HOST=192.168.1.100 LCON_PORT=58115 LCON_TOKEN=your_secret_token
uv run python client/main.py
```

**PowerShell (Windows):**
```powershell
$env:LCON_HOST="192.168.1.100"; $env:LCON_PORT="58115"; $env:LCON_TOKEN="your_secret_token"
uv run python client/main.py
```

**CMD (Windows):**
```cmd
set LCON_HOST=192.168.1.100 && set LCON_PORT=58115 && set LCON_TOKEN=your_secret_token
uv run python client/main.py
```

## ⚙️ Configuration

File: `.minecraft/config/lcon-ws-server.toml`

| ⚙️ Option | 🏷️ Type | 📄 Default | 📝 Description |
|--------|------|---------|-------------|
| `enable_mod` | boolean | `true` | Enable the WebSocket server |
| `port` | int | `58115` | WebSocket server port |
| `token` | string | `your_secret_token` | Auth token. Clients pass `?token=xxx` on connect |
| `command_permission_level` | int | `4` | OP level for `[server]` commands (0-4). 4 = full access without enabling cheats |
| `serializer_mode` | string | `json` | Component serialization: `json` (recommended for Python TUI) or `tostring` |
| `enable_message_emoji` | boolean | `true` | Master switch for message emoji |
| `emoji_*` | string | various | 13 per-message emoji settings (e.g. `emoji_welcome`, `emoji_chat`) |
| `msg_*` | string | various | 13 per-message text settings (e.g. `msg_welcome`, `msg_chat`) |

> 💡 When using the Python TUI client, set `serializer_mode = "json"` in `lcon-ws-server.toml` for best compatibility.

## 🏗 Build

### 🛠️ Local

```bash
./gradlew build
```

📦 Output: `build/libs/lcon-*.jar`

### 🤖 GitHub Actions CI

Push to any branch with specific keywords in the commit message:

| Commit message contains | What happens |
|------------------------|-------------|
| `build action` | Build + upload artifact |
| `build release` | Build + create GitHub Release |

```bash
git commit -m "fix: something; build action"
git commit -m "feat: something; build release"
```

## 📦 Tech Stack

### 🖥️ Server (Forge Mod)

| Dependency | Version | Description |
|:---|---:|:---|
| [![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://adoptium.net/temurin/releases/?version=17) | 17 | Runtime |
| [![Forge](https://img.shields.io/badge/Forge-1.20.1--47.2.19-FF6600?style=flat-square)](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html) | 47.2.19 | Mod loader |
| [![Gradle](https://img.shields.io/badge/Gradle-8.1.1-02303A?style=flat-square&logo=gradle&logoColor=white)](https://gradle.org) | 8.1.1 | Build tool |
| [![Shadow](https://img.shields.io/badge/Shadow-8.1.1-ED8B00?style=flat-square)](https://imperceptiblethoughts.com/shadow/) | 7.1.0 | Fat-jar plugin |
| [![Java-WebSocket](https://img.shields.io/badge/Java--WebSocket-1.5.6-ED8B00?style=flat-square)](https://github.com/TooTallNate/Java-WebSocket) | 1.5.6 | WebSocket server (fat-jarred) |
| [![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white)](https://github.com/VincentZyuApps/lcon/actions) | — | GitHub CI/CD |

### 🐍 Client (Python TUI)

| Dependency | Version | Description |
|:---|---:|:---|
| [![Python](https://img.shields.io/badge/Python-3.13-3776AB?style=flat-square&logo=python&logoColor=white)](https://python.org) | 3.13 | Runtime |
| [![textual](https://img.shields.io/badge/textual-≥8.2-FFD43B?style=flat-square)](https://github.com/textualize/textual) | ≥8.2 | TUI framework |
| [![websocket-client](https://img.shields.io/badge/websocket--client-≥1.9-FFD43B?style=flat-square)](https://github.com/websocket-client/websocket-client) | ≥1.9 | WebSocket client |
