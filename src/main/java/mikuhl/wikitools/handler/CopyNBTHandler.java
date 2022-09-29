package mikuhl.wikitools.handler;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import mikuhl.wikitools.WikiToolsKeybinds;
import mikuhl.wikitools.helper.ClipboardHelper;
import mikuhl.wikitools.helper.FramebufferHelper;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.MapItemRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapData;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;

public class CopyNBTHandler {

    @SubscribeEvent
    public void onKeyInputEvent(InputEvent.KeyInputEvent event) {

        if (!WikiToolsKeybinds.COPY_NBT.isKeyDown()) return;

        Minecraft minecraft = Minecraft.getMinecraft();
        MovingObjectPosition objectMouseOver = minecraft.objectMouseOver;
        Entity entity = objectMouseOver.entityHit;

        if (entity != null) {
            if (entity instanceof EntityOtherPlayerMP) {
                NBTTagCompound nbt = new NBTTagCompound();
                entity.writeToNBT(nbt);
                // Code snippet from: https://github.com/cow-mc/Cowlection/blob/106168f533c627fef4f30ba1fece76c965778924/src/main/java/de/cowtipper/cowlection/command/MooCommand.java#L890
                EntityOtherPlayerMP otherPlayer = (EntityOtherPlayerMP) entity;
                if (otherPlayer.hasCustomName()) {
                    nbt.setString("__customName", otherPlayer.getCustomNameTag());
                }
                GameProfile gameProfile = otherPlayer.getGameProfile();
                for (Property property : gameProfile.getProperties().get("textures")) {
                    nbt.setString("_skin", property.getValue());
                }
                ClipboardHelper.setClipboard(nbt);
                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(I18n.format("wikitools.message.copiedNBT")));
            }
            else {
                if (entity instanceof EntityItemFrame) {
                    EntityItemFrame itemFrame = (EntityItemFrame) entity;
                    if (itemFrame.getDisplayedItem().getItem() instanceof ItemMap) {
                        ItemMap map = (ItemMap) itemFrame.getDisplayedItem().getItem();
                        MapStorage mapStorage = Minecraft.getMinecraft().theWorld.getMapStorage();
                        Map<String, WorldSavedData> worldSavedDataMap = ReflectionHelper.getPrivateValue(MapStorage.class, mapStorage, "loadedDataMap", "field_75749_b");
                        for (String st : worldSavedDataMap.keySet()) {
                            if (st.startsWith("map_")) {
                                Minecraft.getMinecraft().entityRenderer.getMapItemRenderer();
                                MapData md = (MapData) worldSavedDataMap.get(st);
                                BufferedImage icon = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
                                Graphics2D G2D = icon.createGraphics();

                                MapData mapData = md;
                                DynamicTexture mapTexture = new DynamicTexture(128, 128);
                                int[] mapTextureData = mapTexture.getTextureData();

                                for (int i = 0; i < mapTextureData.length; ++i) {
                                    mapTextureData[i] = 0;
                                }
                                for (int i = 0; i < 16384; ++i) {
                                    int j = mapData.colors[i] & 255;

                                    if (j / 4 == 0) {
                                        mapTextureData[i] = (i + i / 128 & 1) * 8 + 16 << 24;
                                    }
                                    else {
                                        mapTextureData[i] = MapColor.mapColorArray[j / 4].func_151643_b(j & 3);
                                    }
                                }

                                mapTexture.updateDynamicTexture();
                                //G2D.drawBytes(md.colors, 0, mapTexture.getTextureData().length, 0,0);
                                G2D.dispose();
                                FramebufferHelper.saveBuffer(icon);
                            }
                        }
                    }
                }
                ClipboardHelper.setClipboard(entity.serializeNBT());
                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(I18n.format("wikitools.message.copiedNBT")));
            }
        }
        else {
            BlockPos pos = objectMouseOver.getBlockPos();
            TileEntity tile = minecraft.theWorld.getTileEntity(pos);
            if (tile != null) {
                ClipboardHelper.setClipboard(tile.serializeNBT());
                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(I18n.format("wikitools.message.copiedNBT")));
            }
        }
    }

    @SubscribeEvent()
    public void onKeyboardInputEvent(GuiScreenEvent.KeyboardInputEvent event) {
        // Keybinds don't register in GUI's
        if (!Keyboard.isKeyDown(WikiToolsKeybinds.COPY_NBT.getKeyCode())) return;
        if (event.gui instanceof GuiContainer) {
            Slot slot = ((GuiContainer) event.gui).getSlotUnderMouse();
            if (slot == null) return;
            if (!slot.getHasStack()) return;
            ClipboardHelper.setClipboard(slot.getStack().serializeNBT());
            Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(I18n.format("wikitools.message.copiedNBT")));
        }
    }
}
