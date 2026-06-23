import json


TRANSLATION_EN = {
    "chat.type.announcement": "[{1}] {2}",
    "chat.type.text": "<{1}> {2}",
    "chat.type.emote": " * {1} {2}",
    "chat.type.admin": "[{1}: {2}]",
    "commands.gamemode.success.self": "Set own game mode to {1}",
    "commands.gamemode.success.other": "Set {1}'s game mode to {2}",
    "commands.say": "[{1}] {2}",
    "commands.whisper.success": "{1} whispers to you: {2}",
    "commands.whisper.sender": "You whisper to {1}: {2}",
    "multiplayer.player.joined": "{1} joined the game",
    "multiplayer.player.left": "{1} left the game",
    "commands.give.success.single": "Gave {1} {2} to {3}",
    "commands.give.success.multiple": "Gave {1} {2} to {3} players",
    "commands.effect.give.success.single": "Applied effect {1} to {2}",
    "commands.teleport.success": "Teleported {1} to {2}, {3}, {4}",
    "commands.teleport.success.entity.single": "Teleported {1} to {2}",
    "commands.teleport.success.location": "Teleported {1} to {2}, {3}, {4}",
    "gameMode.survival": "Survival",
    "gameMode.creative": "Creative",
    "gameMode.adventure": "Adventure",
    "gameMode.spectator": "Spectator",
    "argument.entity.invalid": "Invalid entity",
    "argument.player.entities": "No player found",
    "argument.player.toomany": "Too many players",
    "commands.generic.player.unspecified": "Player not specified",
    "commands.generic.num.invalid": "Invalid number",
    "commands.enchant.success.single": "Enchanted {1}'s item",
    "commands.enchant.success.multiple": "Enchanted the item",
    "commands.xp.success.single": "Gave {1} experience to {2}",
    "commands.weather.set.clear": "Set weather to clear",
    "commands.weather.set.rain": "Set weather to rain",
    "commands.weather.set.thunder": "Set weather to thunder",
    "commands.time.set": "Set time to {1}",
    "commands.difficulty.success": "Set difficulty to {1}",
}


def flatten_components(text: str) -> str:
    if not text or text == '"empty"':
        return text
    text = text.strip().strip('"')
    if not text:
        return text
    return _flatten_json(text)


def _flatten_json(text: str) -> str:
    def walk(node):
        if isinstance(node, str):
            return node
        if isinstance(node, list):
            return "".join(walk(item) for item in node)
        if isinstance(node, dict):
            parts = []
            if "text" in node:
                parts.append(node["text"])
            if "translate" in node:
                key = node["translate"]
                with_args = node.get("with", [])
                rendered_args = [walk(a) for a in with_args]
                template = TRANSLATION_EN.get(key, key)
                for i, arg in enumerate(rendered_args, 1):
                    template = template.replace(f"{{{i}}}", arg, 1)
                parts.append(template)
            if "extra" in node:
                parts.append(walk(node["extra"]))
            return "".join(parts)
        return str(node)

    return walk(json.loads(text))
