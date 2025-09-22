package com.coloryr.allmusic.client.core;

import java.nio.ByteBuffer;

/**
 * AllMusic 核心桥
 */
public interface AllMusicBridge {

    /**
     * 显示消息
     * @param data 显示内容
     */
    void sendMessage(String data);

    /**
     * 获取当前音量
     * @return 音量
     */
    float getVolume();

    /**
     * 停止播放其他音频
     */
    void stopPlayMusic();
}
