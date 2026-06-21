from textual.widgets import RichLog, Input, Button, Static
from textual.containers import Horizontal
from textual.widget import Widget
from textual.binding import Binding
from textual.suggester import Suggester
from .css import load_css


class PrefixSuggester(Suggester):
    PREFIXES = ["[chat]", "[message]", "[system]", "[client]", "[server]"]

    async def get_suggestion(self, value: str) -> str | None:
        if not value.startswith("["):
            return None
        for prefix in self.PREFIXES:
            if prefix.startswith(value):
                return prefix[len(value) :]
        return None


class PrefixInput(Input):
    def __init__(self, *args, **kwargs):
        kwargs.setdefault("suggester", PrefixSuggester())
        super().__init__(*args, **kwargs)


SPINNER_CHARS = ["⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"]


class ConsoleTab(Widget):
    PREFIXES = ["server", "client", "chat", "message", "system"]
    PREFIX_LABELS = ["SERVER", "CLIENT", "CHAT", "MESSAGE", "SYSTEM"]

    DEFAULT_CSS = load_css("console")

    BINDINGS = [
        Binding("ctrl+a", "toggle_auto", "Auto"),
        Binding("ctrl+m", "cycle_prefix", "Mode"),
        Binding("ctrl+slash", "toggle_auto", "Slash"),
    ]

    def __init__(self):
        super().__init__()
        self._prefix_idx = 0
        self._auto_on = True
        self._status = "connecting"
        self._spinner_idx = 0
        self._spinner_timer = None

    def compose(self):
        yield RichLog(id="log", highlight=True, markup=True, max_lines=10000)
        yield Static(id="mode-bar")
        with Horizontal(id="input-row"):
            yield Static(id="prefix-label")
            yield PrefixInput(id="input", placeholder="Type a command...")
            yield Button("Send", id="send", variant="primary")

    def on_mount(self):
        self._spinner_timer = self.set_interval(0.3, self._tick_spinner)
        self._update_ui()
        self.add_log("[dim]⏳ Connecting...[/dim]")

    def add_log(self, message):
        try:
            self.query_one("#log", RichLog).write(message)
        except Exception:
            pass

    def on_input_submitted(self, event):
        self._send_message()

    def on_button_pressed(self, event):
        if event.button.id == "send":
            self._send_message()

    def _cycle_prefix(self, direction=1):
        count = len(self.PREFIXES)
        self._prefix_idx = (self._prefix_idx + direction) % count
        self._update_ui()

    def _toggle_auto(self):
        self._auto_on = not self._auto_on
        self._update_ui()

    def set_connection_status(self, status):
        self._status = status
        if status == "connecting":
            self._spinner_idx = 0
            self._spinner_timer = self.set_interval(0.3, self._tick_spinner)
        else:
            if self._spinner_timer:
                self._spinner_timer.stop()
                self._spinner_timer = None
        self._update_ui()

    def _tick_spinner(self):
        self._spinner_idx = (self._spinner_idx + 1) % len(SPINNER_CHARS)
        self._update_ui()

    def _update_ui(self):
        prefix = self.PREFIXES[self._prefix_idx]
        label = self.PREFIX_LABELS[self._prefix_idx]
        try:
            self.query_one("#prefix-label", Static).update(f"[{prefix}]")
        except Exception:
            pass

        mode_part = f"Mode: {label} | Auto: {'ON' if self._auto_on else 'OFF'}"

        if self._status == "connecting":
            spinner = SPINNER_CHARS[self._spinner_idx]
            status_line = f"▸ {spinner} Connecting... | {mode_part}"
        elif self._status == "connected":
            status_line = f"▸ 🟢 Connected | {mode_part}"
        else:
            status_line = f"▸ 🔴 Disconnected | {mode_part}"

        try:
            mode_bar = self.query_one("#mode-bar", Static)
            mode_bar.update(status_line)
            mode_bar.remove_class("auto-on", "auto-off")
            mode_bar.add_class("auto-on" if self._auto_on else "auto-off")
        except Exception:
            pass

    def _send_message(self):
        if not self.app.ws or not self.app.ws.running:
            self.add_log("[red]❌ Not connected[/red]")
            return
        inp = self.query_one("#input", PrefixInput)
        msg = inp.value.strip()
        if not msg:
            return
        prefix = self.PREFIXES[self._prefix_idx]
        full_msg = f"[{prefix}]{msg}" if self._auto_on else msg
        self.app.ws.send(full_msg)
        self.add_log(f"> {full_msg}")
        inp.value = ""
