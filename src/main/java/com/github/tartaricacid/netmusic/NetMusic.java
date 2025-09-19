package com.github.tartaricacid.netmusic;

import com.coloryr.allmusic.client.core.AllMusicBridge;
import com.coloryr.allmusic.client.core.AllMusicCore;
import com.github.tartaricacid.netmusic.api.NetEaseMusic;
import com.github.tartaricacid.netmusic.api.WebApi;
import com.github.tartaricacid.netmusic.proxy.CommonProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

@Mod(modid = NetMusic.MOD_ID, name = NetMusic.NAME, version = NetMusic.VERSION)
public class NetMusic implements AllMusicBridge {
    public static final String MOD_ID = "netmusic";
    public static final String NAME = "Net Music Mod";
    public static final String VERSION = "1.0.1";

    public static Logger LOGGER;
    @SidedProxy(serverSide = "com.github.tartaricacid.netmusic.proxy.CommonProxy",
            clientSide = "com.github.tartaricacid.netmusic.proxy.ClientProxy")
    public static CommonProxy PROXY;
    public static WebApi NET_EASE_WEB_API;
    @Mod.Instance
    public static NetMusic INSTANCE;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        NET_EASE_WEB_API = new NetEaseMusic().getApi();
        PROXY.preInit(event);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        PROXY.init(event);
    }

    @Mod.EventHandler
    public void preload(final FMLPreInitializationEvent evt) {
        if (!evt.getModConfigurationDirectory().exists()) {
            evt.getModConfigurationDirectory().mkdirs();
        }
        AllMusicCore.init(evt.getModConfigurationDirectory().toPath(), this);
        AllMusicCore.glInit();
        MinecraftForge.EVENT_BUS.register(this);
        try {
            Class<?> server = Class.forName("com.coloryr.allmusic.server.AllMusicForge");
            Field m = server.getField("channel");
            FMLEventChannel channel = (FMLEventChannel) m.get(null);
            channel.register(this);
        } catch (Exception e) {
            NetworkRegistry.INSTANCE.newEventDrivenChannel("allmusic:channel")
                    .register(this);
        }
    }


    @Override
    public Object genTexture(int size) {
        return AllMusicCore.genGLTexture(size);
    }

    public int getScreenWidth() {
        ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
        return scaledresolution.getScaledWidth();
    }

    public int getScreenHeight() {
        ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
        return scaledresolution.getScaledHeight();
    }

    public int getTextWidth(String item) {
        return Minecraft.getMinecraft().fontRenderer.getStringWidth(item);
    }

    public int getFontHeight() {
        return Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT;
    }

    @Override
    public void updateTexture(Object tex, int size, ByteBuffer byteBuffer) {
        AllMusicCore.updateGLTexture((int) tex, size, byteBuffer);
    }

    public void drawText(String item, int x, int y, int color, boolean shadow) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        font.drawString(item, x, y, color, shadow);
    }

    public void drawPic(Object textureID, int size, int x, int y, int ang) {
        GlStateManager.bindTexture((int)textureID);
        GlStateManager.enableAlpha();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        int a = size / 2;
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x + a, (float) y + a, 0.0f);

        if (ang > 0) {
            GL11.glRotatef(ang, 0, 0, 1f);
        }

        int x0 = -a;
        int x1 = a;
        int y0 = -a;
        int y1 = a;

        GL11.glBegin(7);
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex3f(x0, y0, 0.0f);
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex3f(x0, y1, 0.0f);
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex3f(x1, y1, 0.0f);
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex3f(x1, y0, 0.0f);
        GL11.glEnd();
        GL11.glPopMatrix();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.enableAlpha();
    }

    public void sendMessage(String data) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            Minecraft.getMinecraft().ingameGUI.getChatGUI().addToSentMessages(data);
        });
    }

    public float getVolume() {
        return Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.RECORDS);
    }

    @Override
    public void stopPlayMusic() {
        Minecraft.getMinecraft().getSoundHandler().stop("", SoundCategory.MUSIC);
        Minecraft.getMinecraft().getSoundHandler().stop("", SoundCategory.RECORDS);
    }

    @SubscribeEvent
    public void onSound(final PlaySoundEvent e) {
        if (!AllMusicCore.isPlay()) return;
        SoundCategory data = e.getSound().getCategory();
        switch (data) {
            case MUSIC:
            case RECORDS:
                e.setResultSound(null);
        }
    }

    @SubscribeEvent
    public void onServerQuit(final FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        AllMusicCore.onServerQuit();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            AllMusicCore.tick();
        }
    }
}
