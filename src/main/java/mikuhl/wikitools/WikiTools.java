package mikuhl.wikitools;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import mikuhl.wikitools.entity.EntityRenderClone;
import mikuhl.wikitools.helper.FramebufferHelper;
import mikuhl.wikitools.listeners.Listeners;
import mikuhl.wikitools.proxy.CommonProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static jdk.nashorn.internal.objects.Global.Infinity;

@Mod(modid = WikiTools.MODID, version = WikiTools.VERSION)
public class WikiTools {
    public static final String MODID   = "wikitools-hplus";
    public static final String VERSION = "2.6.6";

    private static WikiTools        instance;
    public         WikiToolsConfigs configs;

    public  Listeners        listeners;
    private EntityLivingBase entity        = null;
    public  String           updateMessage = "";

    public WikiTools()
    {
        instance = this;
    }

    @SidedProxy(serverSide = "mikuhl.wikitools.proxy.CommonProxy", clientSide = "mikuhl.wikitools.proxy.ClientProxy")
    public static CommonProxy proxy;

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        proxy.init(event);
        configs = new WikiToolsConfigs();

        try
        {
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet("https://api.github.com/repos/Charzard4261/Wikitools/releases/latest");
            request.addHeader("content-type", "application/json");
            HttpResponse result = httpClient.execute(request);
            String json = EntityUtils.toString(result.getEntity(), "UTF-8");

            JsonElement latest = new JsonParser().parse(json);
            if (checkIfNeedsUpdating(latest.getAsJsonObject().get("tag_name").getAsString()))
            {
                updateMessage = I18n.format("wikitools.needsUpdating") + " " + latest.getAsJsonObject().get("tag_name").getAsString();
            }
        } catch (IOException ex)
        {
        }
    }

    public boolean checkIfNeedsUpdating(String tag)
    {
        tag = tag.replaceAll("[^0-9.]", "");

        boolean needs = false;

        String[] tagIndividual = tag.split("\\.");
        String[] versionIndividual = VERSION.split("\\.");

        for (int i = 0; i < Math.max(tagIndividual.length, versionIndividual.length); i++)
        {
            int tagInt = (tagIndividual.length > i ? Integer.parseInt(tagIndividual[i]) : 0);
            int versionInt = (versionIndividual.length > i ? Integer.parseInt(versionIndividual[i]) : 0);
            if (tagInt > versionInt)
                return true;
            if (tagInt < versionInt)
                return false;
        }

        return false;
    }

    public static WikiTools getInstance()
    {
        return instance;
    }

    public EntityLivingBase getEntity()
    {
        if (entity == null)
            setEntity(new EntityRenderClone(Minecraft.getMinecraft().thePlayer, false));
        return entity;
    }

    private int              customSkinCounter = 0;
    public  ResourceLocation currentCustomSkin;

    public void setEntity(EntityLivingBase entity)
    {
        this.entity = entity;

        // Disable red glow
        entity.hurtTime = 0;

        // Disable name tag
        entity.posX = Double.MAX_VALUE;
        entity.posY = Double.MAX_VALUE;
        entity.posZ = Double.MAX_VALUE;

        // Set rotation
        entity.renderYawOffset = configs.bodyYaw;
        entity.prevRenderYawOffset = configs.bodyYaw;
        entity.rotationYaw = 0;
        // Head rotation pitch
        entity.rotationPitch = configs.headPitch;
        entity.prevRotationPitch = configs.headPitch;
        entity.rotationYawHead = configs.bodyYaw + configs.headYaw;
        entity.prevRotationYawHead = configs.bodyYaw + configs.headYaw;

        if (entity instanceof AbstractClientPlayer)
        {
            if (((AbstractClientPlayer) entity).getSkinType().equalsIgnoreCase("slim"))
                configs.smallArms = true;
            else
                configs.smallArms = false;

            if (((AbstractClientPlayer) entity).hasSkin() || !DefaultPlayerSkin.getDefaultSkin(entity.getUniqueID()).equals(((AbstractClientPlayer) entity).getLocationSkin()))
            {
                ResourceLocation rl = ((AbstractClientPlayer) entity).getLocationSkin();
                ThreadDownloadImageData dat = (ThreadDownloadImageData) Minecraft.getMinecraft().getTextureManager().getTexture(rl);
                BufferedImage bufferedImage = ReflectionHelper.getPrivateValue(ThreadDownloadImageData.class, dat, "bufferedImage", "field_110560_d");

                Graphics2D G2D = bufferedImage.createGraphics();

                for (int x = 0; x < bufferedImage.getWidth(); x++)
                    for (int y = 0; y < bufferedImage.getHeight(); y++)
                    {
                        Color c = new Color(bufferedImage.getRGB(x, y), true);
                        if (0 < c.getAlpha() && c.getAlpha() < 255)
                        {
                            Color corrected = new Color(c.getRed(), c.getGreen(), c.getBlue(), 255);
                            G2D.setColor(corrected);
                            G2D.fill(new Rectangle(x, y, 1, 1));
                        }
                    }
                G2D.dispose();

                DynamicTexture dynTex = new DynamicTexture(bufferedImage);
                currentCustomSkin = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("WikiToolsCustomSkin_" + customSkinCounter, dynTex);
                customSkinCounter++;
            } else
                currentCustomSkin = DefaultPlayerSkin.getDefaultSkin(entity.getUniqueID());
        }
    }

    boolean rendering = false;

    public void render()
    {
        if (rendering) return;

        Minecraft minecraft = Minecraft.getMinecraft();

        rendering = true;
        int displayWidth = minecraft.displayWidth;
        int displayHeight = minecraft.displayHeight;
        int shortest = Math.min(Math.min(displayWidth, displayHeight), 512);

        Framebuffer framebuffer = FramebufferHelper.createFrameBuffer(displayWidth, displayHeight);

        float scale = 1;
        BufferedImage bufferedImage = renderEntity(0, scale, entity, framebuffer);
        int longest = getLongest(bufferedImage);

        if (longest != 0)
        {
            // Some mobs require extremely fine scaling to equal exactly our size.
            // Be ok with 1 pixel smaller if it cant find the exact scale fast enough.
            while (longest != shortest && longest != shortest - 1)
            {
                scale = shortest / (longest / scale);
                if (scale == Infinity) break;
                bufferedImage = renderEntity(shortest, scale, entity, framebuffer);
                longest = getLongest(bufferedImage);
            }

            FramebufferHelper.saveBuffer(bufferedImage);
            FramebufferHelper.restoreFrameBuffer(framebuffer);
        }

        rendering = false;
    }

    private BufferedImage renderEntity(int height, float scale, EntityLivingBase entity, Framebuffer framebuffer)
    {

        FramebufferHelper.clearFrameBuffer();

        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        int posX = resolution.getScaledWidth() / 2;
        int posY = resolution.getScaledHeight() / 2 + (height / resolution.getScaleFactor() / 2);

        FramebufferHelper.drawEntityOnScreen(posX, posY, scale, entity);
        BufferedImage readImage = FramebufferHelper.readImage(framebuffer);
        BufferedImage trimImage = FramebufferHelper.trimImage(readImage);

        return trimImage;
    }

    private int getLongest(BufferedImage image)
    {
        return Math.max(image.getWidth(), image.getHeight());
    }
}
