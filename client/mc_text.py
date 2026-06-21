import re


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
    """Extract human-readable text from Minecraft text component serialization."""
    if not text or text == '"empty"':
        return text
    text = text.strip().strip('"')

    result = _parse_component(text)
    result = result.replace("\\u003d", "=")
    result = result.replace("\\u0027", "'")
    result = result.replace("\\u003c", "<")
    result = result.replace("\\u003e", ">")
    result = result.replace("\\/", "/")
    return result.strip()


def _parse_component(text: str) -> str:
    """Recursively parse a single component or component list."""
    if not text:
        return ""

    if text.startswith("translation{"):
        return _parse_translation(text)
    if text.startswith("literal{"):
        return _parse_literal(text)
    if text.startswith("keybind{"):
        return _parse_keybind(text)
    if text.startswith("score{"):
        return _parse_score(text)
    if text.startswith("selector{"):
        return _parse_selector(text)
    if text.startswith("nbt{"):
        return _parse_nbt(text)
    if text.startswith("["):
        return _parse_list(text)
    return text


def _extract_braced(text: str, start: int):
    """Find matching closing brace from position start (after opening brace)."""
    depth = 1
    i = start
    in_single = False
    in_double = False
    while i < len(text) and depth > 0:
        ch = text[i]
        if ch == "'" and not in_double:
            in_single = not in_single
        elif ch == '"' and not in_single:
            in_double = not in_double
        if not in_single and not in_double:
            if ch == "{":
                depth += 1
            elif ch == "}":
                depth -= 1
        i += 1
    return text[start : i - 1] if depth == 0 else text[start:]


def _parse_translation(text: str) -> str:
    """Parse translation{key='xxx', args=[...]}"""
    m = re.match(r"translation\{key='([^']*)'(?:,\s*args=\[(.*)\])?\}", text, re.DOTALL)
    if not m:
        return text
    key = m.group(1)
    args_str = m.group(2)
    args = _parse_args_list(args_str) if args_str else []

    if key in TRANSLATION_EN:
        template = TRANSLATION_EN[key]
        rendered = []
        arg_idx = 1
        for arg in args:
            placeholder = "{" + str(arg_idx) + "}"
            if placeholder in template:
                template = template.replace(placeholder, arg, 1)
            else:
                rendered.append(arg)
            arg_idx += 1
        result = template
        if rendered:
            result += " " + " ".join(rendered)
        return result
    return text


def _parse_literal(text: str) -> str:
    """Parse literal{text}{style={...}}"""
    m = re.match(r"literal\{([^}]*)\}", text)
    if m:
        return m.group(1)
    return text


def _parse_keybind(text: str) -> str:
    m = re.match(r"keybind\{key='([^']*)'\}", text)
    if m:
        key = m.group(1)
        key_names = {
            "key.forward": "W",
            "key.left": "A",
            "key.back": "S",
            "key.right": "D",
            "key.jump": "Space",
            "key.sneak": "Shift",
            "key.sprint": "Ctrl",
            "key.inventory": "E",
            "key.use": "Right Click",
            "key.attack": "Left Click",
            "key.pickItem": "Middle Click",
            "key.chat": "T",
            "key.playerlist": "Tab",
            "key.command": "/",
            "key.screenshot": "F2",
            "key.fullscreen": "F11",
        }
        return key_names.get(key, f"[{key}]")
    return text


def _parse_score(text: str) -> str:
    return text


def _parse_selector(text: str) -> str:
    return text


def _parse_nbt(text: str) -> str:
    return text


def _parse_list(text: str) -> str:
    """Parse a list of components: [{...}, {...}]"""
    parts = []
    lst = text.strip()
    if lst.startswith("[") and lst.endswith("]"):
        lst = lst[1:-1]
        depth = 0
        current = ""
        in_single = False
        in_double = False
        for ch in lst:
            if ch == "'" and not in_double:
                in_single = not in_single
            elif ch == '"' and not in_single:
                in_double = not in_double
            if not in_single and not in_double:
                if ch == "{":
                    depth += 1
                elif ch == "}":
                    depth -= 1
                elif ch == "," and depth == 0:
                    item_parsed = _parse_component(current.strip())
                    if item_parsed:
                        parts.append(item_parsed)
                    current = ""
                    continue
            current += ch
        if current.strip():
            item_parsed = _parse_component(current.strip())
            if item_parsed:
                parts.append(item_parsed)
    return " ".join(parts)


def _parse_args_list(text: str) -> list[str]:
    """Parse args list from translation component."""
    args = []
    text = text.strip()
    depth = 0
    current = ""
    in_single = False
    in_double = False
    for ch in text:
        if ch == "'" and not in_double:
            in_single = not in_single
        elif ch == '"' and not in_single:
            in_double = not in_double
        if not in_single and not in_double:
            if ch == "{":
                depth += 1
            elif ch == "}":
                depth -= 1
            elif ch == "," and depth == 0:
                arg = _parse_component(current.strip())
                if arg:
                    args.append(arg)
                current = ""
                continue
        current += ch
    if current.strip():
        arg = _parse_component(current.strip())
        if arg:
            args.append(arg)
    return args
