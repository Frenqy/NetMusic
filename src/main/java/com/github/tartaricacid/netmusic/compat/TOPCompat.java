package com.github.tartaricacid.netmusic.compat;

import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.IBlockDisplayOverride;
import com.github.tartaricacid.netmusic.block.BlockMusicPlayer;

import java.util.function.Function;

public class TOPCompat implements Function<ITheOneProbe, Void> {
    @Override
    public Void apply(ITheOneProbe probe) {
        // 注册BlockMusicPlayer的显示覆盖
        probe.registerBlockDisplayOverride(new BlockMusicPlayer());
        return null;
    }
}

