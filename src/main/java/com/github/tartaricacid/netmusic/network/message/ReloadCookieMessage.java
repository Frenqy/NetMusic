package com.github.tartaricacid.netmusic.network.message;

import com.github.tartaricacid.netmusic.NetMusic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ReloadCookieMessage {
    public ReloadCookieMessage() {
    }

    public static ReloadCookieMessage decode(FriendlyByteBuf buf) {
        return new ReloadCookieMessage();
    }

    public static void encode(ReloadCookieMessage message, FriendlyByteBuf buf) {
        // No data to encode
    }

    public static void handle(ReloadCookieMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> {
                LocalPlayer player = Minecraft.getInstance().player;
                try {
                    NetMusic.loadRawCookie();
                    if (player != null) {
                        player.sendSystemMessage(Component.translatable("command.netmusic.cookie.reload.success"));
                    }
                } catch (Exception e) {
                    if (player != null) {
                        player.sendSystemMessage(Component.translatable("command.netmusic.cookie.reload.fail"));
                    }
                    e.printStackTrace();
                }
            });
        }
        context.setPacketHandled(true);
    }
}

