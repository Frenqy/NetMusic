package com.github.tartaricacid.netmusic.network.message;

import com.github.tartaricacid.netmusic.client.audio.MusicPlayManager;
import com.github.tartaricacid.netmusic.client.audio.NetMusicSound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class MusicToClientMessage {
    private final BlockPos pos;
    private final String url;
    private final int timeSecond;
    private final String songName;
    private final long songId;
    private final int elapsedSeconds; // 已播放的秒数

    public MusicToClientMessage(BlockPos pos, String url, int timeSecond, String songName, long songId) {
        this(pos, url, timeSecond, songName, songId, 0);
    }

    public MusicToClientMessage(BlockPos pos, String url, int timeSecond, String songName, long songId, int elapsedSeconds) {
        this.pos = pos;
        this.url = url;
        this.timeSecond = timeSecond;
        this.songName = songName;
        this.songId = songId;
        this.elapsedSeconds = elapsedSeconds;
    }

    public static MusicToClientMessage decode(PacketBuffer buf) {
        return new MusicToClientMessage(BlockPos.of(buf.readLong()), buf.readUtf(), buf.readInt(), buf.readUtf(), buf.readLong(), buf.readInt());
    }

    public static void encode(MusicToClientMessage message, PacketBuffer buf) {
        buf.writeLong(message.pos.asLong());
        buf.writeUtf(message.url);
        buf.writeInt(message.timeSecond);
        buf.writeUtf(message.songName);
        buf.writeLong(message.songId);
        buf.writeInt(message.elapsedSeconds);
    }

    public static void handle(MusicToClientMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> CompletableFuture.runAsync(() -> onHandle(message), Util.backgroundExecutor()));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void onHandle(MusicToClientMessage message) {
        // 播放音乐，从指定进度开始
        MusicPlayManager.play(message.url, message.songName, message.songId, message.elapsedSeconds,
            url -> new NetMusicSound(message.pos, url, message.timeSecond, message.elapsedSeconds));
    }
}
