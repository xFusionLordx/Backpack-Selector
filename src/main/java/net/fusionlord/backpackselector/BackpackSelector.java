package net.fusionlord.backpackselector;

import net.fusionlord.backpackselector.client.gui.BackpackRadialScreen;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

@Mod(BackpackSelector.MODID)
public class BackpackSelector {
    public static final String MODID = "backpackselector";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static BackpackRadialScreen radial = new BackpackRadialScreen();

    public static boolean selector_open = false;
    public static int backpackKeyHoldTicks = 0;
    public static boolean waitingForKeyRelease = false;

    public static final Identifier BACKPACK_SELECTOR = Identifier.fromNamespaceAndPath(BackpackSelector.MODID, "backpack_selector");

    public BackpackSelector(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.register(this);
        NeoForge.EVENT_BUS.addListener(this::onRawMouseInput);
    }

    @SubscribeEvent
    public void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(BACKPACK_SELECTOR, BackpackSelector::extractBackpackRadial);
    }

    private static void extractBackpackRadial(GuiGraphicsExtractor guiGraphicsExtractor, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (selector_open) {
            mc.mouseHandler.releaseMouse();
            radial.extractRenderState(guiGraphicsExtractor, deltaTracker);
        }
    }

    public void onRawMouseInput(InputEvent.MouseButton.Pre event) {
        if (BackpackSelector.selector_open && Minecraft.getInstance().screen == null) {
            if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT && event.getAction() == GLFW.GLFW_PRESS) {
                BackpackSelector.selector_open = false;
                BackpackSelector.radial.openBackpack();

                event.setCanceled(true);
            }
        }
    }
}
