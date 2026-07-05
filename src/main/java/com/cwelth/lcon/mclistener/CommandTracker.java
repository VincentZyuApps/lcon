// 🎯 LCon — Mclistener 指令输出追踪器
// 📄 当 mclistener 客户端发送 execute_command 后，指令输出通过
//    ClientChatReceivedEvent 以聊天消息形式到达，本追踪器负责：
//     1. 收集一段时间内的聊天消息作为指令输出
//     2. 超时后（500ms 无新消息）判定指令完成
//     3. 组装 command_result 发回给对应的 WS 客户端
// 🧠 线程安全说明：
//   - track()    被 WS 线程调用
//   - onChatMessage() 被 Minecraft 主线程调用
//   - tick()     被 Minecraft 主线程调用
//   - entries 使用 ConcurrentHashMap 保证安全

package com.cwelth.lcon.mclistener;

import com.cwelth.lcon.Config;
import com.cwelth.lcon.LCon;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CommandTracker {
    // 🪵 日志记录器
    private static final Logger LOGGER = LogUtils.getLogger();

    // ⏱️ 消抖等待时间：最后一次输出后等待多久判定指令完成
    private static final long DEBOUNCE_MS = 500;
    // ⏱️ 硬超时时间：指令执行后最多等多久（防止无输出指令永远挂起）
    private static final long HARD_TIMEOUT_MS = 15000;

    // 📋 正在追踪的指令条目 <request_id, TrackEntry>
    private final Map<String, TrackEntry> entries = new ConcurrentHashMap<>();

    // 🏗️ 追踪条目内部类
    private static class TrackEntry {
        final WebSocket ws;             // 发送指令的 WS 客户端
        final String requestId;         // 请求唯一 ID
        final String command;           // 执行的指令
        final List<String> outputs;     // 收集到的输出行
        final long createdAt;           // 条目创建时间戳
        long lastOutputMs;              // 最后一条输出到达时间戳
        boolean done;                   // 是否已完成

        TrackEntry(WebSocket ws, String requestId, String command) {
            this.ws = ws;
            this.requestId = requestId;
            this.command = command;
            this.outputs = new ArrayList<>();
            this.createdAt = System.currentTimeMillis();
            this.lastOutputMs = System.currentTimeMillis();
            this.done = false;
        }
    }

    public enum TrackStartStatus {
        STARTED,
        REJECTED_BUSY
    }

    public record TrackStartResult(TrackStartStatus status, String message) {}

    // 🎯 开始追踪一条指令
    // 📝 在 onMessage 中收到 execute_command 时调用
    public TrackStartResult track(WebSocket ws, String requestId, String command) {
        String trackingMode = Config.COMMAND_TRACKING_MODE.get();
        if ("single".equals(trackingMode) && hasPendingCommands()) {
            LOGGER.warn("⛔ [CommandTracker] 单线程模式下拒绝并发指令: request_id={}, command=/{}", requestId, command);
            return new TrackStartResult(TrackStartStatus.REJECTED_BUSY, "409:another command is still running");
        }

        entries.put(requestId, new TrackEntry(ws, requestId, command));
        LOGGER.debug("📝 [CommandTracker] 开始追踪: request_id={}, command=/{}", requestId, command);
        return new TrackStartResult(TrackStartStatus.STARTED, "");
    }

    // 💬 收到聊天消息时调用
    // 📝 在 ClientChatReceivedEvent 处理器中调用
    public void onChatMessage(String text) {
        if (entries.isEmpty()) return;
        if (shouldIgnoreMessage(text)) return;

        long now = System.currentTimeMillis();

        if ("single".equals(Config.COMMAND_TRACKING_MODE.get())) {
            TrackEntry activeEntry = getSingleActiveEntry();
            if (activeEntry != null) {
                activeEntry.outputs.add(text);
                activeEntry.lastOutputMs = now;
            }
            return;
        }

        for (TrackEntry entry : entries.values()) {
            if (!entry.done) {
                entry.outputs.add(text);
                entry.lastOutputMs = now;
            }
        }
    }

    // ⏱️ 每秒 tick 检查超时
    // 📝 在 clientTick 事件中调用
    public void tick() {
        if (entries.isEmpty()) return;
        long now = System.currentTimeMillis();
        List<String> toRemove = new ArrayList<>();
        for (TrackEntry entry : entries.values()) {
            // 跳过已完成的条目
            if (entry.done) {
                toRemove.add(entry.requestId);
                continue;
            }

            long idle = now - entry.lastOutputMs;  // 空闲时间
            long age  = now - entry.createdAt;     // 总生存时间

            // ✅ 判定完成：消抖到期 or 硬超时
            if (idle > DEBOUNCE_MS || age > HARD_TIMEOUT_MS) {
                entry.done = true;
                sendResult(entry);
                toRemove.add(entry.requestId);
            }
        }
        // 清理已完成条目
        toRemove.forEach(entries::remove);
    }

    // 🧹 取消某个客户端的所有追踪（客户端断开时调用）
    public void cancelAll(WebSocket ws) {
        entries.values().removeIf(entry -> entry.ws.equals(ws));
        LOGGER.debug("🧹 [CommandTracker] 已清理客户端 {} 的所有追踪", ws.getRemoteSocketAddress());
    }

    // 🔍 检查当前是否仍有未完成的指令追踪
    // 📝 用于 single 模式下拒绝新的并发 execute_command
    public boolean hasPendingCommands() {
        return entries.values().stream().anyMatch(entry -> !entry.done);
    }

    // 📤 发送 command_result 给客户端
    private void sendResult(TrackEntry entry) {
        // 合并输出行
        String output = String.join("\n", entry.outputs);

        // 构建 command_result JSON
        JsonObject json = new JsonObject();
        json.addProperty("type",       "command_result");
        json.addProperty("request_id", entry.requestId);
        json.addProperty("command",    entry.command);
        json.addProperty("ok",         true);
        json.addProperty("result",     output);

        try {
            if (entry.ws != null && entry.ws.isOpen()) {
                entry.ws.send(json.toString());
                LOGGER.info("📤 [CommandTracker] 已发送结果: request_id={}, command=/{} ({} 行输出)",
                    entry.requestId, entry.command, entry.outputs.size());
            } else {
                LOGGER.warn("⚠️ [CommandTracker] 客户端已断开，无法发送结果: request_id={}", entry.requestId);
            }
        } catch (Exception e) {
            LOGGER.error("💥 [CommandTracker] 发送结果失败: {}", e.getMessage());
        }
    }

    // 🎯 获取 single 模式下当前唯一的活动追踪条目
    // 📝 若所有条目都已完成，则返回 null
    private TrackEntry getSingleActiveEntry() {
        for (TrackEntry entry : entries.values()) {
            if (!entry.done) {
                return entry;
            }
        }
        return null;
    }

    // 🧹 过滤明显不应计入 command_result 的聊天消息
    // 📝 当前排除：普通玩家聊天、玩家加入提示、玩家离开提示
    private boolean shouldIgnoreMessage(String text) {
        if (text == null || text.isBlank()) return true;
        if (text.startsWith("<") && text.contains("> ")) return true;

        String lower = text.toLowerCase();
        return lower.contains(" joined the game")
            || lower.contains(" left the game");
    }
}
