# feat: Python TUI client, Github Action CI/CD pipeline, token auth, JSON serializer, configurable messages, server command enhancement, and bilingual docs

> my repo url: https://github.com/VincentZyuApps/lcon

---
## PR Description


### 🚀 Feat

- **Python TUI Client** (brand new) — Textual-based terminal UI with tabbed interface: Console, Prefixes, Settings, About. Supports Copy/Clear/Mode top-bar buttons, Ctrl+M cycle prefix, prefix auto-complete (PrefixSuggester), connection status spinner, and 15s connection timeout. Includes `client/README.md` with dedicated docs, `config/.env.example` with dotenv support (`_load_dotenv()`, priority: env var > `.env` > default), and env vars: `LCON_HOST`, `LCON_PORT`, `LCON_TOKEN`, `LCON_SOFT_WRAP`, `LCON_LOG_BUFFER`, `LCON_AUTO_MODE`.
- **CI/CD Workflow** — GitHub Actions pipeline (`.github/workflows/build.yml` + `release_template.md`) triggered by commit keywords: `build action` (build + upload artifact), `build release` (build + create GitHub Release).
- **Token Authentication** — `TOKEN` config option in `lcon-ws-server.toml`. Clients pass `?token=xxx` on connect; unauthorized connections are rejected with an error message.
- **JSON Serialization Mode** — `serializer_mode` config option (`json`/`tostring`), defaults to `Component.Serializer.toJson()` for standard Minecraft JSON output. Python TUI clients can `json.loads()` the incoming chat messages.
- **Fully Configurable Messages** — 27 new config options: `ENABLE_MESSAGE_EMOJI` master switch, 13 `emoji_*` and 13 `msg_*` settings. `fmt()` helper respects the emoji toggle per message.
- **Server Command Enhancement** — `[server]` prefix now uses `performPrefixedCommand()` with `createCommandSourceStack().withPermission(level)`, bypassing the need for `/enablecheats`. Configurable via `command_permission_level` (default 4).
- **Config File Rename** — `lcon-client.toml` → `lcon-ws-server.toml` for clarity.
- **Bilingual README** — `README.md` (English) and `README.zh-cn.md` (Chinese), each 217 lines with installation, usage, env var reference, config table, and build instructions.

### 🐛 Fix

- **Shadow jar SLF4J exclusion** — Excluded `org.slf4j` from `minecraftLibrary` and removed `relocate 'org.slf4j'` in `build.gradle` to resolve `NoSuchMethodError` with `LogUtils.getLogger()`.
- **RemotePlayer ClassCastException** — Changed `(LocalPlayer) event.player` to `instanceof LocalPlayer player` pattern guard in `clientTick()`, fixing crashes when a RemotePlayer joins via LAN.
- **Rich markup escaping** — Applied `rich.markup.escape()` to `[]` brackets in Prefixes DataTable to prevent Rich from interpreting `[chat]` as markup.

### 📦 Chore

- **.gitignore overhaul** — Categorized sections for Gradle, Minecraft, IDE, OS, Python, env secrets, temp, and log files.
- **Version bump** — `1.0.3` → `1.2.0` in `gradle.properties`.
- **Java source documentation** — Added detailed Chinese emoji comments to all 7 Java source files.
- **Utilities** — Added `scripts/image_to_badge.py` for PNG-to-SVG shield.io badge generation, `docs/images/` for mod logos.

---

### Additional Requests for Upstream (@ZigTheHedge)

1. **Update Modrinth README**: The Modrinth project page currently has minimal description. I suggest updating it with the bilingual README from this repository, which includes comprehensive feature docs, installation guides, and Python TUI client usage.

2. **Publish new build on Modrinth**: Several releases have been made since the last Modrinth publish (latest: v1.20.1-1.2.0). Pre-built JARs are available in the [Releases](https://github.com/VincentZyuApps/lcon/releases) section. Would be great to upload the latest version so users can install via Modrinth launchers.

3. **Alternative — Grant Modrinth project permissions**: If you'd prefer not to handle publishing manually, could you add me as a collaborator on the Modrinth project? I have a GitHub Actions workflow ready with Modrinth publish support (using `modrinth-token`). This would let CI automatically publish new versions on each release. Happy to handle it either way — just let me know what works best for you.

Thanks for creating this excellent mod!