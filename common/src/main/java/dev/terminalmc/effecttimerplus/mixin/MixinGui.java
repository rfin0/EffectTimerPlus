package dev.terminalmc.effecttimerplus.mixin;

import com.google.common.collect.Ordering;
import dev.terminalmc.effecttimerplus.config.Config;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
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

import static dev.terminalmc.effecttimerplus.util.IndicatorUtil.*;

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

        if (effects.isEmpty() || this.minecraft.screen instanceof EffectRenderingInventoryScreen) {
            graphics.pose().popPose();
            return;
        }

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
                    String label = getAmplifierAsString(effectInstance.getAmplifier());
                    int labelWidth = minecraft.font.width(label);
                    int posX = x + getTextOffsetX(options.potencyLocation, labelWidth);
                    int posY = y + getTextOffsetY(options.potencyLocation, minecraft.font.lineHeight);

                    float scale = (float)Config.get().potencyScale;
                    graphics.pose().pushPose();
                    graphics.pose().translate(posX * (1 - scale), posY * (1 - scale), 0.0F);
                    graphics.pose().translate(getScaleTranslateX(options.potencyLocation, labelWidth, scale),
                            getScaleTranslateY(options.potencyLocation, minecraft.font.lineHeight, scale), 0.0F);
                    graphics.pose().scale(scale, scale, 0.0F);
                    if (options.potencyBack) {
                        graphics.fill(posX - 1, posY - 1, posX + labelWidth,
                                posY + minecraft.font.lineHeight - 1, options.potencyBackColor);
                    }
                    graphics.drawString(minecraft.font, label, posX, posY, options.potencyColor, options.potencyShadow);
                    graphics.pose().popPose();
                }
                // Render timer overlay
                if (options.timerEnabled && (options.timerEnabledAmbient || !effectInstance.isAmbient())) {
                    String label = getDurationAsString(effectInstance.getDuration());
                    int labelWidth = minecraft.font.width(label);
                    int posX = x + getTextOffsetX(options.timerLocation, labelWidth);
                    int posY = y + getTextOffsetY(options.timerLocation, minecraft.font.lineHeight);

                    int color = getTimerColor(effectInstance, options.timerColor,
                            options.timerWarnEnabled, options.timerWarnTime,
                            options.timerWarnColor, options.timerFlashEnabled);
                    float scale = (float)Config.get().timerScale;
                    graphics.pose().pushPose();
                    graphics.pose().translate(posX * (1 - scale), posY * (1 - scale), 0.0F);
                    graphics.pose().translate(getScaleTranslateX(options.timerLocation, labelWidth, scale),
                            getScaleTranslateY(options.timerLocation, minecraft.font.lineHeight, scale), 0.0F);
                    graphics.pose().scale(scale, scale, 0.0F);
                    if (options.timerBack) {
                        graphics.fill(posX - 1, posY - 1, posX + labelWidth,
                                posY + minecraft.font.lineHeight - 1, options.timerBackColor);
                    }
                    graphics.drawString(minecraft.font, label, posX, posY, color, options.timerShadow);
                    graphics.pose().popPose();
                }
            }
        }
        graphics.pose().popPose();
    }
}
