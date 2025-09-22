package com.coloryr.allmusic.client.core;

import com.coloryr.allmusic.client.core.hud.AllMusicHud;
import com.coloryr.allmusic.client.core.objs.SaveOBJ;
import com.coloryr.allmusic.client.core.objs.ConfigObj;
import com.coloryr.allmusic.client.core.player.AllMusicPlayer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.buffer.ByteBuf;

import java.io.*;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * AllMusic核心
 */
public class AllMusicCore {
    public static final CommandType[] types = CommandType.values();

    private static final Gson gson = new Gson();

    /**
     * 音频解码器与播放器
     */
    private static AllMusicPlayer player;

    /**
     * 与游戏链接的桥
     */
    public static AllMusicBridge bridge;

    /**
     * 配置文件
     */
    public static ConfigObj config;

    /**
     * 更新音频缓存
     */
    public static void tick() {
        if (player != null) {
            player.tick();
        }
    }

    /**
     * 是否正在播放音乐
     * @return 是否在播放
     */
    public static boolean isPlay() {
        if (player == null) {
            return false;
        }
        return player.isPlay();
    }

    public static void init(Path file, AllMusicBridge bridge, IntBuffer source) {
        File configFile = new File(file.toFile(), "allmusic.json");
        if (configFile.exists()) {
            try {
                InputStreamReader reader = new InputStreamReader(
                        Files.newInputStream(configFile.toPath()),
                        StandardCharsets.UTF_8);
                BufferedReader bf = new BufferedReader(reader);
                config = new Gson().fromJson(bf, ConfigObj.class);
                bf.close();
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (config == null) {
            config = new ConfigObj();
            config.picSize = 200;
            config.queueSize = 100;
            config.exitSize = 50;
            try {
                String data = new GsonBuilder().setPrettyPrinting()
                        .create()
                        .toJson(config);
                FileOutputStream out = new FileOutputStream(configFile);
                OutputStreamWriter write = new OutputStreamWriter(out, StandardCharsets.UTF_8);
                write.write(data);
                write.close();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        AllMusicCore.bridge = bridge;
        player = new AllMusicPlayer(source);
    }

    /**
     * 初始化核心
     * @param file 配置文件
     * @param bridge 游戏桥
     */
    public static void init(Path file, AllMusicBridge bridge) {
        init(file, bridge, null);
    }

    /**
     * 退出服务器时
     */
    public static void onServerQuit() {
        try {
            stopPlaying();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止播放
     */
    private static void stopPlaying() {
        player.closePlayer();
    }

    /**
     * 重载音频
     */
    public static void reload() {
        if (player != null) {
            player.setReload();
        }
    }

    /**
     * 从数据包中读文字
     * @param buf 数据包
     * @return 文字
     */
    private static String readString(ByteBuf buf) {
        int size = buf.readInt();
        byte[] temp = new byte[size];
        buf.readBytes(temp);

        return new String(temp, StandardCharsets.UTF_8);
    }

    public static void SetMusic(String url) {
        stopPlaying();
        player.setMusic(url);
    }

    public static void StopPlaying() {
        stopPlaying();
    }
}
