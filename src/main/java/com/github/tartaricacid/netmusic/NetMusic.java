package com.github.tartaricacid.netmusic;

import com.coloryr.allmusic.client.core.AllMusicBridge;
import com.coloryr.allmusic.client.core.AllMusicCore;
import com.coloryr.allmusic.client.core.HttpClientUtil;
import com.coloryr.allmusic.client.core.objs.CookieObj;
import com.github.tartaricacid.netmusic.api.NetEaseMusic;
import com.github.tartaricacid.netmusic.api.WebApi;
import com.github.tartaricacid.netmusic.proxy.CommonProxy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import okhttp3.Cookie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION)
public class NetMusic implements AllMusicBridge {
    public static CookieObj cookie;
    private static File configDir;

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);
    @SidedProxy(serverSide = "com.github.tartaricacid.netmusic.proxy.CommonProxy",
            clientSide = "com.github.tartaricacid.netmusic.proxy.ClientProxy")
    public static CommonProxy PROXY;
    public static WebApi NET_EASE_WEB_API;
    @Mod.Instance
    public static NetMusic INSTANCE;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
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
        configDir = evt.getModConfigurationDirectory();
        loadRawCookie();
        loadConfig();
        AllMusicCore.init(configDir.toPath(), this);
        MinecraftForge.EVENT_BUS.register(this);
        HttpClientUtil.init();
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

    private static void loadConfig(){
        try{
            File cookieFile = new File(configDir, "netmusic_cookie.json");

            InputStreamReader reader = new InputStreamReader(Files.newInputStream(cookieFile.toPath()), StandardCharsets.UTF_8);
            BufferedReader bf = new BufferedReader(reader);
            cookie = new Gson().fromJson(bf, CookieObj.class);
            bf.close();
            reader.close();
            if (cookie == null || cookie.cookieStore == null) {
                cookie = new CookieObj();
                saveCookie();
            }
        }catch (Exception e) {
            //log.warning("§d[AllMusic3]§c读取配置文件错误");
            e.printStackTrace();
        }
    }

    private static void loadRawCookie(){
        File cookieFile = new File(configDir, "netmusic_raw_cookie.txt");
        String cookieStr = "";
        try {
            InputStreamReader reader = new InputStreamReader(Files.newInputStream(cookieFile.toPath()), StandardCharsets.UTF_8);
            BufferedReader bf = new BufferedReader(reader);
            String line;
            while ((line = bf.readLine()) != null) {
                cookieStr += line;
            }
            bf.close();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!cookieStr.isEmpty()) {
            String[] cookies = cookieStr.split(";");
            Map<String, Cookie> list1 = new HashMap<>();
            for (String item : cookies) {
                String[] cookieitem = item.split("=");
                if (cookieitem.length == 1) {
                    if (list1.containsKey(cookieitem[0])) {
                        continue;
                    }
                    list1.put(cookieitem[0], new Cookie.Builder()
                            .name(cookieitem[0])
                            .domain("163.com")
                            .expiresAt(Long.MAX_VALUE)
                            .build());
                } else {
                    list1.put(cookieitem[0], new Cookie.Builder()
                            .name(cookieitem[0])
                            .value(cookieitem[1])
                            .domain("163.com")
                            .expiresAt(Long.MAX_VALUE)
                            .build());
                }
            }
            cookie = new CookieObj();
            cookie.cookieStore.put("music.163.com", new ArrayList<>(list1.values()));
            saveCookie();
        }
    }

    public static void saveCookie() {
        try {
            File cookieFile = new File(configDir, "netmusic_cookie.json");
            String data = new GsonBuilder().setPrettyPrinting().create().toJson(cookie);
            FileOutputStream out = new FileOutputStream(cookieFile);
            OutputStreamWriter write = new OutputStreamWriter(
                    out, StandardCharsets.UTF_8);
            write.write(data);
            write.close();
        } catch (Exception e) {
            //log.warning("§d[AllMusic3]§c配置文件保存错误");
            e.printStackTrace();
        }
    }
}
