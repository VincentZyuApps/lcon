// 🧩 LCon — WebSocket remote control for Minecraft client
// 📄 模组主入口（Main Entry Point）
// 🧠 在客户端（单人/局域网）中启动 WebSocket 服务端，
//    让外部工具可以远程执行指令和控制游戏
// 🏗️ Forge 1.20.1
// 📋 模组加载流程：
//     1. Forge 调用 LCon() 构造函数
//     2. 注册客户端配置 → 生成 lcon-ws-server.toml
//     3. 从磁盘加载配置文件
//     4. 调用 MainSetup.setup() 注册事件处理器
//     5. 首次玩家 tick → 创建并启动 WebSocket 服务端

package com.cwelth.lcon;

import com.cwelth.lcon.server.WSSListener;
import com.cwelth.lcon.setup.MainSetup;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(LCon.MODID)
public class LCon
{
    // 🆔 模组唯一 ID，用于 Forge 注解和配置文件名
    public static final String MODID = "lcon";

    // 🔌 全局唯一的 WebSocket 服务端实例，在第一次玩家 tick 时创建
    // 🧠 生命周期：
    //     - 创建：第一次 clientTick 事件（进入世界时）
    //     - 销毁：player LoggingOut 事件（退出世界时）
    public static WSSListener wss = null;

    // 🏗️ 构造函数 — Forge 通过 @Mod 注解自动发现并调用
    // ⏱️ 在游戏主菜单加载时就会执行（早于进入世界）
    public LCon()
    {
        // ⚙️ 注册客户端配置（生成 lcon-ws-server.toml）
        // 📄 文件位置：.minecraft/config/lcon-ws-server.toml
        // 📝 配置项包括：端口、token、权限等级、消息文本和 emoji
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_CONFIG);

        // 📂 从磁盘加载配置文件
        // 🧠 如果文件不存在则使用默认值创建
        Config.loadConfig(Config.CLIENT_CONFIG, FMLPaths.CONFIGDIR.get().resolve(MODID + "-ws-server.toml"));

        // 🚀 初始化事件处理器
        // 📋 注册：ClientTickEvent、LoggingOut、ChatReceived 等监听器
        MainSetup.setup();
    }
}
