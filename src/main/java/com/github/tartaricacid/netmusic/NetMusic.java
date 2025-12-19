package com.github.tartaricacid.netmusic;

import com.coloryr.allmusic.client.core.AllMusicBridge;
import com.coloryr.allmusic.client.core.AllMusicCore;
import com.coloryr.allmusic.client.core.HttpClientUtil;
import com.coloryr.allmusic.client.core.objs.CookieObj;
import com.github.tartaricacid.netmusic.api.NetEaseMusic;
import com.github.tartaricacid.netmusic.api.WebApi;
import com.github.tartaricacid.netmusic.config.GeneralConfig;
import com.github.tartaricacid.netmusic.init.InitBlocks;
import com.github.tartaricacid.netmusic.init.InitContainer;
import com.github.tartaricacid.netmusic.init.InitItems;
import com.github.tartaricacid.netmusic.init.InitSounds;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.sound.SoundEngineLoadEvent;
import net.minecraftforge.client.event.sound.SoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import okhttp3.Cookie;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Mod(NetMusic.MOD_ID)
public class NetMusic implements AllMusicBridge {
    public static final String MOD_ID = "netmusic";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static WebApi NET_EASE_WEB_API;

    public static CookieObj cookie;
    private static File configDir;

    public NetMusic() {
        NET_EASE_WEB_API = new NetEaseMusic().getApi();
        InitBlocks.BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        InitBlocks.TILE_ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());
        InitItems.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        InitSounds.SOUND_EVENTS.register(FMLJavaModLoadingContext.get().getModEventBus());
        InitContainer.CONTAINER_TYPE.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, GeneralConfig.init());

        MinecraftForge.EVENT_BUS.register(this);

        configDir = FMLPaths.CONFIGDIR.get().toFile();
        loadRawCookie();
        loadConfig();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ()->this::InitAllMusicCore);
        HttpClientUtil.init();
    }

    public static void loadConfig() {
        try {
            File cookieFile = new File(configDir, "netmusic_cookie.json");

            InputStreamReader reader = new InputStreamReader(Files.newInputStream(cookieFile.toPath()),
                    StandardCharsets.UTF_8);
            BufferedReader bf = new BufferedReader(reader);
            cookie = new Gson().fromJson(bf, CookieObj.class);
            bf.close();
            reader.close();
            if (cookie == null || cookie.cookieStore == null) {
                cookie = new CookieObj();
                saveCookie();
            }
        } catch (Exception e) {
            // log.warning("§d[AllMusic3]§c读取配置文件错误");
            e.printStackTrace();
        }
    }

    public static void loadRawCookie() {
        File cookieFile = new File(configDir, "netmusic_raw_cookie.txt");
        String cookieStr = "";
        try {
            InputStreamReader reader = new InputStreamReader(Files.newInputStream(cookieFile.toPath()),
                    StandardCharsets.UTF_8);
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
                    if (list1.containsKey(cookieitem[0].trim())) {
                        continue;
                    }
                    list1.put(cookieitem[0].trim(), new Cookie.Builder()
                            .name(cookieitem[0].trim())
                            .domain("163.com")
                            .expiresAt(Long.MAX_VALUE)
                            .build());
                } else {
                    list1.put(cookieitem[0].trim(), new Cookie.Builder()
                            .name(cookieitem[0].trim())
                            .value(cookieitem[1].trim())
                            .domain("163.com")
                            .expiresAt(Long.MAX_VALUE)
                            .build());
                }
            }
            cookie = new CookieObj();
            cookie.cookieStore.put("music.163.com", new ArrayList<>(list1.values()));
            saveCookie();

            // clear raw cookie file content
            try {
                FileOutputStream out = new FileOutputStream(cookieFile);
                OutputStreamWriter write = new OutputStreamWriter(
                        out, StandardCharsets.UTF_8);
                write.write("");
                write.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
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
            // log.warning("§d[AllMusic3]§c配置文件保存错误");
            e.printStackTrace();
        }
    }

    @Override
    public void sendMessage(String data) {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal(data));
        });
    }

    @Override
    public float getVolume() {
        return Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.RECORDS);
    }

    @Override
    public void stopPlayMusic() {
        Minecraft.getInstance().getSoundManager().stop(null, SoundSource.MUSIC);
        Minecraft.getInstance().getSoundManager().stop(null, SoundSource.RECORDS);
    }

    @SubscribeEvent
    public void onSound(final SoundEvent.SoundSourceEvent e) {
        if (!AllMusicCore.isPlay()) return;
        SoundSource data = e.getSound().getSource();
        switch (data) {
            case MUSIC, RECORDS -> e.getChannel().stop();
        }
    }

    @SubscribeEvent
    public void onServerQuit(final ClientPlayerNetworkEvent.LoggingOut e) {
        AllMusicCore.onServerQuit();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event){
        AllMusicCore.tick();
    }

    private void InitAllMusicCore() {
        AllMusicCore.init(FMLPaths.CONFIGDIR.get(), this);
    }
}