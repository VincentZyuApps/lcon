## 🚀 Quick Start

```bash
uv venv --python 3.13
uv pip install textual websocket-client
cd ./client
uv run python ./main.py
```

## 🌐 Environment Variables

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

**🐧 bash (Linux / macOS / WSL / Git Bash):**
```bash
LCON_HOST=192.168.1.100 LCON_PORT=58115 LCON_TOKEN=your_secret_token
uv run python ./main.py
```

**🪟 PowerShell (Windows):**
```powershell
$env:LCON_HOST="192.168.1.100"; $env:LCON_PORT="58115"; $env:LCON_TOKEN="your_secret_token"
uv run python ./main.py
```

**🖥️ CMD (Windows):**
```cmd
set LCON_HOST=192.168.1.100 && set LCON_PORT=58115 && set LCON_TOKEN=your_secret_token
uv run python ./main.py
```

> 💡 When using the Python TUI client, set `serializer_mode = "json"` in `lcon-ws-server.toml` for best compatibility.
