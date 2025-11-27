package com.github.tartaricacid.netmusic.client;

import com.coloryr.allmusic.client.core.AllMusicCore;
import com.github.tartaricacid.netmusic.NetMusic;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.sound.SoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NetMusic.MOD_ID, value = Dist.CLIENT)
public class AllMusicEvent {

}
