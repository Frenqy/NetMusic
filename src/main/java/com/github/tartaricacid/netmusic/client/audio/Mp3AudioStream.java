package com.github.tartaricacid.netmusic.client.audio;

import com.github.tartaricacid.netmusic.config.GeneralConfig;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;
import net.minecraft.client.audio.IAudioStream;
import org.apache.commons.compress.utils.IOUtils;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;

/**
 * @author SQwatermark
 */
public class Mp3AudioStream implements IAudioStream {
    private final AudioInputStream stream;
    private final byte[] array;
    private int offset;

    public Mp3AudioStream(URL url) throws UnsupportedAudioFileException, IOException {
        this(url, 0);
    }

    /**
     * 支持从指定秒数开始播放
     * @param url 音乐URL
     * @param startSeconds 开始播放的秒数
     */
    public Mp3AudioStream(URL url, int startSeconds) throws UnsupportedAudioFileException, IOException {
        AudioInputStream originalInputStream = new MpegAudioFileReader().getAudioInputStream(url);
        AudioFormat originalFormat = originalInputStream.getFormat();
        AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, originalFormat.getSampleRate(), 16,
                originalFormat.getChannels(), originalFormat.getChannels() * 2, originalFormat.getSampleRate(), false);
        AudioInputStream targetInputStream = AudioSystem.getAudioInputStream(targetFormat, originalInputStream);
        if (GeneralConfig.ENABLE_STEREO.get()) {
            targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, originalFormat.getSampleRate(), 16,
                    1, 2, originalFormat.getSampleRate(), false);
            this.stream = AudioSystem.getAudioInputStream(targetFormat, targetInputStream);
        } else {
            this.stream = targetInputStream;
        }
        this.array = IOUtils.toByteArray(stream);

        // 计算从指定秒数开始的字节偏移量
        // 公式: 偏移量 = 采样率 * 通道数 * 位深度/8 * 秒数
        AudioFormat format = this.stream.getFormat();
        int bytesPerSecond = (int) (format.getSampleRate() * format.getChannels() * (format.getSampleSizeInBits() / 8));
        int startOffset = bytesPerSecond * startSeconds;

        // 确保偏移量不超过数组长度
        this.offset = Math.min(startOffset, array.length);
    }

    @Override
    public AudioFormat getFormat() {
        return stream.getFormat();
    }

    @Override
    public ByteBuffer read(int size) {
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(size);
        if (array.length >= offset + size) {
            byteBuffer.put(array, offset, size);
        } else {
            byteBuffer.put(new byte[size]);
        }
        offset += size;
        byteBuffer.flip();
        return byteBuffer;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
