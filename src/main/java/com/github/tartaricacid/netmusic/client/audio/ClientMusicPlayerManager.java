package com.github.tartaricacid.netmusic.client.audio;

import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * 客户端音乐播放器管理器
 * 用于在客户端内存中跟踪当前正在播放的音乐播放器
 */
@OnlyIn(Dist.CLIENT)
public class ClientMusicPlayerManager {
    private static BlockPos currentPlayingPosition = null;
    private static String currentSongUrl = null;
    private static String currentSongName = null;
    private static long currentSongId = 0;
    private static int currentSongDuration = 0; // 歌曲总时长（秒）
    private static NetMusicSound currentSound = null;

    /**
     * 设置当前正在播放的音乐播放器信息
     */
    public static void setCurrentPlaying(BlockPos pos, String url, String songName, long songId, int duration, NetMusicSound sound) {
        currentPlayingPosition = pos;
        currentSongUrl = url;
        currentSongName = songName;
        currentSongId = songId;
        currentSongDuration = duration;
        currentSound = sound;
    }

    /**
     * 清除当前播放信息
     */
    public static void clearCurrentPlaying() {
        currentPlayingPosition = null;
        currentSongUrl = null;
        currentSongName = null;
        currentSongId = 0;
        currentSongDuration = 0;
        currentSound = null;
    }

    /**
     * 获取当前播放的位置
     */
    @Nullable
    public static BlockPos getCurrentPlayingPosition() {
        return currentPlayingPosition;
    }

    /**
     * 获取当前歌曲URL
     */
    @Nullable
    public static String getCurrentSongUrl() {
        return currentSongUrl;
    }

    /**
     * 获取当前歌曲名称
     */
    @Nullable
    public static String getCurrentSongName() {
        return currentSongName;
    }

    /**
     * 获取当前歌曲ID
     */
    public static long getCurrentSongId() {
        return currentSongId;
    }

    /**
     * 获取当前歌曲总时长（秒）
     */
    public static int getCurrentSongDuration() {
        return currentSongDuration;
    }

    /**
     * 获取当前播放的声音实例
     */
    @Nullable
    public static NetMusicSound getCurrentSound() {
        return currentSound;
    }

    /**
     * 检查是否有正在播放的音乐
     */
    public static boolean hasCurrentPlaying() {
        return currentPlayingPosition != null && currentSound != null;
    }

    /**
     * 获取当前已播放的秒数
     */
    public static int getCurrentElapsedSeconds() {
        if (currentSound != null) {
            return currentSound.getElapsedSeconds();
        }
        return 0;
    }
}

