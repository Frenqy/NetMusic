package com.github.tartaricacid.netmusic.network;

import com.coloryr.allmusic.client.core.AllMusicCore;
import com.coloryr.allmusic.client.core.HttpClientUtil;
import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.api.NetWorker;
import com.github.tartaricacid.netmusic.client.audio.NetMusicSound;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.net.URL;

public class MusicToClientMessage implements IMessage {
    private static final String ERROR_404 = "http://music.163.com/404";

    private BlockPos pos;
    private String url;
    private int timeSecond;
    private String songName;
    private long songId;

    public MusicToClientMessage() {
    }

    public MusicToClientMessage(BlockPos pos, String url, int timeSecond, String songName, long songId) {
        this.pos = pos;
        this.url = url;
        this.timeSecond = timeSecond;
        this.songName = songName;
        this.songId = songId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        url = ByteBufUtils.readUTF8String(buf);
        timeSecond = buf.readInt();
        songName = ByteBufUtils.readUTF8String(buf);
        songId = buf.readLong();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        ByteBufUtils.writeUTF8String(buf, url);
        buf.writeInt(timeSecond);
        ByteBufUtils.writeUTF8String(buf, songName);
        buf.writeLong(songId);
    }

    public static class Handler implements IMessageHandler<MusicToClientMessage, IMessage> {
        @SideOnly(Side.CLIENT)
        private static void playerMusic(MusicToClientMessage message, String url) {
            FMLClientHandler.instance().getClient().addScheduledTask(() -> {
                try {
                    AllMusicCore.SetMusic(url);
                    Minecraft.getMinecraft().ingameGUI.setRecordPlayingMessage(message.songName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(MusicToClientMessage message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                String url = null;

                long id = message.songId;
                if (id != 0) {
                    url = HttpClientUtil.getPlayUrl(String.valueOf(id));
                    NetMusic.LOGGER.debug("获取VIP播放地址: " + url);
                }

                if (url == null) {
                    if (message.url.startsWith("https://music.163.com/")){
                        try {
                            url = NetWorker.getRedirectUrl(message.url, NetMusic.NET_EASE_WEB_API.getRequestPropertyData());
                            NetMusic.LOGGER.debug("获取网易播放地址: " + url);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        url = message.url;
                        NetMusic.LOGGER.debug("使用预设外链地址: " + url);
                    }
                }

                if (url != null && !url.equals(ERROR_404)) {
                    playerMusic(message, url);
                    NetMusic.LOGGER.debug("正在播放音乐: " + message.songName);
                } else {
                    NetMusic.LOGGER.error("音乐播放失败: " + message.songName);
                    Minecraft.getMinecraft().ingameGUI.addChatMessage(ChatType.SYSTEM, new TextComponentTranslation("message.netmusic.music_cd.play.fail"));
                }
            }
            return null;
        }
    }
}
