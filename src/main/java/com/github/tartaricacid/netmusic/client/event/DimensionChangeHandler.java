package com.github.tartaricacid.netmusic.client.event;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.client.audio.ClientMusicPlayerManager;
import com.github.tartaricacid.netmusic.client.audio.MusicPlayManager;
import com.github.tartaricacid.netmusic.client.audio.NetMusicSound;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 客户端维度切换事件处理器
 * 用于在玩家切换维度后恢复音乐播放
 */
@EventBusSubscriber(modid = NetMusic.MOD_ID, value = Dist.CLIENT)
public class DimensionChangeHandler {

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        // 只在客户端处理
        if (!event.getEntity().level().isClientSide) {
            return;
        }

        // 检查是否有正在播放的音乐
        if (ClientMusicPlayerManager.hasCurrentPlaying()) {
            BlockPos pos = ClientMusicPlayerManager.getCurrentPlayingPosition();
            String url = ClientMusicPlayerManager.getCurrentSongUrl();
            String songName = ClientMusicPlayerManager.getCurrentSongName();
            long songId = ClientMusicPlayerManager.getCurrentSongId();
            int duration = ClientMusicPlayerManager.getCurrentSongDuration();
            int elapsedSeconds = ClientMusicPlayerManager.getCurrentElapsedSeconds();

            // 延迟一小段时间后恢复播放，确保维度已经完全切换
            Minecraft.getInstance().submitAsync(() -> {
                try {
                    Thread.sleep(1000); // 延迟1秒
                } catch (InterruptedException e) {
                    NetMusic.LOGGER.error("等待维度切换时被中断", e);
                    Thread.currentThread().interrupt();
                }

                // 重新播放音乐，从已播放的位置继续
                Minecraft.getInstance().execute(() -> {
                    if (pos != null && url != null && songName != null) {
                        NetMusic.LOGGER.info("玩家切换维度，从{}秒处恢复播放音乐: {}", elapsedSeconds, songName);
                        // 直接在客户端创建新的播放请求
                        MusicPlayManager.play(
                            url,
                            songName,
                            songId,
                            urlFinal -> {
                                NetMusicSound sound =
                                    new NetMusicSound(
                                        pos,
                                        urlFinal,
                                        duration,
                                        elapsedSeconds
                                    );
                                // 更新管理器中的声音实例
                                ClientMusicPlayerManager.setCurrentPlaying(pos, url, songName, songId, duration, sound);
                                return sound;
                            }
                        );
                    }
                });
            });
        }
    }
}

