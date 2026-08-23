/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.screen.CartographyTableScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class CartographyTableScreen
extends HandledScreen<CartographyTableScreenHandler> {
    private static final Identifier TEXTURE = new Identifier("textures/gui/container/cartography_table.png");

    public CartographyTableScreen(CartographyTableScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.titleY -= 2;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        MapState mapState;
        Integer integer;
        this.renderBackground(context);
        int i = this.x;
        int j = this.y;
        context.drawTexture(TEXTURE, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight);
        ItemStack itemStack = ((CartographyTableScreenHandler)this.handler).getSlot(1).getStack();
        boolean bl = itemStack.isOf(Items.MAP);
        boolean bl2 = itemStack.isOf(Items.PAPER);
        boolean bl3 = itemStack.isOf(Items.GLASS_PANE);
        ItemStack itemStack2 = ((CartographyTableScreenHandler)this.handler).getSlot(0).getStack();
        boolean bl4 = false;
        if (itemStack2.isOf(Items.FILLED_MAP)) {
            integer = FilledMapItem.getMapId(itemStack2);
            mapState = FilledMapItem.getMapState(integer, (World)this.client.world);
            if (mapState != null) {
                if (mapState.locked) {
                    bl4 = true;
                    if (bl2 || bl3) {
                        context.drawTexture(TEXTURE, i + 35, j + 31, this.backgroundWidth + 50, 132, 28, 21);
                    }
                }
                if (bl2 && mapState.scale >= 4) {
                    bl4 = true;
                    context.drawTexture(TEXTURE, i + 35, j + 31, this.backgroundWidth + 50, 132, 28, 21);
                }
            }
        } else {
            integer = null;
            mapState = null;
        }
        this.drawMap(context, integer, mapState, bl, bl2, bl3, bl4);
    }

    private void drawMap(DrawContext context, @Nullable Integer mapId, @Nullable MapState mapState, boolean cloneMode, boolean expandMode, boolean lockMode, boolean cannotExpand) {
        int i = this.x;
        int j = this.y;
        if (expandMode && !cannotExpand) {
            context.drawTexture(TEXTURE, i + 67, j + 13, this.backgroundWidth, 66, 66, 66);
            this.drawMap(context, mapId, mapState, i + 85, j + 31, 0.226f);
        } else if (cloneMode) {
            context.drawTexture(TEXTURE, i + 67 + 16, j + 13, this.backgroundWidth, 132, 50, 66);
            this.drawMap(context, mapId, mapState, i + 86, j + 16, 0.34f);
            context.getMatrices().push();
            context.getMatrices().translate(0.0f, 0.0f, 1.0f);
            context.drawTexture(TEXTURE, i + 67, j + 13 + 16, this.backgroundWidth, 132, 50, 66);
            this.drawMap(context, mapId, mapState, i + 70, j + 32, 0.34f);
            context.getMatrices().pop();
        } else if (lockMode) {
            context.drawTexture(TEXTURE, i + 67, j + 13, this.backgroundWidth, 0, 66, 66);
            this.drawMap(context, mapId, mapState, i + 71, j + 17, 0.45f);
            context.getMatrices().push();
            context.getMatrices().translate(0.0f, 0.0f, 1.0f);
            context.drawTexture(TEXTURE, i + 66, j + 12, 0, this.backgroundHeight, 66, 66);
            context.getMatrices().pop();
        } else {
            context.drawTexture(TEXTURE, i + 67, j + 13, this.backgroundWidth, 0, 66, 66);
            this.drawMap(context, mapId, mapState, i + 71, j + 17, 0.45f);
        }
    }

    private void drawMap(DrawContext context, @Nullable Integer mapId, @Nullable MapState mapState, int x, int y, float scale) {
        if (mapId != null && mapState != null) {
            context.getMatrices().push();
            context.getMatrices().translate(x, y, 1.0f);
            context.getMatrices().scale(scale, scale, 1.0f);
            this.client.gameRenderer.getMapRenderer().draw(context.getMatrices(), context.getVertexConsumers(), mapId, mapState, true, LightmapTextureManager.MAX_LIGHT_COORDINATE);
            context.draw();
            context.getMatrices().pop();
        }
    }
}

