// ⚙️ LCon — 配置文件定义
// 📄 所有可配置项：端口、token、权限等级、消息文本和 emoji
// 🧠 使用 ForgeConfigSpec 自动生成 .toml 配置文件
//     首次运行时自动创建，写入默认值
// 📂 文件位置：.minecraft/config/lcon-ws-server.toml
// 📋 配置分类：
//     - main：模组开关、端口、token、权限、序列化模式
//     - messages.auth：token 校验相关消息
//     - messages.welcome：连接欢迎消息
//     - messages.prefixes：各前缀帮助说明
//     - messages.status：就绪 / 错误状态消息
//     - messages.log：控制台日志消息

package com.cwelth.lcon;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraftforge.common.ForgeConfigSpec;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Config {
    public static final String CATEGORY_MAIN = "main";

    // ⚙️ 基本功能配置
    public static ForgeConfigSpec.BooleanValue ENABLE_MOD;          // 模组总开关
    public static ForgeConfigSpec.IntValue PORT;                    // WS 监听端口
    public static ForgeConfigSpec.ConfigValue<String> TOKEN;        // 认证令牌
    public static ForgeConfigSpec.IntValue COMMAND_PERMISSION_LEVEL; // [server] 指令 OP 等级
    public static ForgeConfigSpec.ConfigValue<String> SERIALIZER_MODE; // 消息序列化模式

    // 📢 消息配置（emoji + 文本分离，可分别自定义）
    public static ForgeConfigSpec.BooleanValue ENABLE_MESSAGE_EMOJI; // emoji 总开关

    // 🔒 token 校验
    public static ForgeConfigSpec.ConfigValue<String> EMOJI_UNAUTHORIZED;
    public static ForgeConfigSpec.ConfigValue<String> MSG_UNAUTHORIZED;

    // 🎉 欢迎消息
    public static ForgeConfigSpec.ConfigValue<String> EMOJI_WELCOME;
    public static ForgeConfigSpec.ConfigValue<String> MSG_WELCOME;

    // 📋 前缀列表标题
    public static ForgeConfigSpec.ConfigValue<String> EMOJI_PREFIXES;
    public static ForgeConfigSpec.ConfigValue<String> MSG_PREFIXES;

    // 💬 各前缀的说明
    public static ForgeConfigSpec.ConfigValue<String> EMOJI_CHAT;
    public static ForgeConfigSpec.ConfigValue<String> MSG_CHAT;
    public static ForgeConfigSpec.ConfigValue<String> EMOJI_MESSAGE;
    public static ForgeConfigSpec.ConfigValue<String> MSG_MESSAGE;
    public static ForgeConfigSpec.ConfigValue<String> EMOJI_SYSTEM;
    public static ForgeConfigSpec.ConfigValue<String> MSG_SYSTEM;
    public static ForgeConfigSpec.ConfigValue<String> EMOJI_CLIENT;
    public static ForgeConfigSpec.ConfigValue<String> MSG_CLIENT;
    public static ForgeConfigSpec.ConfigValue<String> EMOJI_SERVER;
    public static ForgeConfigSpec.ConfigValue<String> MSG_SERVER;

    // ✅ 就绪 / ❌ 错误消息
    public static ForgeConfigSpec.ConfigValue<String> EMOJI_READY;
    public static ForgeConfigSpec.ConfigValue<String> MSG_READY;
    public static ForgeConfigSpec.ConfigValue<String> EMOJI_ERROR_PREFIX;
    public static ForgeConfigSpec.ConfigValue<String> MSG_ERROR_PREFIX;
    public static ForgeConfigSpec.ConfigValue<String> EMOJI_ERROR_INTERNAL;
    public static ForgeConfigSpec.ConfigValue<String> MSG_ERROR_INTERNAL;

    // 🪵 控制台日志
    public static ForgeConfigSpec.ConfigValue<String> EMOJI_LOG_START;
    public static ForgeConfigSpec.ConfigValue<String> MSG_LOG_START;
    public static ForgeConfigSpec.ConfigValue<String> EMOJI_LOG_ERROR;
    public static ForgeConfigSpec.ConfigValue<String> MSG_LOG_ERROR;

    // ================ 🌐 Mclistener 协议配置 ================
    // 🧠 新端口 60626，与旧前缀协议端口 58115 完全隔离
    //    参考：MCDR / CS2 / LeviLamina 同类插件的配置风格

    public static ForgeConfigSpec.BooleanValue ENABLE_MCLISTENER;            // 🌐 总开关
    public static ForgeConfigSpec.ConfigValue<String> MCLISTENER_HOST;       // 🖧 监听地址
    public static ForgeConfigSpec.IntValue MCLISTENER_PORT;                  // 🔌 监听端口
    public static ForgeConfigSpec.ConfigValue<String> MCLISTENER_TOKEN;      // 🔑 认证令牌
    public static ForgeConfigSpec.BooleanValue ENABLE_PLAYER_JOIN_BROADCAST;  // 📢 玩家加入广播
    public static ForgeConfigSpec.BooleanValue ENABLE_PLAYER_LEAVE_BROADCAST; // 📢 玩家离开广播
    public static ForgeConfigSpec.ConfigValue<String> PLAYER_LIFECYCLE_SOURCE; // 🚪 玩家上下线事件来源
    public static ForgeConfigSpec.BooleanValue BROADCAST_PLAYER_LEAVE_ON_SERVER_STOP; // 🛑 服务器关闭时逐个广播离开
    public static ForgeConfigSpec.BooleanValue ENABLE_PLAYER_CHAT_BROADCAST;  // 💬 聊天广播
    public static ForgeConfigSpec.ConfigValue<String> PLAYER_CHAT_CAPTURE_MODE; // 💬 聊天捕获模式：event | text | both
    public static ForgeConfigSpec.BooleanValue ENABLE_RECEIVE_GROUP_MESSAGE;  // 📩 接收群消息
    public static ForgeConfigSpec.ConfigValue<String> GROUP_MESSAGE_FORMAT;  // ✏️ 群消息游戏内显示格式
    public static ForgeConfigSpec.ConfigValue<String> EXEC_COMMAND_MODE;     // 🎮 远程指令模式：disabled | client
    public static ForgeConfigSpec.ConfigValue<String> COMMAND_TRACKING_MODE; // 🎯 指令追踪模式：single | parallel（parallel 为实验性模式，可能不稳定或串线）

    private static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();
    public static ForgeConfigSpec CLIENT_CONFIG;

    static {
        // ⚙️ 基本功能配置块
        CLIENT_BUILDER.comment("Main config").push(CATEGORY_MAIN);
        ENABLE_MOD = CLIENT_BUILDER.comment("Is the mod available at all?").define("enable_mod", true);
        PORT = CLIENT_BUILDER.comment("Port to listen on. Default 58115.").defineInRange("port", 58115, 1024, 65535);
        TOKEN = CLIENT_BUILDER.comment("Authentication token. Clients must pass ?token=xxx when connecting.").define("token", "your_secret_token");
        COMMAND_PERMISSION_LEVEL = CLIENT_BUILDER.comment("OP level for [server] commands (0-4). Default 4 = full access without enabling cheats.").defineInRange("command_permission_level", 4, 0, 4);
        SERIALIZER_MODE = CLIENT_BUILDER.comment("Component serialization mode. 'json' for Minecraft JSON format (recommended for Python TUI), 'tostring' for ComponentContents.toString() format.").define("serializer_mode", "json");
        CLIENT_BUILDER.pop();

        // 📢 消息配置块
        CLIENT_BUILDER.comment("Message configuration. Set emoji to empty to disable emoji per-message.").push("messages");
        ENABLE_MESSAGE_EMOJI = CLIENT_BUILDER.comment("Master switch for message emoji. If false, no emoji are prepended regardless of per-message emoji settings.").define("enable_message_emoji", true);

        // 🔒 Token 校验消息
        CLIENT_BUILDER.comment("Token / Auth messages").push("auth");
        EMOJI_UNAUTHORIZED = CLIENT_BUILDER.define("emoji_unauthorized", "🔒");
        MSG_UNAUTHORIZED = CLIENT_BUILDER.define("msg_unauthorized", "401:Unauthorized - invalid token.");
        CLIENT_BUILDER.pop();

        // 🎉 连接欢迎消息
        CLIENT_BUILDER.comment("Welcome messages on connect").push("welcome");
        EMOJI_WELCOME = CLIENT_BUILDER.define("emoji_welcome", "🎉");
        MSG_WELCOME = CLIENT_BUILDER.define("msg_welcome", "200:Welcome to LCon! Have fun! Don't forget to use prefixes with every message you send to me.");
        EMOJI_PREFIXES = CLIENT_BUILDER.define("emoji_prefixes", "📋");
        MSG_PREFIXES = CLIENT_BUILDER.define("msg_prefixes", "200:Valid prefixes:");
        CLIENT_BUILDER.pop();

        // 💬 各前缀帮助说明
        CLIENT_BUILDER.comment("Prefix help lines").push("prefixes");
        EMOJI_CHAT = CLIENT_BUILDER.define("emoji_chat", "💬");
        MSG_CHAT = CLIENT_BUILDER.define("msg_chat", "200:[chat] - send message to Minecraft chat.");
        EMOJI_MESSAGE = CLIENT_BUILDER.define("emoji_message", "📩");
        MSG_MESSAGE = CLIENT_BUILDER.define("msg_message", "200:[message] - display message for player only.");
        EMOJI_SYSTEM = CLIENT_BUILDER.define("emoji_system", "🔔");
        MSG_SYSTEM = CLIENT_BUILDER.define("msg_system", "200:[system] - display system message in chat (for player only).");
        EMOJI_CLIENT = CLIENT_BUILDER.define("emoji_client", "🖥️");
        MSG_CLIENT = CLIENT_BUILDER.define("msg_client", "200:[client] - execute client-side command.");
        EMOJI_SERVER = CLIENT_BUILDER.define("emoji_server", "🖧");
        MSG_SERVER = CLIENT_BUILDER.define("msg_server", "200:[server] - execute server-side command.");
        CLIENT_BUILDER.pop();

        // ✅ / ❌ 状态消息
        CLIENT_BUILDER.comment("Ready and error messages").push("status");
        EMOJI_READY = CLIENT_BUILDER.define("emoji_ready", "✅");
        MSG_READY = CLIENT_BUILDER.define("msg_ready", "201:ready.");
        EMOJI_ERROR_PREFIX = CLIENT_BUILDER.define("emoji_error_prefix", "❌");
        MSG_ERROR_PREFIX = CLIENT_BUILDER.define("msg_error_prefix", "400:Error! Send message prefix first! [chat], [message], [system], [client], [server] are valid prefixes.");
        EMOJI_ERROR_INTERNAL = CLIENT_BUILDER.define("emoji_error_internal", "💥");
        MSG_ERROR_INTERNAL = CLIENT_BUILDER.define("msg_error_internal", "500:Internal server error - ");
        CLIENT_BUILDER.pop();

        // 🪵 日志消息
        CLIENT_BUILDER.comment("Log messages").push("log");
        EMOJI_LOG_START = CLIENT_BUILDER.define("emoji_log_start", "🚀");
        MSG_LOG_START = CLIENT_BUILDER.define("msg_log_start", "LCon WebSocket server listening on port ");
        EMOJI_LOG_ERROR = CLIENT_BUILDER.define("emoji_log_error", "🔥");
        MSG_LOG_ERROR = CLIENT_BUILDER.define("msg_log_error", "LCon WebSocket error: ");
        CLIENT_BUILDER.pop();

        CLIENT_BUILDER.pop(); // messages

        // 🌐 Mclistener 协议配置
        CLIENT_BUILDER.comment("Mclistener protocol settings (port 60626, JSON protocol for Koishi client)").push("mclistener");

        ENABLE_MCLISTENER = CLIENT_BUILDER
            .comment("🌐 Enable the mclistener protocol WebSocket server (独立端口，与旧前缀协议互不干扰)")
            .define("enable", true);

        MCLISTENER_HOST = CLIENT_BUILDER
            .comment("🖧 Mclistener WS server listen address (0.0.0.0 = all interfaces)")
            .define("host", "0.0.0.0");

        MCLISTENER_PORT = CLIENT_BUILDER
            .comment("🔌 Mclistener WS server port (default 60626)")
            .defineInRange("port", 60626, 1024, 65535);

        MCLISTENER_TOKEN = CLIENT_BUILDER
            .comment("🔑 Auth token for mclistener connections. Empty = no auth. Clients pass via ?token=xxx")
            .define("token", "");

        ENABLE_PLAYER_JOIN_BROADCAST = CLIENT_BUILDER
            .comment("📢 Broadcast player join events via mclistener ({\\\"type\\\":\\\"player_join\\\", ...})")
            .define("enable_player_join_broadcast", true);

        ENABLE_PLAYER_LEAVE_BROADCAST = CLIENT_BUILDER
            .comment("📢 Broadcast player leave events via mclistener ({\\\"type\\\":\\\"player_leave\\\", ...})")
            .define("enable_player_leave_broadcast", true);

        PLAYER_LIFECYCLE_SOURCE = CLIENT_BUILDER
            .comment(
                "🚪 Player lifecycle source. Changes take effect after leaving and re-entering the world.",
                "server_event = use integrated server PlayerLoggedInEvent / PlayerLoggedOutEvent (recommended)",
                "client_entity_tracking = use client entity load/unload events (legacy compatibility; may report false join/leave events)",
                "A future version may add a tablist source for clients connected to external servers"
            )
            .define("player_lifecycle_source", "server_event");

        BROADCAST_PLAYER_LEAVE_ON_SERVER_STOP = CLIENT_BUILDER
            .comment("🛑 Broadcast player_leave for every tracked online player when the integrated server stops")
            .define("broadcast_player_leave_on_server_stop", false);

        ENABLE_PLAYER_CHAT_BROADCAST = CLIENT_BUILDER
            .comment("💬 Broadcast player chat messages via mclistener ({\\\"type\\\":\\\"player_chat\\\", ...})")
            .define("enable_player_chat_broadcast", true);

        PLAYER_CHAT_CAPTURE_MODE = CLIENT_BUILDER
            .comment("💬 Player chat capture mode: event = use ClientChatReceivedEvent.Player only (recommended), text = parse '<PlayerName> message' from rendered text only, both = prefer event and fallback to text parsing")
            .define("player_chat_capture_mode", "event");

        ENABLE_RECEIVE_GROUP_MESSAGE = CLIENT_BUILDER
            .comment("📩 Receive chat_platform_to_server messages and relay to in-game chat")
            .define("enable_receive_group_message", true);

        GROUP_MESSAGE_FORMAT = CLIENT_BUILDER
            .comment("✏️ In-game display format for group messages. Placeholders: {group_id} {group_name} {nickname} {message}")
            .define("group_message_format", "§6§l[{group_name}]§r §7({group_id})§r §a§o{nickname}§r§f: {message}");

        EXEC_COMMAND_MODE = CLIENT_BUILDER
            .comment("🎮 Remote command execution mode: disabled = off, client = execute via player.connection.sendCommand()")
            .define("exec_command_mode", "disabled");

        COMMAND_TRACKING_MODE = CLIENT_BUILDER
            .comment("🎯 Command tracking mode: single = allow only one pending execute_command at a time (recommended), parallel = experimental mode that allows multiple pending commands with best-effort chat-based tracking only and may be unstable / produce mixed results")
            .define("command_tracking_mode", "single");

        CLIENT_BUILDER.pop(); // mclistener

        CLIENT_CONFIG = CLIENT_BUILDER.build();
    }

    // 📂 从磁盘加载 / 刷新配置文件
    public static void loadConfig(ForgeConfigSpec spec, Path path) {

        final CommentedFileConfig configData = CommentedFileConfig.builder(path)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .preserveInsertionOrder()
                .build();

        configData.load();
        spec.setConfig(configData);
    }
}
