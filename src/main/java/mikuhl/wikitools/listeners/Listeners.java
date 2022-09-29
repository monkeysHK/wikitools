package mikuhl.wikitools.listeners;

import java.util.ArrayList;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import mikuhl.wikitools.WikiTools;
import mikuhl.wikitools.WikiToolsKeybinds;
import mikuhl.wikitools.entity.RenderPlayerOverride;
import mikuhl.wikitools.gui.WTGuiScreen;
import mikuhl.wikitools.helper.ClipboardHelper;
import net.minecraft.block.BlockSkull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.event.ClickEvent;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.codec.binary.Base64;
import org.lwjgl.input.Keyboard;

public class Listeners {
    public boolean openUI = false;
    // colors used on wiki
    private final String[] glassPaneColors = {
            "White",
            "Orange",
            "Magenta",
            "Light Blue",
            "Yellow",
            "Lime",
            "Pink",
            "Gray",
            "Light Gray",
            "Cyan",
            "Purple",
            "Blue",
            "Brown",
            "Green",
            "Red",
            "Black"
    };
    private final String[] skullNames = {
            "Skeleton Skull",
            "Wither Skeleton Skull",
            "Zombie Head",
            "Player Head",
            "Creeper Head"
    };
    private final String[] leatherArmorPieces = {
            "Helmet",
            "Chestplate",
            "Leggings",
            "Boots"
    };

    @SubscribeEvent()
    public void onRender(TickEvent.RenderTickEvent e)
    {
        if (openUI)
            Minecraft.getMinecraft().displayGuiScreen(new WTGuiScreen());
        openUI = false;
    }

    @SubscribeEvent
    public void copySkullIDHandler(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!Keyboard.getEventKeyState() || !(WikiToolsKeybinds.COPY_SKULL_ID.getKeyCode() >= 0 && Keyboard.isKeyDown(WikiToolsKeybinds.COPY_SKULL_ID.getKeyCode())))
            return;

        if (event.gui instanceof GuiContainer) {
            GuiContainer guiContainer = (GuiContainer) event.gui;
            if (guiContainer.getSlotUnderMouse() == null)
                return;

            ItemStack is = guiContainer.getSlotUnderMouse().getStack();
            if (is == null ||
                    !(is.getItem() instanceof ItemSkull) ||
                    !is.hasTagCompound() ||
                    !is.getTagCompound().hasKey("SkullOwner"))
                return;
            String base64 = is.getTagCompound().getCompoundTag("SkullOwner").getCompoundTag("Properties").getTagList("textures", 10).getCompoundTagAt(0).getString("Value");
            JsonElement decoded = new JsonParser().parse(new String(Base64.decodeBase64(base64)));
            String skullID = decoded.getAsJsonObject().getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString().split("/")[4];

            ClipboardHelper.setClipboard(skullID);

            Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(I18n.format("wikitools.message.copiedSkullID") + " " + skullID));
        }
    }

    @SubscribeEvent
    public void copyWikiTooltipHandler(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!Keyboard.getEventKeyState() || !(WikiToolsKeybinds.COPY_WIKI_TOOLTIP.getKeyCode() >= 0 && Keyboard.isKeyDown(WikiToolsKeybinds.COPY_WIKI_TOOLTIP.getKeyCode())))
            return;

        // Used for "copy for module" mode
        boolean copyformodule = Keyboard.isKeyDown(Minecraft.getMinecraft().gameSettings.keyBindSneak.getKeyCode());

        if (event.gui instanceof GuiContainer) {
            GuiContainer guiContainer = (GuiContainer) event.gui;
            if (guiContainer.getSlotUnderMouse() == null)
                return;

            ItemStack is = guiContainer.getSlotUnderMouse().getStack();
            if (is == null)
                return;
            String ID = sanitiseAll(is.getDisplayName(), true, !copyformodule);
            String name = getName(is);
            String title = sanitiseAll(is.getDisplayName(), false, !copyformodule);
            String text = getLore(is, "\\\\n", false, copyformodule);
            boolean ench = is.hasEffect();

            if (copyformodule) {
                ClipboardHelper.setClipboard("['" + ID + "'] = { " +
                    "name = '" + name + "', " +
                    (ench ? "ench = true, " : "") +
                    "title = '" + title + "', " +
                    (!text.equals("") ? "text = '" + text + "', " : "") +
                    "},");
            }
            else {
                ClipboardHelper.setClipboard(
                    "item{" + name + "} " +
                    (ench ? "ench{true} " : "") +
                    "title{" + title + "} " +
                    (!text.equals("") ? "text{" + text + "} " : ""));
            }

            Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(
                    I18n.format(copyformodule ? "wikitools.message.copiedTooltipCode" : "wikitools.message.copiedTooltip")));
        }
    }

    @SubscribeEvent
    public void copyWikiUIHandler(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!Keyboard.getEventKeyState() || !(WikiToolsKeybinds.COPY_WIKI_UI.getKeyCode() >= 0 && Keyboard.isKeyDown(WikiToolsKeybinds.COPY_WIKI_UI.getKeyCode())))
            return;

        // Used for "filled" mode
        boolean filled = Keyboard.isKeyDown(Minecraft.getMinecraft().gameSettings.keyBindSneak.getKeyCode());

        if (event.gui instanceof GuiContainer) {
            Minecraft mc = Minecraft.getMinecraft();

            if (mc.thePlayer.openContainer instanceof ContainerChest) {
                ContainerChest chest = (ContainerChest) mc.thePlayer.openContainer;
                ArrayList<String> lines = new ArrayList<String>();
                int size = chest.getLowerChestInventory().getSizeInventory();
                int rowsize = size / 9;
                lines.add("{{LargeChest");
                lines.add("|header=" + sanitiseColors(chest.getLowerChestInventory().getName(), true));
                if (filled)
                    lines.add("|fillmode=filled");
                if (rowsize != 6)
                    lines.add("|rows=" + rowsize);

                for (int i = 0; i < size; i++) {
                    int cellX = (i / 9) + 1, cellY = (i % 9) + 1;
                    String cellpos = "" + (char)(cellX + 'A' - 1) + cellY;
                    String item, title, text;
                    int count = 0;
                    boolean ench;

                    // If the current slot is empty
                    if (!chest.getSlot(i).getHasStack()) {
                        if (filled)
                            lines.add("|" + cellpos + "=item{}");
                        continue;
                    }

                    // Extract to variables
                    ItemStack stack = chest.getSlot(i).getStack();

                    // Handle Custom UI Items
                    if (stack.hasDisplayName()) {
                        if (stack.getUnlocalizedName().contains("tile.thinStainedGlass")
                                && stack.getDisplayName().equalsIgnoreCase(" ")) {
                            // handle border glass panes
                            String color = glassPaneColors[stack.getItemDamage()];
                            if (!color.equals("Black"))
                                lines.add("|" + cellpos + "=Blank (" + color + ")");
                            else if (!filled)
                                lines.add("|" + cellpos + "=Blank");
                            continue;
                        }
                    }

                    // Handle Other UI Items
                    // parse item name
                    item = getName(stack);
                    ench = stack.hasEffect();

                    // parse stack size
                    if (stack.stackSize > 1)
                        count = stack.stackSize;

                    // parse lore title
                    title = sanitiseAll(stack.getDisplayName(), false, true);

                    // parse lore text
                    text = getLore(stack, "\\n", false, true);

                    // add line to ui string
                    lines.add(chain(cellpos, item, ench, count, title, text));
                }

                lines.add("}}");

                ClipboardHelper.setClipboard(String.join("\n", lines));

                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(
                        I18n.format("wikitools.message.copiedUI")
                        + (filled ? " " + I18n.format("wikitools.message.UIfilled") : "")
                ));
            }
        }
    }

    @SubscribeEvent
    public void copyEntityHandler(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!Keyboard.getEventKeyState() || !(WikiToolsKeybinds.COPY_ENTITY.getKeyCode() >= 0 && Keyboard.isKeyDown(WikiToolsKeybinds.COPY_ENTITY.getKeyCode())))
            return;

        if (event.gui instanceof GuiContainer)
        {
            boolean copyashead = Keyboard.isKeyDown(Minecraft.getMinecraft().gameSettings.keyBindSneak.getKeyCode());

            GuiContainer guiContainer = (GuiContainer) event.gui;
            if (guiContainer.getSlotUnderMouse() == null)
                return;

            ItemStack is = guiContainer.getSlotUnderMouse().getStack();
            if (is == null)
                return;

            if (copyashead && (is.getItem() instanceof ItemBlock || is.getItem() instanceof ItemSkull))
                // Copy the entity as head
                WikiTools.getInstance().getEntity().replaceItemInInventory(103, is);
            else if (is.getItem() instanceof ItemArmor)
                // Copy the entity as armor piece
                WikiTools.getInstance().getEntity().replaceItemInInventory(100 + (3 - ((ItemArmor) is.getItem()).armorType), is);
            else
                // Copy the entity as held item
                WikiTools.getInstance().getEntity().setCurrentItemOrArmor(0, is);

            Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(I18n.format("wikitools.message.addedItem")));
        }
    }

    public String sanitiseAll(String text, boolean deleteColors, boolean asWikitext) {
        text = text.replaceAll("\\\\", "\\\\\\\\")
                    .replaceAll("&", "\\\\&");
        text = sanitiseColors(text, deleteColors);

        if (asWikitext) {
            // UI-specific handlers
            text = text.replaceAll(";", "\\\\\\;")
                    .replaceAll("\\{", "\\\\{")
                    .replaceAll("}", "\\\\}")
                    .replaceAll("\\|", "{{!}}")
                    .replaceAll("=", "{{=}}");
        } else {
            // backslash handler when used in module context, e.g. for tooltip line
            // double backslash is read as single backslash in module
            text = text.replaceAll("\\\\", "\\\\\\\\");
            // then, handle single quotation marks
            text = text.replaceAll("'", "\\\\'");
        }

        return text;
    }
    public String sanitiseColors(String text, boolean deleteColors) {
        // If not delete, replace all colour codes (§) with &
        if (!deleteColors)
            text = text.replaceAll("\u00A7", "&");
        else
            text = text.replaceAll("\u00A7.", "");
        return text;
    }
    public String getName(ItemStack stack) {
        String name;
        if (stack.getItem() instanceof ItemSkull) {
            if (stack.hasTagCompound() &&
                    stack.getTagCompound().hasKey("SkullOwner")) {
                // if item is a custom skull, use displayed name
                name = sanitiseColors(stack.getDisplayName(), true);
            }
            else {
                // if item is a vanilla skull, get vanilla skull name
                name = skullNames[stack.getItemDamage()];
            }
        }
        else if (stack.getItem() instanceof ItemArmor &&
                stack.hasTagCompound() &&
                stack.getTagCompound().hasKey("display") &&
                stack.getTagCompound().getCompoundTag("display").hasKey("color")) {
            int col = stack.getTagCompound().getCompoundTag("display").getInteger("color");
            int type = ((ItemArmor)stack.getItem()).armorType;
            name = String.format("Leather.%s.%06X", leatherArmorPieces[type], (0xFFFFFF & col));
        }
        else {
            // if item is not a skull, use Minecraft item name
            name = stack.getItem().getItemStackDisplayName(stack);
        }
        return name;
    }
    public String getLore(ItemStack stack, String delimiter, boolean deleteColors, boolean asWikitext) {
        if (stack.hasTagCompound() &&
                stack.getTagCompound().hasKey("display") &&
                stack.getTagCompound().getCompoundTag("display").hasKey("Lore")) {
            NBTTagList lore = stack.getTagCompound().getCompoundTag("display").getTagList("Lore", 8);
            ArrayList<String> lore_lines = new ArrayList<String>();
            for (int l = 0; l < lore.tagCount(); l++) {
                lore_lines.add(sanitiseAll(lore.getStringTagAt(l), deleteColors, asWikitext));
            }
            return String.join(delimiter, lore_lines);
        }
        return "";
    }
    public String chain(String cellpos, String item, boolean ench, int count, String title, String text) {
        return "|" + cellpos + "=item{" + item + "}"
                + (ench ? " ench{true}" : "")
                + (count > 1 ? " num{" + count + "}" : "")
                + " link{none}"
                + (!title.equals("") ? " title{" + title + "}" : "")
                + (!text.equals("") ? " text{" + text + "}" : "");
    }

    /**
     * Called when the Player hovers over an Item
     *
     * @param event Item Tooltip Event
     */
    @SubscribeEvent
    public void checkForTooltips(ItemTooltipEvent event)
    {
        if (Minecraft.getMinecraft().gameSettings.advancedItemTooltips)
        {
            ItemStack is = event.itemStack;
            if (is == null ||
                    !is.hasTagCompound() ||
                    !is.getTagCompound().hasKey("ExtraAttributes") ||
                    !is.getTagCompound().getCompoundTag("ExtraAttributes").hasKey("id"))
                return;
            String id = is.getTagCompound().getCompoundTag("ExtraAttributes").getString("id");
            event.toolTip.add("Skyblock ID: " + id);
        }
    }

    /**
     * Keybinds in this function only apply when NOT in a GUI
     *
     * @param event Client Tick Event
     */
    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START)
            return;

        if (WikiToolsKeybinds.COPY_SKULL_ID.isPressed())
        {
            Minecraft minecraft = Minecraft.getMinecraft();
            MovingObjectPosition objectMouseOver = minecraft.objectMouseOver;
            Entity entity = objectMouseOver.entityHit;

            if (entity != null)
            {
                if (entity instanceof EntityLivingBase)
                {
                    NBTTagCompound nbt = new NBTTagCompound();
                    entity.writeToNBT(nbt);

                    if (!nbt.hasKey("Equipment") ||
                            !nbt.getTagList("Equipment", 10).getCompoundTagAt(4).getCompoundTag("tag").hasKey("SkullOwner"))
                        return;
                    String base64 = nbt.getTagList("Equipment", 10).getCompoundTagAt(4).getCompoundTag("tag").getCompoundTag("SkullOwner").getCompoundTag("Properties").getTagList("textures", 10).getCompoundTagAt(0).getString("Value");
                    JsonElement decoded = new JsonParser().parse(new String(Base64.decodeBase64(base64)));
                    String skullID = decoded.getAsJsonObject().getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString().split("/")[4];

                    ClipboardHelper.setClipboard(skullID);

                    Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(I18n.format("wikitools.message.copiedSkullID") + " " + skullID));
                }
            } else
            {
                BlockPos pos = objectMouseOver.getBlockPos();
                TileEntity tile = minecraft.theWorld.getTileEntity(pos);
                if (tile != null && tile.getBlockType() instanceof BlockSkull)
                {
                    if (!tile.serializeNBT().hasKey("Owner"))
                        return;
                    String base64 = tile.serializeNBT().getCompoundTag("Owner").getCompoundTag("Properties").getTagList("textures", 10).getCompoundTagAt(0).getString("Value");
                    JsonElement decoded = new JsonParser().parse(new String(Base64.decodeBase64(base64)));
                    String skullID = decoded.getAsJsonObject().getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString().split("/")[4];

                    ClipboardHelper.setClipboard(skullID);

                    Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(I18n.format("wikitools.message.copiedSkullID") + " " + skullID));
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event)
    {
        if (event.entity == Minecraft.getMinecraft().thePlayer && !WikiTools.getInstance().updateMessage.isEmpty())
        {
            IChatComponent ichatcomponent = new ChatComponentText(WikiTools.getInstance().updateMessage);
            ichatcomponent.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/Charzard4261/wikitools/releases/latest"));
            ichatcomponent.getChatStyle().setUnderlined(true);
            Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(ichatcomponent);
            WikiTools.getInstance().updateMessage = "";
        }
    }

    @SubscribeEvent
    public void onRenderLiving(RenderPlayerEvent.Pre event)
    {
        if (event.entity == WikiTools.getInstance().getEntity()
                && WikiTools.getInstance().getEntity() instanceof AbstractClientPlayer
                && !(event.renderer instanceof RenderPlayerOverride))
        {
            event.setCanceled(true);
            RenderPlayerOverride re = new RenderPlayerOverride(Minecraft.getMinecraft().getRenderManager(), WikiTools.getInstance().configs.smallArms);
            re.doRender((AbstractClientPlayer) event.entity, event.x, event.y, event.z, WikiTools.getInstance().configs.bodyYaw, 0);
        }
    }

}
