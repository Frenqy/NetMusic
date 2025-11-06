package com.github.tartaricacid.netmusic.client;

import com.github.tartaricacid.netmusic.NetMusic;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NetMusic.MOD_ID, value = Dist.CLIENT)
public class ClientCookieCommand {
    @SubscribeEvent
    public static void onClientChat(ClientChatEvent event) {
        String msg = event.getMessage();
        if (msg == null) {
            return;
        }
        String trimmed = msg.trim();
        if ("/netmusic cookie".equalsIgnoreCase(trimmed)) {
            event.setCanceled(true);
            // execute refresh on client and give feedback
            NetMusic.loadRawCookie();
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(new StringTextComponent("Cookie refreshed"), false);
            }
        }
    }
}

