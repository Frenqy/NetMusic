package com.github.tartaricacid.netmusic.client.init;

import com.github.tartaricacid.netmusic.client.command.ClientNetMusicCommand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientCommandRegistry {
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        ClientNetMusicCommand.register(event.getDispatcher());
    }
}
