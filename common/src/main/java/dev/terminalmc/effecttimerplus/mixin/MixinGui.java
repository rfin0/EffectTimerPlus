/*
 * EffectTimerPlus
 * Copyright (C) 2024 magicus
 * Copyright (C) 2025 TerminalMC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.terminalmc.effecttimerplus.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.terminalmc.effecttimerplus.config.Config;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static dev.terminalmc.effecttimerplus.util.IndicatorUtil.*;

/**
 * This file includes derivative work of code from
 * <a href="https://github.com/magicus/statuseffecttimer">Status Effect Timer</a>
 */
@Mixin(value = Gui.class, priority = 2000)
public class MixinGui {

    @Final
    @Shadow
    private Minecraft minecraft;

    @Unique
    @Nullable
    private Runnable effectTimerPlus$runnable;

    @Inject(
            method = "renderEffects",
            at = @At("HEAD")
    )
    private void scale(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        float scale = (float) Config.get().scale;
        graphics.pose().pushPose();
        graphics.pose().translate(graphics.guiWidth() * (1 - scale), 0.0F, 0.0F);
        graphics.pose().scale(scale, scale, 0.0F);
    }

    @Inject(
            method = "renderEffects",
            at = @At("RETURN")
    )
    private void descale(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        graphics.pose().popPose();
    }

    @WrapOperation(
            method = "renderEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"
            )
    )
    private void CreateOverlayRunnable(GuiGraphics graphics, ResourceLocation sprite, int x, int y,
                                       int width, int height, Operation<Void> original,
                                       @Local MobEffectInstance effectInstance) {
        original.call(graphics, sprite, x, y, width, height);

        Config options = Config.get();
        effectTimerPlus$runnable = () -> {
            // Render potency overlay
            if (options.potencyEnabled && effectInstance.getAmplifier() > 0) {
                String label = getAmplifierAsString(effectInstance.getAmplifier());
                int labelWidth = minecraft.font.width(label);
                int posX = x + getTextOffsetX(options.potencyLocation, labelWidth, width);
                int posY = y + getTextOffsetY(options.potencyLocation, minecraft.font.lineHeight, height);

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
                int posX = x + getTextOffsetX(options.timerLocation, labelWidth, width);
                int posY = y + getTextOffsetY(options.timerLocation, minecraft.font.lineHeight, height);

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
        };
    }

    @ModifyExpressionValue(
            method = "renderEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;add(Ljava/lang/Object;)Z"
            )
    )
    private boolean AddOverlayRunnable(boolean original, @Local List<Runnable> runnables) {
        if (effectTimerPlus$runnable != null) {
            runnables.add(effectTimerPlus$runnable);
            effectTimerPlus$runnable = null;
        }
        return original;
    }
}
