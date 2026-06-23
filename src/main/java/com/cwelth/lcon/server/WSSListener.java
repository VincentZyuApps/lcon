// 🖧 LCon — WebSocket 服务端
// 📄 基于 Java-WebSocket 库，在客户端启动一个 WS 服务端
// 🧠 外部工具以 WebSocket 客户端连入，发送带前缀的文本指令，
//    服务端解析前缀并执行对应的游戏操作
// 🔌 使用反射调用包私有构造器（见 RconManager）
// 📋 消息处理流程：
//     1. onOpen → token 校验 → 发送欢迎 + 前缀帮助
//     2. onMessage → 解析前缀 → 执行对应操作
//     3. onClose → 清理资源
//     4. onError → 记录日志 + 通知客户端
// 📦 基于 org.java_websocket.server.WebSocketServer
//    （已 shade 到 lcon.org.java_websocket 命名空间避免冲突）

package com.cwelth.lcon.server;

/*
import com.netiq.websocket.WebSocket;
import com.netiq.websocket.WebSocketServer;

 */
import com.cwelth.lcon.Config;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.ForgeConfigSpec;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;

import java.net.InetSocketAddress;

public class WSSListener extends WebSocketServer {
    // 🪵 SLF4J 日志记录器（通过 Minecraft 的 LogUtils 获取）
    private static final Logger LOGGER = LogUtils.getLogger();

    // 👤 当前本地玩家引用，由 updatePlayer() 在主线程更新
    // ⚠️ 每次 tick 都会更新，确保引用不过期
    private LocalPlayer player;

    // 🏗️ 构造函数 — 绑定端口，保存玩家引用
    public WSSListener(int port, LocalPlayer player)
    {
        super(new InetSocketAddress(port));
        this.player = player;
    }

    // 🔄 更新玩家引用（每次 tick 调用，确保 player 不过期）
    // 🧠 Minecraft 的 LocalPlayer 对象会因世界重载等原因变化，
    //    需要定期更新引用以避免使用过期对象访问游戏
    public void updatePlayer(LocalPlayer player)
    {
        this.player = player;
    }

    @Override
    // 🔓 新 WS 客户端连接时调用
    // 1）校验 token（如果有配置）
    // 2）发送欢迎消息 + 前缀帮助列表
    // 📋 发送的消息序列：
    //     - 欢迎 → 前缀标题 → chat → message → system → client → server → 就绪
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        String configuredToken = Config.TOKEN.get();

        // 🔑 如果配置了 token，校验客户端传来的 ?token=xxx 参数
        if (!configuredToken.isEmpty()) {
            String token = extractQueryParam(clientHandshake.getResourceDescriptor(), "token");
            if (!configuredToken.equals(token)) {
                // ❌ token 不匹配 → 发送错误消息后关闭连接
                webSocket.send(fmt(Config.EMOJI_UNAUTHORIZED, Config.MSG_UNAUTHORIZED));
                webSocket.close();
                return;
            }
        }

        // 🎉 发送欢迎消息序列
        webSocket.send(fmt(Config.EMOJI_WELCOME, Config.MSG_WELCOME));
        webSocket.send(fmt(Config.EMOJI_PREFIXES, Config.MSG_PREFIXES));
        webSocket.send(fmt(Config.EMOJI_CHAT, Config.MSG_CHAT));
        webSocket.send(fmt(Config.EMOJI_MESSAGE, Config.MSG_MESSAGE));
        webSocket.send(fmt(Config.EMOJI_SYSTEM, Config.MSG_SYSTEM));
        webSocket.send(fmt(Config.EMOJI_CLIENT, Config.MSG_CLIENT));
        webSocket.send(fmt(Config.EMOJI_SERVER, Config.MSG_SERVER));
        webSocket.send(fmt(Config.EMOJI_READY, Config.MSG_READY));
    }

    // 🔍 解析 URI 查询参数（如 ?token=xxx → "xxx"）
    // 📝 URI 示例：ws://localhost:58115?token=my_secret
    private static String extractQueryParam(String uri, String param) {
        if (uri == null || !uri.contains("?")) return "";
        String query = uri.substring(uri.indexOf("?") + 1);
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(param)) return kv[1];
        }
        return "";
    }

    @Override
    // 🔌 客户端断开连接时调用（暂无特殊处理）
    // 📌 Java-WebSocket 库要求实现此方法，当前保持空实现
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {

    }

    @Override
    // 📩 收到 WS 客户端消息时调用
    // 🧠 根据前缀分发到不同的处理方法：
    //     - [chat]     → player.connection.sendChat()
    //     - [message]  → player.displayClientMessage()
    //     - [system]   → player.sendSystemMessage()
    //     - [client]   → ClientCommandHandler.runCommand()
    //     - [server]   → server.getCommands().performPrefixedCommand()
    //     - 其他前缀   → 返回错误提示
    public void onMessage(WebSocket webSocket, String s) {
        String clearMessage = "";

        // 💬 [chat] — 以玩家身份发送聊天消息
        // 🎯 模拟玩家在聊天栏输入消息
        if(s.startsWith("[chat]"))
        {
            clearMessage = s.substring(6);
            player.connection.sendChat(clearMessage);
            return;
        }

        // 📩 [message] — 仅向玩家显示消息（客户端侧，其他人看不到）
        // 🎯 用于发送通知或提示，不出现在服务器聊天广播中
        if(s.startsWith("[message]"))
        {
            clearMessage = s.substring(9);
            player.displayClientMessage(Component.literal(clearMessage), true);
            return;
        }

        // 🔔 [system] — 在聊天栏显示系统消息
        // 🎯 类似 /say 或系统广播，显示为灰色系统提示
        if(s.startsWith("[system]"))
        {
            clearMessage = s.substring(8);
            player.sendSystemMessage(Component.literal(clearMessage));
            return;
        }

        // 🖥️ [client] — 执行客户端侧命令（如 /fps、/clear）
        // 🎯 由 ClientCommandHandler 处理，不会发送到服务端
        if(s.startsWith("[client]"))
        {
            clearMessage = s.substring(8);
            if(clearMessage.startsWith("/")) clearMessage = clearMessage.substring(1);
            ClientCommandHandler.runCommand(clearMessage);
            return;
        }

        // 🖧 [server] — 执行服务端命令（绕过玩家权限，使用配置的 OP 等级）
        // 🎯 通过 createCommandSourceStack().withPermission() 提升权限
        // ⚠️ 即使服务器没有开启作弊，也能执行命令
        if(s.startsWith("[server]"))
        {
            clearMessage = s.substring(8);
            if(clearMessage.startsWith("/")) clearMessage = clearMessage.substring(1);
            MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
            if (server != null) {
                ServerPlayer serverPlayer = server.getPlayerList().getPlayer(player.getUUID());
                if (serverPlayer != null) {
                    int level = Config.COMMAND_PERMISSION_LEVEL.get();
                    server.getCommands().performPrefixedCommand(
                        serverPlayer.createCommandSourceStack().withPermission(level),
                        clearMessage
                    );
                }
            }
            return;
        }

        // ❌ 未知前缀 — 发送错误提示，列出所有合法前缀
        webSocket.send(fmt(Config.EMOJI_ERROR_PREFIX, Config.MSG_ERROR_PREFIX));
    }

    @Override
    // 💥 发生错误时调用 — 记录日志并通知客户端
    // 🧠 可能的原因：端口被占用、消息解析失败等
    public void onError(WebSocket webSocket, Exception e) {
        LOGGER.error(fmt(Config.EMOJI_LOG_ERROR, Config.MSG_LOG_ERROR) + e.getMessage());
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.send(fmt(Config.EMOJI_ERROR_INTERNAL, Config.MSG_ERROR_INTERNAL) + e.getMessage());
        }
    }

    @Override
    // 🚀 服务端启动成功时调用
    // 🧠 在端口绑定完成后触发，打印监听端口号
    public void onStart() {
        LOGGER.info(fmt(Config.EMOJI_LOG_START, Config.MSG_LOG_START) + this.getPort());
    }

    // 🛠️ 工具方法：根据 emoji 总开关决定是否拼上 emoji
    // 📝 如果 enable_message_emoji = true，返回 "📋 200:Valid prefixes:"
    //    如果 enable_message_emoji = false，返回 "200:Valid prefixes:"
    private static String fmt(ForgeConfigSpec.ConfigValue<String> emojiCfg,
                               ForgeConfigSpec.ConfigValue<String> msgCfg) {
        return Config.ENABLE_MESSAGE_EMOJI.get()
            ? emojiCfg.get() + " " + msgCfg.get()
            : msgCfg.get();
    }
}
