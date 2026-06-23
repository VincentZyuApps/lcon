// 🚀 LCon — 模组主初始化流程
// 📄 在模组构造完成后调用，负责注册所有事件处理器
// 📋 初始化顺序：
//     1. LCon() 构造函数 → 加载配置
//     2. MainSetup.setup() → 注册处理器（本文件）
//     3. Registries.setup() → 获取 Mod 总线
//     4. EventHandlersForge → 注册到 Forge 总线（待扩展）
//     5. EventHandlersModClient → 自动注册（@Mod.EventBusSubscriber）

package com.cwelth.lcon.setup;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

public class MainSetup {
    public static void setup() {
        // 📦 初始化注册表（目前为空，预留）
        Registries.setup();

        // 📡 获取 Forge 运行时事件总线
        IEventBus bus = MinecraftForge.EVENT_BUS;

        // 📋 注册 Forge 总线事件处理器
        // 💡 EventHandlersModClient 使用 @Mod.EventBusSubscriber 自动注册
        //    不需要在此手动 register()
        bus.register(new EventHandlersForge());
    }
}
