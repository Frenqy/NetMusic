package com.github.tartaricacid.netmusic.client.event;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.api.ISeekableSound;
import com.github.tartaricacid.netmusic.api.IUrlSound;
import com.github.tartaricacid.netmusic.client.audio.Mp3AudioStream;
import net.minecraft.client.audio.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = NetMusic.MOD_ID, value = Dist.CLIENT)
public class PlayNetMusicEvent {
    @SubscribeEvent
    public static void onSoundPlay(PlaySoundEvent event) {
        ISound sound = event.getSound();
        if (sound instanceof IUrlSound) {
            URL songUrl = ((IUrlSound) sound).getSongUrl();
            int startSeconds = 0;
            if (sound instanceof ISeekableSound) {
                startSeconds = ((ISeekableSound) sound).getStartSeconds();
            }
            play(sound, event.getManager(), songUrl, startSeconds);
            event.setResultSound(null);
        }
    }

    private static CompletableFuture<IAudioStream> getStream(URL url, int startSeconds) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new Mp3AudioStream(url, startSeconds);
            } catch (IOException | UnsupportedAudioFileException e) {
                NetMusic.LOGGER.error("创建音频流失败", e);
            }
            return null;
        }, Util.backgroundExecutor());
    }

    private static void play(ISound sound, SoundEngine engine, URL url, int startSeconds) {
        if (sound != null && sound.canPlaySound()) {
            SoundEventAccessor accessor = sound.resolve(engine.soundManager);
            ResourceLocation resourcelocation = sound.getLocation();

            float volume = sound.getVolume();
            float attenuationDis = Math.max(volume, 1.0F) * (float) sound.getSound().getAttenuationDistance();
            SoundCategory category = sound.getSource();
            float calculateVolume = calculateVolume(sound, engine);
            float calculatePitch = calculatePitch(sound);
            ISound.AttenuationType type = sound.getAttenuation();
            boolean relative = sound.isRelative();

            Vector3d vector3d = new Vector3d(sound.getX(), sound.getY(), sound.getZ());
            if (!engine.listeners.isEmpty()) {
                boolean tmp = relative || type == ISound.AttenuationType.NONE || engine.listener.getListenerPosition().distanceToSqr(vector3d) < (double) (attenuationDis * attenuationDis);
                if (tmp && accessor != null) {
                    for (ISoundEventListener listener : engine.listeners) {
                        listener.onPlaySound(sound, accessor);
                    }
                }
            }

            if (engine.listener.getGain() <= 0) {
                NetMusic.LOGGER.debug("Skipped playing soundEvent: {}, master volume was zero", resourcelocation);
            } else {
                CompletableFuture<ChannelManager.Entry> handle = engine.channelAccess.createHandle(SoundSystem.Mode.STREAMING);
                ChannelManager.Entry entry = handle.join();
                engine.soundDeleteTime.put(sound, engine.tickCount + 20);
                engine.instanceToChannel.put(sound, entry);
                engine.instanceBySource.put(category, sound);
                entry.execute((soundSource) -> {
                    soundSource.setPitch(calculatePitch);
                    soundSource.setVolume(calculateVolume);
                    // soundSource.linearAttenuation(attenuationDis);
                    soundSource.setLooping(false);
                    soundSource.setSelfPosition(Vector3d.ZERO);
                    soundSource.setRelative(true);
                    soundSource.disableAttenuation();
                });
                getStream(url, startSeconds).thenAccept((stream) -> entry.execute((source) -> {
                    if (stream != null) {
                        source.attachBufferStream(stream);
                        source.play();
                    }
                }));

                if (sound instanceof ITickableSound) {
                    engine.tickingSounds.add((ITickableSound) sound);
                }
            }
        }
    }

    private static float calculatePitch(ISound pSound) {
        return MathHelper.clamp(pSound.getPitch(), 0.5F, 2.0F);
    }

    private static float calculateVolume(ISound pSound, SoundEngine engine) {
        return MathHelper.clamp(pSound.getVolume() * getVolume(pSound.getSource(), engine), 0.0F, 1.0F);
    }

    private static float getVolume(@Nullable SoundCategory pCategory, SoundEngine engine) {
        return pCategory != null && pCategory != SoundCategory.MASTER ? engine.options.getSoundSourceVolume(pCategory) : 1.0F;
    }
}
