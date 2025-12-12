package com.github.tartaricacid.netmusic.compat.tlm.message;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.client.audio.MusicPlayManager;
import com.github.tartaricacid.netmusic.compat.tlm.client.audio.MaidNetMusicSound;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import io.netty.buffer.ByteBuf;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.concurrent.CompletableFuture;

public class MaidMusicToClientMessage implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MaidMusicToClientMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(NetMusic.MOD_ID, "maid_music_to_client"));
    public static final StreamCodec<ByteBuf, MaidMusicToClientMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MaidMusicToClientMessage::getEntityId,
            ByteBufCodecs.STRING_UTF8, MaidMusicToClientMessage::getUrl,
            ByteBufCodecs.VAR_INT, MaidMusicToClientMessage::getTimeSecond,
            ByteBufCodecs.STRING_UTF8, MaidMusicToClientMessage::getSongName,
            ByteBufCodecs.VAR_LONG, MaidMusicToClientMessage::getSongId,
            MaidMusicToClientMessage::new);

    private final int entityId;
    private final String url;
    private final int timeSecond;
    private final String songName;
    private final long songId;

    public MaidMusicToClientMessage(int entityId, String url, int timeSecond, String songName, long songId) {
        this.entityId = entityId;
        this.url = url;
        this.timeSecond = timeSecond;
        this.songName = songName;
        this.songId = songId;
    }

    public static void handle(MaidMusicToClientMessage message, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> CompletableFuture.runAsync(() -> onHandle(message), Util.backgroundExecutor()));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void onHandle(MaidMusicToClientMessage message) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        Entity entity = Minecraft.getInstance().level.getEntity(message.entityId);
        if (!(entity instanceof EntityMaid maid)) {
            return;
        }
        MusicPlayManager.play(message.url, message.songName, message.songId, url -> new MaidNetMusicSound(maid, url, message.timeSecond));
    }

    public int getEntityId() {
        return entityId;
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

    public  long getSongId() {
        return songId;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
