package dev.terminalmc.effecttimerplus;

import dev.terminalmc.effecttimerplus.gui.screen.ConfigScreenProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = EffectTimerPlus.MOD_ID, dist = Dist.CLIENT)
public class EffectTimerPlusNeoForge {
    public EffectTimerPlusNeoForge() {
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
                () -> (mc, parent) -> ConfigScreenProvider.getConfigScreen(parent));

        EffectTimerPlus.init();
    }
}
