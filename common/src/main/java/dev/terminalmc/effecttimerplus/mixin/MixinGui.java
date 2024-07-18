package dev.terminalmc.effecttimerplus.mixin;

import com.google.common.collect.Ordering;
import dev.terminalmc.effecttimerplus.config.Config;
import dev.terminalmc.effecttimerplus.util.IndicatorUtil;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

/**
 * Includes derivative work of code used by
 * <a href="https://github.com/magicus/statuseffecttimer/">Status Effect Timer</a>
 */
@Mixin(value = Gui.class, priority = 500)
public class MixinGui {

    @Final
    @Shadow
    private Minecraft minecraft;

    @Inject(method = "renderEffects", at = @At("HEAD"))
    private void scaleGraphics(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        float scale = (float) Config.get().scale;
        graphics.pose().pushPose();
        graphics.pose().translate(graphics.guiWidth() * (1 - scale), 0.0F, 0.0F);
        graphics.pose().scale(scale, scale, 0.0F);
    }

    @Inject(method = "renderEffects", at = @At("RETURN"))
    private void descaleGraphicsAndOverlay(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        // Replicate vanilla placement algorithm to place labels correctly
        Collection<MobEffectInstance> effects = this.minecraft.player.getActiveEffects();
        if (!effects.isEmpty()) {

            int beneficialCount = 0;
            int nonBeneficialCount = 0;

            for (MobEffectInstance effectInstance : Ordering.natural().reverse().sortedCopy(effects)) {
                Holder<MobEffect> effect = effectInstance.getEffect();
                if (effectInstance.showIcon()) {
                    int x = graphics.guiWidth();
                    int y = 1;
                    if (this.minecraft.isDemo()) {
                        y += 15;
                    }

                    if (effect.value().isBeneficial()) {
                        ++beneficialCount;
                        x -= 25 * beneficialCount;
                    } else {
                        ++nonBeneficialCount;
                        x -= 25 * nonBeneficialCount;
                        y += 26;
                    }

                    Config options = Config.get();
                    // Render potency overlay
                    if (options.potencyEnabled && effectInstance.getAmplifier() > 0) {
                        String label = IndicatorUtil.getAmplifierAsString(effectInstance.getAmplifier());
                        int labelWidth = minecraft.font.width(label);
                        int posX = x + IndicatorUtil.getTextOffsetX(options.potencyLocation, labelWidth);
                        int posY = y + IndicatorUtil.getTextOffsetY(options.potencyLocation);
                        graphics.fill(posX, posY, posX + labelWidth, posY + minecraft.font.lineHeight - 1,
                                options.potencyBackColor);
                        graphics.drawString(minecraft.font, label, posX, posY, options.potencyColor, false);
                    }
                    // Render timer overlay
                    if (options.timerEnabled && (options.timerEnabledAmbient || !effectInstance.isAmbient())) {
                        String label = IndicatorUtil.getDurationAsString(effectInstance.getDuration());
                        int labelWidth = minecraft.font.width(label);
                        int posX = x + IndicatorUtil.getTextOffsetX(options.timerLocation, labelWidth);
                        int posY = y + IndicatorUtil.getTextOffsetY(options.timerLocation);
                        graphics.fill(posX, posY, posX + labelWidth, posY + minecraft.font.lineHeight - 1,
                                options.timerBackColor);
                        int color = IndicatorUtil.getTimerColor(effectInstance, options.timerColor,
                                options.timerWarnEnabled, options.timerWarnTime,
                                options.timerWarnColor, options.timerFlashEnabled);
                        graphics.drawString(minecraft.font, label, posX, posY, color, false);
                    }
                }
            }
            graphics.pose().popPose();
        }
    }
}
