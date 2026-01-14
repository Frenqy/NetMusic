package com.github.tartaricacid.netmusic.client.audio;

import com.coloryr.allmusic.client.core.HttpClientUtil;
import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.api.NetWorker;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public final class MusicPlayManager {
    private static final String ERROR_404 = "http://music.163.com/404";
    private static final String MUSIC_163_URL = "https://music.163.com/";
    private static final String LOCAL_FILE_PROTOCOL = "file";

    public static void play(String url, String songName, long songId, Function<URL, SoundInstance> sound) {
        String vipUrl = null;
        if (songId != 0) {
            vipUrl = HttpClientUtil.getPlayUrl(String.valueOf(songId));
            if (vipUrl == null){
                Minecraft.getInstance().gui.getChat().addMessage(Component.literal("§c无法获取VIP音乐的播放地址，请检查Cookie是否有效或音乐是否下架"));
            }
        }

        if (vipUrl != null) {
            url = vipUrl;
            NetMusic.LOGGER.debug("获取VIP播放地址: " + url);
        } else if (url.startsWith(MUSIC_163_URL)) {
            try {
                url = NetWorker.getRedirectUrl(url, NetMusic.NET_EASE_WEB_API.getRequestPropertyData());
                NetMusic.LOGGER.debug("获取网易播放地址: " + url);
            } catch (IOException e) {
                NetMusic.LOGGER.error("获取网易播放地址失败", e);
            }
        }

        if (url != null && !url.equals(ERROR_404)) {
            playMusic(url, songName, sound);
        }
    }

    private static void playMusic(String url, String songName, Function<URL, SoundInstance> sound) {
        final URL urlFinal;
        try {
            urlFinal = new URL(url);
            // 如果是本地文件
            if (urlFinal.getProtocol().equals(LOCAL_FILE_PROTOCOL)) {
                File file = new File(urlFinal.toURI());
                if (!file.exists()) {
                    NetMusic.LOGGER.info("File not found: {}", url);
                    return;
                }
            }
            Minecraft.getInstance().submitAsync(() -> {
                Minecraft.getInstance().getSoundManager().play(sound.apply(urlFinal));
                Minecraft.getInstance().gui.setNowPlaying(Component.literal(songName));
            });
        } catch (MalformedURLException | URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
