// 🖧 LCon — Mclistener WebSocket 服务端
// 📄 基于 Java-WebSocket 库，独立端口，只处理 mclistener JSON 协议
// 🧠 与 WSSListener 完全隔离，两个 WS 服务端互不干扰：
//   - WSSListener (58115) → 旧前缀协议，用于 Python TUI 客户端
//   - MclistenerWSS (60626) → 新 JSON 协议，用于 Koishi 客户端
// 📋 消息处理流程：
//     1. onOpen → token 校验 → 发送 ready
//     2. onMessage → 解析 JSON → 按 type 分发
//        - chat_platform_to_server → 显示到游戏内聊天栏
//        - execute_command / external_command_to_server → 执行 + 追踪输出
//     3. onClose → 清理该客户端的指令追踪
//     4. onError → 记录日志 + 通知客户端
//     5. onStart → 记录启动端口

package com.cwelth.lcon.mclistener;

import com.cwelth.lcon.Config;
import com.cwelth.lcon.LCon;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;

import java.net.InetSocketAddress;

public class MclistenerWSS extends WebSocketServer {
    // 🪵 SLF4J 日志记录器
    private static final Logger LOGGER = LogUtils.getLogger();

    // 🏗️ 构造函数 — 绑定端口
    public MclistenerWSS(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    // 🔓 新 WS 客户端连接时调用
    // 1）校验 token（如果有配置）
    // 2）发送 ready 消息
    public void onOpen(WebSocket ws, ClientHandshake handshake) {
        String configuredToken = Config.MCLISTENER_TOKEN.get();
        if (!configuredToken.isEmpty()) {
            String token = extractQueryParam(handshake.getResourceDescriptor(), "token");
            if (!configuredToken.equals(token)) {
                ws.send("{\"type\":\"error\",\"message\":\"401:Unauthorized - invalid token\"}");
                ws.close();
                return;
            }
        }
        // ✅ 发送就绪消息
        ws.send("{\"type\":\"ready\",\"message\":\"201:mclistener ready\"}");
        LOGGER.info("🔗 [MclistenerWSS] 客户端已连接: {}", ws.getRemoteSocketAddress());
    }

    @Override
    // 📩 收到 WS 客户端消息时调用
    // 🧠 解析 JSON → 按 type 分发到不同的处理方法
    public void onMessage(WebSocket ws, String text) {
        try {
            JsonObject json = JsonParser.parseString(text).getAsJsonObject();
            String type = json.get("type").getAsString();

            switch (type) {
                // 📩 聊天平台 → 游戏内
                case "chat_platform_to_server" -> handleChatPlatform(ws, json);

                // 🎮 远程指令执行（兼容两种 type 名）
                case "execute_command", "external_command_to_server" -> handleExecuteCommand(ws, json);

                // 🔑 后连接鉴权消息（忽略即可，已在 URL 参数完成鉴权）
                case "auth" -> {}

                // 🤷 未知类型
                default -> LOGGER.warn("🤷 [MclistenerWSS] 未知消息类型: {}", type);
            }
        } catch (Exception e) {
            LOGGER.error("💥 [MclistenerWSS] JSON 解析失败: {} | 原始消息: {}", e.getMessage(), text);
        }
    }

    // 📩 处理聊天平台 → 游戏内的消息转发
    private void handleChatPlatform(WebSocket ws, JsonObject json) {
        if (!Config.ENABLE_RECEIVE_GROUP_MESSAGE.get()) return;

        // 提取字段
        String groupId   = getJsonString(json, "group_id");
        String groupName = getJsonString(json, "group_name");
        String nickname  = getJsonString(json, "nickname");
        String message   = getJsonString(json, "message");

        // 按配置的格式渲染
        String formatted = Config.GROUP_MESSAGE_FORMAT.get()
            .replace("{group_id}",   groupId)
            .replace("{group_name}", groupName)
            .replace("{nickname}",   nickname)
            .replace("{message}",    message);

        // 在主线程显示到游戏聊天栏
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(Component.literal(formatted));
            }
        });

        LOGGER.info("📩 [群→服] [{}] {}: {}", groupName, nickname, message);
    }

    // 🎮 处理远程指令执行请求
    private void handleExecuteCommand(WebSocket ws, JsonObject json) {
        String mode = Config.EXEC_COMMAND_MODE.get();
        if ("disabled".equals(mode)) {
            LOGGER.warn("🎮 [MclistenerWSS] 远程指令执行已禁用（exec_command_mode = disabled）");
            sendCommandResult(ws, getJsonString(json, "request_id"), getJsonString(json, "command"), false,
                "", "403:remote command execution is disabled");
            return;
        }

        String reqId   = getJsonString(json, "request_id");
        String command = getJsonString(json, "command");
        if (command.startsWith("/")) command = command.substring(1);

        if (reqId.isBlank() || command.isBlank()) {
            sendCommandResult(ws, reqId, command, false, "", "400:request_id and command are required");
            return;
        }

        // 🎮 在主线程执行指令
        String cmdToExec = command;
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.connection != null) {
                if (LCon.commandTracker == null) {
                    sendCommandResult(ws, reqId, cmdToExec, false, "", "500:command tracker is unavailable");
                    return;
                }

                CommandTracker.TrackStartResult trackResult = LCon.commandTracker.track(ws, reqId, cmdToExec);
                if (trackResult.status() != CommandTracker.TrackStartStatus.STARTED) {
                    sendCommandResult(ws, reqId, cmdToExec, false, "", trackResult.message());
                    return;
                }

                Minecraft.getInstance().player.connection.sendCommand(cmdToExec);
                LOGGER.info("🎮 [MclistenerWSS] 已执行指令: /{} (request_id={})", cmdToExec, reqId);
            } else {
                LOGGER.warn("🎮 [MclistenerWSS] 玩家连接不可用，无法执行指令: /{}", cmdToExec);
                sendCommandResult(ws, reqId, cmdToExec, false, "", "503:player connection unavailable");
            }
        });
    }

    @Override
    // 🔌 客户端断开连接时调用 — 清理该客户端的指令追踪
    public void onClose(WebSocket ws, int code, String reason, boolean remote) {
        LOGGER.info("🔌 [MclistenerWSS] 客户端断开: {} (code={}, reason={})",
            ws.getRemoteSocketAddress(), code, reason);
        // 清理该客户端的所有待追踪指令
        if (LCon.commandTracker != null) {
            LCon.commandTracker.cancelAll(ws);
        }
    }

    @Override
    // 💥 发生错误时调用
    public void onError(WebSocket ws, Exception ex) {
        LOGGER.error("🔥 [MclistenerWSS] 错误: {}", ex.getMessage());
        if (ws != null && ws.isOpen()) {
            JsonObject err = new JsonObject();
            err.addProperty("type", "error");
            err.addProperty("message", "500:Internal error - " + ex.getMessage());
            ws.send(err.toString());
        }
    }

    @Override
    // 🚀 服务端启动成功时调用
    public void onStart() {
        LOGGER.info("🚀 [MclistenerWSS] WebSocket 服务端已启动，端口: {}", this.getPort());
    }

    // 📡 广播 JSON 消息到所有客户端
    public void broadcastJson(String json) {
        broadcast(json);
    }

    // 📡 发送 JSON 消息给指定客户端
    public void sendJson(WebSocket ws, String json) {
        if (ws != null && ws.isOpen()) {
            ws.send(json);
        }
    }

    private void sendCommandResult(WebSocket ws, String requestId, String command, boolean ok, String output, String error) {
        if (ws == null || !ws.isOpen()) return;

        JsonObject json = new JsonObject();
        json.addProperty("type", "command_result");
        json.addProperty("request_id", requestId == null ? "" : requestId);
        json.addProperty("command", command == null ? "" : command);
        json.addProperty("ok", ok);
        json.addProperty("output", output == null ? "" : output);
        if (error != null && !error.isBlank()) {
            json.addProperty("error", error);
        }
        ws.send(json.toString());
    }

    // 🛠️ 安全获取 JSON 字符串字段，缺失时返回空字符串
    private static String getJsonString(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsString() : "";
    }

    // 🔍 解析 URI 查询参数（?token=xxx）
    private static String extractQueryParam(String uri, String param) {
        if (uri == null || !uri.contains("?")) return "";
        String query = uri.substring(uri.indexOf("?") + 1);
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(param)) return kv[1];
        }
        return "";
    }
}
