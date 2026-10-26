package net.fusionlord.backpackselector.client.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackOpenPayload;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BackpackRadialScreen {

    private record RadialSlotContext(ItemStack stack, int slotIndex, String handlerName, String identifier) {}

    private final List<RadialSlotContext> foundBackpacks = new ArrayList<>();
    private int selectedSliceIndex = -1;

    public BackpackRadialScreen() {}

    private void scanForBackpacks() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        this.foundBackpacks.clear();
        PlayerInventoryProvider.get().findBackpack(mc.player, (stack, handlerName, identifier, slotIndex) -> {
            foundBackpacks.add(new RadialSlotContext(stack, slotIndex, handlerName, identifier));
            return false;
        });
    }

    public void openBackpack() {
        Minecraft mc = Minecraft.getInstance();
        mc.mouseHandler.grabMouse();
        if (this.selectedSliceIndex == -1 || this.selectedSliceIndex >= foundBackpacks.size()) {
            return;
        }
        ClientPacketDistributor.sendToServer(getPacketPayload());
        this.selectedSliceIndex = -1;
    }

    private @NonNull BackpackOpenPayload getPacketPayload() {
        RadialSlotContext choice = foundBackpacks.get(this.selectedSliceIndex);
        return new BackpackOpenPayload(choice.slotIndex(), choice.identifier(), choice.handlerName());
    }

    public void extractRenderState(@NonNull GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        mc.mouseHandler.releaseMouse();

        int centerX = mc.getWindow().getGuiScaledWidth() / 2;
        int centerY = mc.getWindow().getGuiScaledHeight() / 2;

        double scaleFactor = mc.getWindow().getGuiScale();
        double mouseX = mc.mouseHandler.xpos() / scaleFactor;
        double mouseY = mc.mouseHandler.ypos() / scaleFactor;

        this.scanForBackpacks();
        int totalSlices = foundBackpacks.size();

        if (totalSlices == 0) {
            guiGraphics.centeredText(mc.font, "No Backpacks Found", centerX, 10, 0xFFFF5555);
            return;
        }

        this.selectedSliceIndex = calculateSelectionIndex(mouseX, mouseY, centerX, centerY, totalSlices);
        renderBackgroundTrapezoids(guiGraphics, centerX, centerY, totalSlices);
        renderForegroundItemStacks(guiGraphics, mc, centerX, centerY, totalSlices);
        if (this.selectedSliceIndex != -1 && this.selectedSliceIndex < totalSlices) {
            renderOverlayTooltips(guiGraphics, mc, mouseX, mouseY, centerY, totalSlices);
        }
    }

    private int calculateSelectionIndex(double mouseX, double centerY, int centerX, int centerYPos, int totalSlices) {
        double deltaX = mouseX - centerX;
        double deltaY = centerY - centerYPos;
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        if (distance > 21) {
            double angle = Math.atan2(deltaY, deltaX) + Math.PI / 2;
            if (angle < 0) angle += 2 * Math.PI;

            double sliceSize = (2 * Math.PI) / totalSlices;
            return (int) Math.floor((angle + (sliceSize / 2)) / sliceSize) % totalSlices;
        }
        return -1;
    }

    private void renderBackgroundTrapezoids(GuiGraphicsExtractor guiGraphics, int centerX, int centerY, int totalSlices) {
        double sliceSize = (2 * Math.PI) / totalSlices;
        final int DEADZONE_RADIUS = 20;
        final int OUTER_RADIUS = DEADZONE_RADIUS + 44;
        final float GAP_ANGLE = 0.05f;

        for (int i = 0; i < totalSlices; i++) {
            boolean isHovered = (i == this.selectedSliceIndex);
            int color = isHovered ? 0x90FFFFFF : 0x50000000;

            double sliceAngle = (i * sliceSize) - Math.PI / 2;
            double startAngle = sliceAngle - (sliceSize / 2) + (GAP_ANGLE / 2);
            double endAngle = sliceAngle + (sliceSize / 2) - (GAP_ANGLE / 2);

            float outerLeftX  = (float) (centerX + Math.cos(startAngle) * OUTER_RADIUS);
            float outerLeftY  = (float) (centerY + Math.sin(startAngle) * OUTER_RADIUS);
            float innerLeftX  = (float) (centerX + Math.cos(startAngle) * (DEADZONE_RADIUS + 3));
            float innerLeftY  = (float) (centerY + Math.sin(startAngle) * (DEADZONE_RADIUS + 3));
            float innerRightX = (float) (centerX + Math.cos(endAngle) * (DEADZONE_RADIUS + 3));
            float innerRightY = (float) (centerY + Math.sin(endAngle) * (DEADZONE_RADIUS + 3));
            float outerRightX = (float) (centerX + Math.cos(endAngle) * OUTER_RADIUS);
            float outerRightY = (float) (centerY + Math.sin(endAngle) * OUTER_RADIUS);

            guiGraphics.submitGuiElementRenderState(
                    new SmoothTrapezoidRenderState(
                            RenderPipelines.GUI,
                            TextureSetup.noTexture(),
                            new Matrix3x2f(guiGraphics.pose()),
                            outerLeftX, outerLeftY,
                            innerLeftX, innerLeftY,
                            innerRightX, innerRightY,
                            outerRightX, outerRightY,
                            color
                    )
            );
        }
    }

    private void renderForegroundItemStacks(GuiGraphicsExtractor guiGraphics, Minecraft mc, int centerX, int centerY, int totalSlices) {
        double sliceSize = (2 * Math.PI) / totalSlices;
        final int DEADZONE_RADIUS = 20;
        final int OUTER_RADIUS = DEADZONE_RADIUS + 44;
        int radiusToItemCenter = DEADZONE_RADIUS + 3 + ((OUTER_RADIUS - DEADZONE_RADIUS) / 2);

        for (int i = 0; i < totalSlices; i++) {
            RadialSlotContext ctx = foundBackpacks.get(i);
            double sliceAngle = (i * sliceSize) - Math.PI / 2;
            int itemX = (int) (centerX + Math.cos(sliceAngle) * radiusToItemCenter);
            int itemY = (int) (centerY + Math.sin(sliceAngle) * radiusToItemCenter);

            guiGraphics.pose().pushMatrix();
            guiGraphics.fakeItem(ctx.stack(), itemX - 8, itemY - 8);
            guiGraphics.itemDecorations(mc.font, ctx.stack(), itemX - 8, itemY - 8);
            guiGraphics.pose().popMatrix();
        }
    }

    private void renderOverlayTooltips(GuiGraphicsExtractor guiGraphics, Minecraft mc, double mouseX, double mouseY, int centerY, int totalSlices) {
        RadialSlotContext ctx = foundBackpacks.get(this.selectedSliceIndex);
        ItemStack itemStack = ctx.stack;

        guiGraphics.nextStratum();
        List<Component> textComponents = Screen.getTooltipFromItem(mc, itemStack);

        List<Component> cleanedTextComponents = new ArrayList<>();
        for (Component component : textComponents) {
            String stringKey = component.getString();
            if (!stringKey.contains("press_for_contents") && !stringKey.contains("to View Contents")) {
                cleanedTextComponents.add(component);
            }
        }

        List<ClientTooltipComponent> finalTooltipComponents = new ArrayList<>(
                cleanedTextComponents.stream()
                        .map(Component::getVisualOrderText)
                        .map(ClientTooltipComponent::create)
                        .toList()
        );

        if (itemStack.getItem() instanceof net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem backpackItem) {
            backpackItem.getInventoryTooltip(itemStack).ifPresent(tooltipComponent -> {
                finalTooltipComponents.add(1, ClientTooltipComponent.create(tooltipComponent));
            });
        }

        guiGraphics.tooltip(
                mc.font,
                finalTooltipComponents,
                (int) mouseX,
                (int) mouseY,
                DefaultTooltipPositioner.INSTANCE,
                itemStack.get(net.minecraft.core.component.DataComponents.TOOLTIP_STYLE)
        );
    }

    public record SmoothTrapezoidRenderState(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            Matrix3x2fc pose,
            float x0, float y0,
            float x1, float y1,
            float x2, float y2,
            float x3, float y3,
            int color,
            @Nullable ScreenRectangle bounds
    ) implements GuiElementRenderState {
        public SmoothTrapezoidRenderState(
                RenderPipeline pipeline,
                TextureSetup textureSetup,
                Matrix3x2fc pose,
                float x0, float y0,
                float x1, float y1,
                float x2, float y2,
                float x3, float y3,
                int color
        ) {
            this(pipeline, textureSetup, pose, x0, y0, x1, y1, x2, y2, x3, y3, color,
                    calculateBounds(x0, y0, x1, y1, x2, y2, x3, y3, pose));
        }

        @Override
        public void buildVertices(VertexConsumer vertexConsumer) {
            vertexConsumer.addVertexWith2DPose(this.pose(), this.x0(), this.y0()).setColor(this.color());
            vertexConsumer.addVertexWith2DPose(this.pose(), this.x1(), this.y1()).setColor(this.color());
            vertexConsumer.addVertexWith2DPose(this.pose(), this.x2(), this.y2()).setColor(this.color());
            vertexConsumer.addVertexWith2DPose(this.pose(), this.x3(), this.y3()).setColor(this.color());
        }

        private static ScreenRectangle calculateBounds(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, Matrix3x2fc pose) {
            int minX = (int) Math.min(Math.min(x0, x1), Math.min(x2, x3));
            int maxX = (int) Math.max(Math.max(x0, x1), Math.max(x2, x3));
            int minY = (int) Math.min(Math.min(y0, y1), Math.min(y2, y3));
            int maxY = (int) Math.max(Math.max(y0, y1), Math.max(y2, y3));

            return new ScreenRectangle(minX, minY, maxX - minX, maxY - minY).transformMaxBounds(pose);
        }

        @Override public @NonNull RenderPipeline pipeline() { return this.pipeline; }
        @Override public @NonNull TextureSetup textureSetup() { return this.textureSetup; }
        @Override public @Nullable ScreenRectangle scissorArea() { return null; }
        @Override public @Nullable ScreenRectangle bounds() { return this.bounds; }
    }


}
