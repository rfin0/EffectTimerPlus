package dev.terminalmc.effecttimerplus.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.terminalmc.effecttimerplus.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.function.Consumer;

import static dev.terminalmc.effecttimerplus.util.IndicatorUtil.*;

@Mixin(value = Gui.class, priority = 500)
public class MixinGui {

    @Final
    @Shadow
    private Minecraft minecraft;

    @Unique
    @Nullable
    private Runnable effectTimerPlus$runnable;

    @Unique
    private void effectTimerPlus$scale(GuiGraphics graphics) {
        float scale = (float) Config.get().scale;
        graphics.pose().pushPose();
        graphics.pose().translate(graphics.guiWidth() * (1 - scale), 0.0F, 0.0F);
        graphics.pose().scale(scale, scale, 0.0F);
    }

    @Unique
    private void effectTimerPlus$descale(GuiGraphics graphics) {
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
        effectTimerPlus$scale(graphics);
        original.call(graphics, sprite, x, y, width, height);
        effectTimerPlus$descale(graphics);

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

    @WrapOperation(
            method = "renderEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V"
            )
    )
    private void scale(List instance, Consumer consumer, Operation<Void> original,
                       @Local(argsOnly = true) GuiGraphics graphics) {
        effectTimerPlus$scale(graphics);

        original.call(instance, consumer);

        effectTimerPlus$descale(graphics);
    }
}
