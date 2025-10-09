package com.github.tartaricacid.netmusic.network.message;

import com.coloryr.allmusic.client.core.HttpClientUtil;
import com.github.tartaricacid.netmusic.NetMusic;
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
    public static final StreamCodec<ByteBuf, MusicToClientMessage> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, MusicToClientMessage::getPos, ByteBufCodecs.STRING_UTF8, MusicToClientMessage::getUrl, ByteBufCodecs.VAR_INT, MusicToClientMessage::getTimeSecond, ByteBufCodecs.STRING_UTF8, MusicToClientMessage::getSongName, ByteBufCodecs.VAR_LONG, MusicToClientMessage::getSongId, MusicToClientMessage::new);

    private final BlockPos pos;
    private final String url;
    private final int timeSecond;
    private final String songName;
    private final long songId;

    public MusicToClientMessage(BlockPos pos, String url, int timeSecond, String songName, long songId) {
        this.pos = pos;
        this.url = url;
        this.timeSecond = timeSecond;
        this.songName = songName;
        this.songId = songId;
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

    public static void handle(MusicToClientMessage message, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> CompletableFuture.runAsync(() -> onHandle(message), Util.backgroundExecutor()));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void onHandle(MusicToClientMessage message) {
        NetMusic.LOGGER.info("Received music play message: pos={}, url={}, timeSecond={}, songName={}, songId={}",
                message.pos, message.url, message.timeSecond, message.songName, message.songId);
        String playUrl = message.url;
        if (message.songId != 0){
            playUrl = HttpClientUtil.getPlayUrl(String.valueOf(message.songId));
            NetMusic.LOGGER.info("Resolved play URL from song ID {}: {}", message.songId, playUrl);
        }
        if (playUrl == null){
            playUrl = message.url;
        }
        MusicPlayManager.play(playUrl, message.songName, url -> new NetMusicSound(message.pos, url, message.timeSecond));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
