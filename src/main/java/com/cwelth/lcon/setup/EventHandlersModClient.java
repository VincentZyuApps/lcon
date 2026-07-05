// 🧩 LCon — 客户端事件处理器
// 📄 监听 Minecraft 客户端事件，管理 WS 服务端的生命周期：
//   - PlayerTickEvent  → 启动 / 更新 WebSocket 服务端
//   - LoggingOut      → 关闭 WebSocket 服务端
//   - ChatReceived    → 将聊天消息广播给所有 WS 客户端

package com.cwelth.lcon.setup;


import com.cwelth.lcon.Config;
import com.cwelth.lcon.LCon;
import com.cwelth.lcon.mclistener.CommandTracker;
import com.cwelth.lcon.mclistener.MclistenerWSS;
import com.cwelth.lcon.server.WSSListener;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = LCon.MODID)
public class EventHandlersModClient {
    // 🪵 日志记录器
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long PLAYER_LIFECYCLE_DEDUPE_WINDOW_MS = 2000L;
    private static final Map<String, Long> RECENT_PLAYER_LIFECYCLE_EVENTS = new HashMap<>();

    @SubscribeEvent
    // 🎨 创造模式标签页（暂无自定义物品，保留为空）
    public static void addCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if(event.getTabKey() == CreativeModeTabs.INGREDIENTS)
        {

        }
    }

    @SubscribeEvent
    // 🚪 玩家进入世界时触发 — 广播 player_join 到 mclistener 客户端
    // 🧠 监听所有 Player 类型实体加入（包含本地玩家和其他在线玩家）
    public static void entityJoinLevel(EntityJoinLevelEvent event) {
        if (!Config.ENABLE_MCLISTENER.get() || !Config.ENABLE_PLAYER_JOIN_BROADCAST.get()) return;
        if (LCon.mclistenerWss == null) return;
        if (event.getEntity() instanceof Player player) {
            if (!player.level().isClientSide) return;
            if (isDuplicatePlayerLifecycleEvent("player_join", player)) return;
            String playerName = player.getScoreboardName();
            JsonObject json = new JsonObject();
            json.addProperty("type", "player_join");
            json.addProperty("player_name", playerName);
            json.addProperty("player_uuid", player.getUUID().toString());
            LCon.mclistenerWss.broadcastJson(json.toString());
            LOGGER.info("📢 [Mclistener] 玩家加入广播: {}", playerName);
        }
    }

    @SubscribeEvent
    // 🚪 玩家离开世界时触发 — 广播 player_leave 到 mclistener 客户端
    public static void entityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!Config.ENABLE_MCLISTENER.get() || !Config.ENABLE_PLAYER_LEAVE_BROADCAST.get()) return;
        if (LCon.mclistenerWss == null) return;
        if (event.getEntity() instanceof Player player) {
            if (!player.level().isClientSide) return;
            if (isDuplicatePlayerLifecycleEvent("player_leave", player)) return;
            String playerName = player.getScoreboardName();
            JsonObject json = new JsonObject();
            json.addProperty("type", "player_leave");
            json.addProperty("player_name", playerName);
            json.addProperty("player_uuid", player.getUUID().toString());
            LCon.mclistenerWss.broadcastJson(json.toString());
            LOGGER.info("📢 [Mclistener] 玩家离开广播: {}", playerName);
        }
    }

    private static boolean isDuplicatePlayerLifecycleEvent(String type, Player player) {
        long now = System.currentTimeMillis();
        pruneRecentPlayerLifecycleEvents(now);

        String key = type + ":" + player.getUUID();
        Long lastAt = RECENT_PLAYER_LIFECYCLE_EVENTS.put(key, now);
        if (lastAt == null) return false;

        boolean duplicate = now - lastAt <= PLAYER_LIFECYCLE_DEDUPE_WINDOW_MS;
        if (duplicate) {
            LOGGER.debug("🔁 [Mclistener] 跳过重复玩家生命周期事件: {} {}", type, player.getScoreboardName());
        }
        return duplicate;
    }

    private static void pruneRecentPlayerLifecycleEvents(long now) {
        Iterator<Map.Entry<String, Long>> iterator = RECENT_PLAYER_LIFECYCLE_EVENTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (now - entry.getValue() > PLAYER_LIFECYCLE_DEDUPE_WINDOW_MS) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    // 🕐 玩家每 tick 触发一次 — 用于管理 WS 服务端生命周期
    // ⚠️ 局域网联机时，其他玩家的 RemotePlayer 也会触发此事件
    //    必须用 instanceof 跳过，否则强转 LocalPlayer 会 ClassCastException
    public static void clientTick(TickEvent.PlayerTickEvent event) throws IOException {
        if(!event.player.level().isClientSide) return;
        // 👤 跳过远程玩家（局域网加入的朋友），只处理本地玩家
        if (!(event.player instanceof LocalPlayer player)) return;
        if(player != null)
        {
            if(Config.ENABLE_MOD.get())
            {
                // 🚀 首次进入世界 → 创建旧前缀协议 WS 服务端并启动
                if(LCon.wss == null)
                {
                    LCon.wss = new WSSListener(Config.PORT.get(), player);
                    LCon.wss.start();
                } else
                    // 🔄 已存在 → 更新玩家引用（防止过期）
                    LCon.wss.updatePlayer(player);
            }

            // 🌐 Mclistener 协议 WS 服务端生命周期管理
            if(Config.ENABLE_MCLISTENER.get())
            {
                // 🚀 首次进入世界 → 创建 mclistener WS 服务端
                if(LCon.mclistenerWss == null)
                {
                    LCon.mclistenerWss = new MclistenerWSS(Config.MCLISTENER_PORT.get());
                    LCon.mclistenerWss.start();
                }
            } else {
                // 🔌 禁用时关闭 mclistener WS 服务端
                if(LCon.mclistenerWss != null)
                {
                    try {
                        LCon.mclistenerWss.stop(3, "201:closed.");
                    } catch (InterruptedException e) {
                        // 🚫 忽略中断异常
                    } finally {
                        LCon.mclistenerWss = null;
                    }
                }
            }

            // 🎯 首次 tick → 创建指令追踪器
            if(LCon.commandTracker == null)
            {
                LCon.commandTracker = new CommandTracker();
            }
        }

        // 🎯 指令追踪器 tick（检查超时，发送结果）
        if(LCon.commandTracker != null)
        {
            LCon.commandTracker.tick();
        }
    }

    @SubscribeEvent
    // 🚪 玩家登出时调用 — 关闭所有 WS 服务端
    public void logOut(ClientPlayerNetworkEvent.LoggingOut event){
        if(event.getConnection() == null) return;

        // 🔌 关闭旧前缀协议 WS 服务端
        if(LCon.wss != null) {
            try {
                LCon.wss.stop(3, "201:closed.");
            } catch (InterruptedException e) {
            } finally {
                LCon.wss = null;
            }
        }

        // 🔌 关闭 mclistener WS 服务端
        if(LCon.mclistenerWss != null) {
            try {
                LCon.mclistenerWss.stop(3, "201:closed.");
            } catch (InterruptedException e) {
            } finally {
                LCon.mclistenerWss = null;
            }
        }

        // 🧹 清空指令追踪器
        LCon.commandTracker = null;
        RECENT_PLAYER_LIFECYCLE_EVENTS.clear();
    }

    @SubscribeEvent
    // 💬 收到聊天消息时调用 — 广播给所有连接的 WS 客户端
    // 🧠 有三种输出：
    //   1️⃣（保留）旧前缀协议 → Python TUI 客户端
    //   2️⃣（新增）mclistener JSON 协议 → Koishi 客户端
    //   3️⃣（新增）投喂给 CommandTracker → 指令结果追踪
    // 📎 通过 lcon-ws-server.toml 中的 serializer_mode 配置项切换
    // 📎 mclistener 配置在 [mclistener] 分类下
    public static void getChatMessage(ClientChatReceivedEvent event) throws IOException {
        Component message = event.getMessage();
        String fullText = message.getString();

        // 🆕 投喂给指令追踪器（所有聊天消息都喂，由追踪器自行过滤）
        if(LCon.commandTracker != null) {
            LCon.commandTracker.onChatMessage(fullText);
        }

        // 🆕 Mclistener 协议广播（player_chat JSON 格式）
        if(Config.ENABLE_MCLISTENER.get() && Config.ENABLE_PLAYER_CHAT_BROADCAST.get() && LCon.mclistenerWss != null) {
            broadcastPlayerChat(event, fullText);
        }

        // ⬇️ 旧前缀协议广播（完全保留不动）
        if(LCon.wss != null)
        {
            String mode = Config.SERIALIZER_MODE.get();
            String serialized;

            if ("json".equals(mode)) {
                // 📦 JSON 模式 — 使用 Minecraft 标准的文本组件 JSON 序列化
                // ✅ 官方 API，健壮稳定，推荐 Python TUI 使用
                serialized = Component.Serializer.toJson(message);
            } else if ("tostring".equals(mode)) {
                // 🔙 tostring 模式 — 使用 ComponentContents.toString() 旧格式
                // ⚠️ 非标准格式，保留用于向后兼容
                Gson gson = new Gson();
                serialized = gson.toJson(message.getContents().toString());
            } else {
                // 🚨 未知模式 — 回退到 JSON 默认行为
                serialized = Component.Serializer.toJson(message);
            }

            LCon.wss.broadcast("200:" + serialized);
        }
    }

    private static void broadcastPlayerChat(ClientChatReceivedEvent event, String fullText) {
        String captureMode = Config.PLAYER_CHAT_CAPTURE_MODE.get();

        ChatBroadcastPayload eventPayload = null;
        ChatBroadcastPayload textPayload = null;

        if ("event".equals(captureMode) || "both".equals(captureMode)) {
            eventPayload = extractPlayerChatFromEvent(event);
        }
        if ("text".equals(captureMode) || ("both".equals(captureMode) && eventPayload == null)) {
            textPayload = extractPlayerChatFromText(fullText);
        }

        ChatBroadcastPayload payload = eventPayload != null ? eventPayload : textPayload;
        if (payload == null) return;

        JsonObject json = new JsonObject();
        json.addProperty("type", "player_chat");
        json.addProperty("player_name", payload.playerName());
        json.addProperty("content", payload.content());
        if (payload.playerUuid() != null) {
            json.addProperty("player_uuid", payload.playerUuid().toString());
        }
        LCon.mclistenerWss.broadcastJson(json.toString());
    }

    private static ChatBroadcastPayload extractPlayerChatFromEvent(ClientChatReceivedEvent event) {
        if (!(event instanceof ClientChatReceivedEvent.Player playerEvent)) {
            return null;
        }

        String playerName = playerEvent.getBoundChatType().name().getString();
        String content = playerEvent.getPlayerChatMessage().signedContent();
        if (content == null || content.isBlank()) {
            content = playerEvent.getMessage().getString();
        }

        if (playerName == null || playerName.isBlank() || content == null || content.isBlank()) {
            return null;
        }

        return new ChatBroadcastPayload(playerName, content, playerEvent.getSender());
    }

    private static ChatBroadcastPayload extractPlayerChatFromText(String fullText) {
        if (fullText == null || !fullText.startsWith("<") || !fullText.contains("> ")) {
            return null;
        }

        int nameEnd = fullText.indexOf("> ");
        if (nameEnd <= 1 || nameEnd + 2 >= fullText.length()) {
            return null;
        }

        String playerName = fullText.substring(1, nameEnd);
        String content = fullText.substring(nameEnd + 2);
        if (playerName.isBlank() || content.isBlank()) {
            return null;
        }

        return new ChatBroadcastPayload(playerName, content, null);
    }

    private record ChatBroadcastPayload(String playerName, String content, UUID playerUuid) {}
}
