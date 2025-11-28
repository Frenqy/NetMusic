package com.github.tartaricacid.netmusic.event;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.network.NetworkHandler;
import com.github.tartaricacid.netmusic.network.message.MusicToClientMessage;
import com.github.tartaricacid.netmusic.tileentity.TileEntityMusicPlayer;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;

/**
 * 服务端维度切换处理器
 * 监听玩家切换维度，检测正在播放的音乐并重新发送带进度的播放消息
 */
@Mod.EventBusSubscriber(modid = NetMusic.MOD_ID)
public class ServerDimensionChangeHandler {

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        // 只处理服务端
        if (event.getPlayer().level.isClientSide) {
            return;
        }

        if (!(event.getPlayer() instanceof ServerPlayerEntity)) {
            return;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
        ServerWorld world = player.getLevel();

        NetMusic.LOGGER.debug("检测到玩家 {} 切换维度，检查正在播放的音乐", player.getName().getString());

        // 延迟执行，确保玩家已完全进入新维度
        world.getServer().execute(() -> {
            // 遍历所有已加载的区块，查找正在播放的音乐播放器
            for (TileEntity te : world.blockEntityList) {
                if (te instanceof TileEntityMusicPlayer) {
                    TileEntityMusicPlayer musicPlayer = (TileEntityMusicPlayer) te;

                    // 如果音乐播放器正在播放
                    if (musicPlayer.isPlay() && musicPlayer.getCurrentTime() > 0) {
                        BlockPos pos = musicPlayer.getBlockPos();

                        // 获取CD信息
                        ItemMusicCD.SongInfo songInfo = ItemMusicCD.getSongInfo(
                            musicPlayer.getPlayerInv().getStackInSlot(0)
                        );

                        if (songInfo != null) {
                            // 计算已播放的时长（总时长 - 剩余时长）
                            // currentTime 是 tick 单位，需要转换为秒
                            int totalTicks = songInfo.songTime * 20 + 64;
                            int remainingTicks = musicPlayer.getCurrentTime();
                            int elapsedTicks = totalTicks - remainingTicks;
                            int elapsedSeconds = Math.max(0, elapsedTicks / 20);

                            NetMusic.LOGGER.info(
                                "向玩家 {} 重新发送音乐: {}, 已播放: {} 秒",
                                player.getName().getString(),
                                songInfo.songName,
                                elapsedSeconds
                            );

                            // 发送带进度的播放消息给该玩家
                            MusicToClientMessage msg = new MusicToClientMessage(
                                pos,
                                songInfo.songUrl,
                                songInfo.songTime,
                                songInfo.songName,
                                songInfo.songId,
                                elapsedSeconds
                            );

                            NetworkHandler.CHANNEL.send(
                                PacketDistributor.PLAYER.with(() -> player),
                                msg
                            );
                        }
                    }
                }
            }
        });
    }
}

