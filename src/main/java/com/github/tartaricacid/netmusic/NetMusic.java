package com.github.tartaricacid.netmusic;

import com.coloryr.allmusic.client.core.HttpClientUtil;
import com.coloryr.allmusic.client.core.objs.CookieObj;
import com.github.tartaricacid.netmusic.api.NetEaseMusic;
import com.github.tartaricacid.netmusic.api.WebApi;
import com.github.tartaricacid.netmusic.config.GeneralConfig;
import com.github.tartaricacid.netmusic.init.*;
import com.github.tartaricacid.netmusic.network.NetworkHandler;
import com.google.gson.GsonBuilder;
import com.google.gson.Gson;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLPaths;
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
public class NetMusic {
    public static final String MOD_ID = "netmusic";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static WebApi NET_EASE_WEB_API;
    public static CookieObj cookie;
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

        configDir = FMLPaths.CONFIGDIR.get().toFile();
        loadRawCookie();
        loadConfig();
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
}