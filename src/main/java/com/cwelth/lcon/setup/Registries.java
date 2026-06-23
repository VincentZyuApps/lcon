// 📦 LCon — Forge Mod 事件总线注册
// 📄 获取 Mod 总线引用，为后续注册物品/方块/实体等做准备
// 🧠 Forge 的两层事件系统：
//     - Mod 总线（ModEventBus）：模组加载阶段，用于注册内容
//     - Forge 总线（MinecraftForge.EVENT_BUS）：运行时事件
// 💡 目前未注册任何内容，保留为空供后续扩展

package com.cwelth.lcon.setup;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import static com.cwelth.lcon.LCon.MODID;

public class Registries {

    public static void setup() {
        // 📡 获取 Forge Mod 事件总线
        // 🧠 可在此通过 bus.register() / bus.addListener() 注册：
        //     - 物品（DeferredRegister）
        //     - 方块（DeferredRegister）
        //     - 实体类型（DeferredRegister）
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
    }
}
