package com.github.tartaricacid.netmusic.network.message;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.client.audio.ClientMusicPlayerManager;
import com.github.tartaricacid.netmusic.client.audio.MusicPlayManager;
import com.github.tartaricacid.netmusic.client.audio.NetMusicSound;
import io.netty.buffer.ByteBuf;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.concurrent.CompletableFuture;

public class MusicToClientMessage implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MusicToClientMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(NetMusic.MOD_ID, "music_to_client"));
    public static final StreamCodec<ByteBuf, MusicToClientMessage> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, MusicToClientMessage::getPos,
            ByteBufCodecs.STRING_UTF8, MusicToClientMessage::getUrl,
            ByteBufCodecs.VAR_INT, MusicToClientMessage::getTimeSecond,
            ByteBufCodecs.STRING_UTF8, MusicToClientMessage::getSongName,
            ByteBufCodecs.VAR_LONG, MusicToClientMessage::getSongId,
            ByteBufCodecs.VAR_INT, MusicToClientMessage::getElapsedSeconds,
            MusicToClientMessage::new);

    private final BlockPos pos;
    private final String url;
    private final int timeSecond;
    private final String songName;
    private final long songId;
    private final int elapsedSeconds; // 已播放的秒数

    public MusicToClientMessage(BlockPos pos, String url, int timeSecond, String songName, long songId, int elapsedSeconds) {
        this.pos = pos;
        this.url = url;
        this.timeSecond = timeSecond;
        this.songName = songName;
        this.songId = songId;
        this.elapsedSeconds = elapsedSeconds;
    }

    public BlockPos getPos() {
        return pos;
    }

    public String getUrl() {
        return url;
    }

    public int getTimeSecond() {
        return timeSecond;
    }

    public String getSongName() {
        return songName;
    }

    public long getSongId() {
        return songId;
    }

    public int getElapsedSeconds() {
        return elapsedSeconds;
    }

    public static void handle(MusicToClientMessage message, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> CompletableFuture.runAsync(() -> onHandle(message), Util.backgroundExecutor()));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void onHandle(MusicToClientMessage message) {
        MusicPlayManager.play(message.url, message.songName, message.songId, url -> {
            NetMusicSound sound = new NetMusicSound(message.pos, url, message.timeSecond, message.elapsedSeconds);
            // 保存到客户端管理器
            ClientMusicPlayerManager.setCurrentPlaying(message.pos, message.url, message.songName, message.songId, message.timeSecond, sound);
            return sound;
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
