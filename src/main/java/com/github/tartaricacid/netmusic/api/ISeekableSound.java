package com.github.tartaricacid.netmusic.api;

/**
 * 支持从指定时间开始播放的音频接口
 */
public interface ISeekableSound {
    /**
     * 获取开始播放的秒数
     * @return 从第几秒开始播放，0表示从头开始
     */
    int getStartSeconds();
}

