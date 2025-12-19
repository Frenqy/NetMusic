package com.github.tartaricacid.netmusic.client.audio;

import com.coloryr.allmusic.client.core.HttpClientUtil;
import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.api.NetWorker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

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

    public static void play(String url, String songName, long songId, Function<URL, ISound> sound) {
        play(url, songName, songId, 0, sound);
    }

    public static void play(String url, String songName, long songId, int startSeconds, Function<URL, ISound> sound) {
        String vipUrl = null;
        if (songId != 0) {
            vipUrl = HttpClientUtil.getPlayUrl(String.valueOf(songId));
            if (vipUrl == null){
                Minecraft.getInstance().gui.getChat().addMessage(new StringTextComponent("§c无法获取VIP音乐的播放地址，请检查Cookie是否有效或音乐是否下架"));
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
        } else {
            NetMusic.LOGGER.debug("使用原始播放地址: " + url);
        }

        if (url != null && !url.equals(ERROR_404)) {
            playMusic(url, songName, startSeconds, sound);
        }
    }

    private static void playMusic(String url, String songName, int startSeconds, Function<URL, ISound> sound) {
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

            final int finalStartSeconds = startSeconds;
            Minecraft.getInstance().submitAsync(() -> {
                Minecraft.getInstance().getSoundManager().stop();
                Minecraft.getInstance().getSoundManager().play(sound.apply(urlFinal));
                if (startSeconds > 0) {
                    NetMusic.LOGGER.info("从 {} 秒处开始播放音乐: {}", startSeconds, songName);
                }
                Minecraft.getInstance().gui.setNowPlaying(new StringTextComponent(songName));
            });
        } catch (MalformedURLException | URISyntaxException e) {
            NetMusic.LOGGER.error("播放音乐失败", e);
        }
    }
}
