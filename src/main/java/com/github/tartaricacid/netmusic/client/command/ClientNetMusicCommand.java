package com.github.tartaricacid.netmusic.client.command;

import com.github.tartaricacid.netmusic.NetMusic;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ClientNetMusicCommand {
    private static final String ROOT_NAME = "netmusic";
    private static final String COOKIE_NAME = "cookie";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(ROOT_NAME);
        LiteralArgumentBuilder<CommandSourceStack> cookie = Commands.literal(COOKIE_NAME);

        root.then(cookie.executes(ClientNetMusicCommand::loadCookie));
        dispatcher.register(root);
    }

    private static int loadCookie(CommandContext<CommandSourceStack> context) {
        try {
            NetMusic.loadRawCookie();
            context.getSource().sendSuccess(() -> Component.translatable("command.netmusic.cookie.reload.success"), false);
        } catch (Exception e) {
            e.printStackTrace();
            context.getSource().sendFailure(Component.translatable("command.netmusic.cookie.reload.fail"));
        }
        return Command.SINGLE_SUCCESS;
    }
}
