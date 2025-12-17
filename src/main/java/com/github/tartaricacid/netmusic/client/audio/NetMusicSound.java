package com.github.tartaricacid.netmusic.client.audio;

import com.github.tartaricacid.netmusic.init.InitSounds;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class NetMusicSound extends AbstractTickableSoundInstance {
    private final URL songUrl;
    private final int tickTimes;
    private final BlockPos pos;
    private int tick;
    private int startSeconds;

    public NetMusicSound(BlockPos pos, URL songUrl, int timeSecond, int startSeconds) {
        super(InitSounds.NET_MUSIC.get(), SoundSource.RECORDS, SoundInstance.createUnseededRandom());
        this.songUrl = songUrl;
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.tickTimes = timeSecond * 20;
        this.volume = 4.0f;
        this.tick = 0;
        this.attenuation = Attenuation.NONE;
        this.relative = true;
        this.startSeconds = startSeconds;
        this.pos = pos;
    }

    /**
     * 获取已播放的秒数
     */
    public int getElapsedSeconds() {
        return startSeconds + (tick / 20);
    }

    /**
     * 获取音乐播放器位置
     */
    public BlockPos getPos() {
        return pos;
    }

    @Override
    public void tick() {
        Level world = Minecraft.getInstance().level;
        if (world == null) {
            return;
        }
        tick++;
        if (tick > tickTimes + 50) {
            // 音乐即将停止，清除管理器中的记录
            ClientMusicPlayerManager.clearCurrentPlaying();
            this.stop();
        }

//        BlockEntity te = world.getBlockEntity(pos);
//        if (te instanceof TileEntityMusicPlayer) {
//            TileEntityMusicPlayer musicPlay = (TileEntityMusicPlayer) te;
//            if (!musicPlay.isPlay()) {
//                this.stop();
//            }
//        } else {
//            this.stop();
//        }
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new NetMusicAudioStream(this.songUrl, this.startSeconds);
            } catch (IOException | UnsupportedAudioFileException e) {
                e.printStackTrace();
            }
            return null;
        }, Util.backgroundExecutor());
    }
}
