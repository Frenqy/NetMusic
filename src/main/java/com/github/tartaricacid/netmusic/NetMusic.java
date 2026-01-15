package com.github.tartaricacid.netmusic;

import com.coloryr.allmusic.client.core.AllMusicBridge;
import com.coloryr.allmusic.client.core.AllMusicCore;
import com.coloryr.allmusic.server.core.music.api.HttpClientUtil;
import com.coloryr.allmusic.server.core.objs.CookieObj;
import com.github.tartaricacid.netmusic.api.NetEaseMusic;
import com.github.tartaricacid.netmusic.api.WebApi;
import com.github.tartaricacid.netmusic.config.GeneralConfig;
import com.github.tartaricacid.netmusic.init.*;
import com.github.tartaricacid.netmusic.network.NetworkHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod(NetMusic.MOD_ID)
public class NetMusic implements AllMusicBridge {
    public static final String MOD_ID = "netmusic";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final Gson gson = new Gson();
    public static WebApi NET_EASE_WEB_API;

    public static List<CookieObj> cookie;
    private static File configDir;

    public NetMusic(IEventBus modEventBus, ModContainer modContainer) {
        NET_EASE_WEB_API = new NetEaseMusic().getApi();
        InitBlocks.BLOCKS.register(modEventBus);
        InitBlocks.TILE_ENTITIES.register(modEventBus);
        InitItems.ITEMS.register(modEventBus);
        InitItems.TABS.register(modEventBus);
        InitSounds.SOUND_EVENTS.register(modEventBus);
        InitContainer.CONTAINER_TYPE.register(modEventBus);
        InitDataComponent.DATA_COMPONENTS.register(modEventBus);

        modEventBus.addListener(NetworkHandler::registerPacket);
        modEventBus.addListener(InitCapabilities::registerGenericItemHandlers);

        modContainer.registerConfig(ModConfig.Type.COMMON, GeneralConfig.init());

        NeoForge.EVENT_BUS.register(this);

        configDir = FMLPaths.CONFIGDIR.get().toFile();
        loadConfig();

        InitAllMusicCore();
        HttpClientUtil.init();
    }

    public static void loadConfig() {
        try {
            File cookieFile = new File(configDir, "netmusic_cookie.json");

            InputStreamReader reader = new InputStreamReader(Files.newInputStream(cookieFile.toPath()),
                    StandardCharsets.UTF_8);
            BufferedReader bf = new BufferedReader(reader);
            Type listType = new TypeToken<ArrayList<CookieObj>>(){}.getType();
            cookie = new Gson().fromJson(bf, listType);
            bf.close();
            reader.close();
            if (cookie == null) {
                cookie = new ArrayList<>();
                saveCookie();
            }
        } catch (IOException e) {
            LOGGER.warn("§d[AllMusic3]§c未找到配置文件，已生成默认配置文件");
            cookie = new ArrayList<>();
            saveCookie();
        } catch (Exception e) {
            LOGGER.warn("§d[AllMusic3]§c读取配置文件错误");
            e.printStackTrace();
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
            LOGGER.warn("§d[AllMusic3]§c配置文件保存错误");
            e.printStackTrace();
        }
    }

    @Override
    public void sendMessage(String data) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player == null)
                return;
            Minecraft.getInstance().player.sendSystemMessage(Component.literal(data));
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
    public void onSound(final PlaySoundSourceEvent e) {
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
    public void onTick(ClientTickEvent.Post event) {
        AllMusicCore.tick();
    }

    private void InitAllMusicCore() {
        if (FMLLoader.getDist() != Dist.CLIENT) {
            return;
        }
        AllMusicCore.init(FMLPaths.CONFIGDIR.get(), this);
    }
}