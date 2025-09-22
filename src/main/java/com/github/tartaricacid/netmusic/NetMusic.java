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
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
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
        // 注册TOP兼容性
        FMLInterModComms.sendFunctionMessage("theoneprobe", "getTheOneProbe", "com.github.tartaricacid.netmusic.compat.TOPCompat");
    }

    @Mod.EventHandler
    public void preload(final FMLPreInitializationEvent evt) {
        AllMusicCore.init(evt.getModConfigurationDirectory().toPath(), this);
        MinecraftForge.EVENT_BUS.register(this);
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
    @SideOnly(Side.CLIENT)
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
    @SideOnly(Side.CLIENT)
    public void onServerQuit(final FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        AllMusicCore.onServerQuit();
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            AllMusicCore.tick();
        }
    }
}
