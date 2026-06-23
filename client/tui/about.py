from textual.widgets import Markdown
from textual.containers import Vertical
from textual.widget import Widget
from .css import load_css


class AboutTab(Widget):
    DEFAULT_CSS = load_css("about")

    def compose(self):
        with Vertical():
            yield Markdown()

    def on_mount(self):
        version = getattr(self.app, "_mod_version", "unknown")
        md = self.query_one(Markdown)
        md.update(f"""# LCon

**Version:** {version}

WebSocket remote control for Minecraft client.

A Forge mod that runs a WebSocket server on the Minecraft **client** (single-player / LAN), allowing external applications to execute commands and interact with the game in real time.

---

**Repository:** [github.com/VincentZyuApps/lcon](https://github.com/VincentZyuApps/lcon)

**Original Author:** [ZigTheHedge](https://github.com/ZigTheHedge)
""")
