package net.fusionlord.backpackselector.mixin;

import net.fusionlord.backpackselector.BackpackSelector;
import net.minecraft.client.input.KeyEvent;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BackpackScreen.class)
public class BackpackScreenMixin {
    @Inject(method = "keyPressed",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    public void keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (BackpackSelector.waitingForKeyRelease) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
