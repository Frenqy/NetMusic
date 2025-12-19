package com.github.tartaricacid.netmusic.compat.tlm.message;

import com.github.tartaricacid.netmusic.client.audio.MusicPlayManager;
import com.github.tartaricacid.netmusic.compat.tlm.client.audio.MaidNetMusicSound;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class MaidMusicToClientMessage {
    private final int entityId;
    private final String url;
    private final int timeSecond;
    private final String songName;

    public MaidMusicToClientMessage(int entityId, String url, int timeSecond, String songName) {
        this.entityId = entityId;
        this.url = url;
        this.timeSecond = timeSecond;
        this.songName = songName;
    }

    public static MaidMusicToClientMessage decode(FriendlyByteBuf buf) {
        return new MaidMusicToClientMessage(buf.readInt(), buf.readUtf(), buf.readInt(), buf.readUtf());
    }

    public static void encode(MaidMusicToClientMessage message, FriendlyByteBuf buf) {
        buf.writeInt(message.entityId);
        buf.writeUtf(message.url);
        buf.writeInt(message.timeSecond);
        buf.writeUtf(message.songName);
    }

    public static void handle(MaidMusicToClientMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> CompletableFuture.runAsync(() -> onHandle(message), Util.backgroundExecutor()));
        }
        context.setPacketHandled(true);
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
        MusicPlayManager.play(message.url, message.songName, 0, url -> new MaidNetMusicSound(maid, url, message.timeSecond));
    }
}
