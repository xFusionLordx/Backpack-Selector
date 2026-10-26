package net.fusionlord.backpackselector.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.p3pp3rf1y.sophisticatedbackpacks.client.KeybindHandler;
import net.fusionlord.backpackselector.BackpackSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeybindHandler.class)
public class KeybindHandlerMixin {
    @Unique
    private static final int HOLD_THRESHOLD_TICKS = 5;

    @Inject(
            method = "handleKeyInputEvent",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void onHandleKeyInputEvent(ClientTickEvent.Post event, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        boolean isKeyDown = InputConstants.isKeyDown(mc.getWindow(), KeybindHandler.BACKPACK_OPEN_KEYBIND.getKey().getValue());

        if (mc.player != null) {
            if (mc.screen == null) {
                if (isKeyDown) {
                    BackpackSelector.backpackKeyHoldTicks++;

                    if (BackpackSelector.backpackKeyHoldTicks >= HOLD_THRESHOLD_TICKS) {
                        while (KeybindHandler.BACKPACK_OPEN_KEYBIND.consumeClick()) {}
                        if (!BackpackSelector.waitingForKeyRelease)
                            BackpackSelector.waitingForKeyRelease = true;
                        if (!BackpackSelector.selector_open) {
                            BackpackSelector.selector_open = true;
                        }
                    }
                    ci.cancel();

                } else {
                    if (BackpackSelector.backpackKeyHoldTicks > HOLD_THRESHOLD_TICKS) {
                        if (BackpackSelector.selector_open) {
                            while (KeybindHandler.BACKPACK_OPEN_KEYBIND.consumeClick()) {}

                            BackpackSelector.waitingForKeyRelease = false;
                            BackpackSelector.selector_open = false;
                            BackpackSelector.radial.openBackpack();
                        }
                        ci.cancel();
                    }
                    BackpackSelector.backpackKeyHoldTicks = 0;
                }
            }

            if (BackpackSelector.waitingForKeyRelease) {
                if (!isKeyDown) {
                    BackpackSelector.selector_open = false;
                    BackpackSelector.waitingForKeyRelease = false;
                    BackpackSelector.backpackKeyHoldTicks = 0;
                }
            }
        }


    }
}
