package com.github.tartaricacid.netmusic.compat.tlm.backpack.data;

import com.github.tartaricacid.netmusic.compat.tlm.message.MaidMusicToClientMessage;
import com.github.tartaricacid.netmusic.init.InitItems;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.network.NetworkHandler;
import com.github.tartaricacid.touhoulittlemaid.api.backpack.IBackpackData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.IIntArray;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

public class MusicPlayerBackpackData implements IBackpackData {
    private int selectSlotId = 0;
    private int playTick = 0;

    private final IIntArray dataAccess = new IIntArray() {
        @Override
        public int get(int index) {
            if (index == 0) {
                return MusicPlayerBackpackData.this.selectSlotId;
            }
            if (index == 1) {
                return MusicPlayerBackpackData.this.playTick;
            }
            return 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                MusicPlayerBackpackData.this.selectSlotId = value;
            }
            if (index == 1) {
                MusicPlayerBackpackData.this.playTick = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    @Override
    public IIntArray getDataAccess() {
        return dataAccess;
    }

    @Override
    public void load(CompoundNBT compoundTag, EntityMaid entityMaid) {
        if (compoundTag.contains("MusicPlayerSelectSlotId", Constants.NBT.TAG_INT)) {
            this.selectSlotId = compoundTag.getInt("MusicPlayerSelectSlotId");
        }
    }

    @Override
    public void save(CompoundNBT compoundTag, EntityMaid entityMaid) {
        compoundTag.putInt("MusicPlayerSelectSlotId", this.selectSlotId);
    }

    @Override
    public void serverTick(EntityMaid entityMaid) {
        if (this.playTick > 0) {
            this.playTick--;
            if (playTick == 0) {
                playNextSong(entityMaid);
            }
        }
    }

    private void playNextSong(EntityMaid entityMaid) {
        CombinedInvWrapper availableInv = entityMaid.getAvailableInv(false);
        int startSlot = this.selectSlotId + 6 + 1;
        int stopSlot = 6 + 24;
        // 先从当前位置 +1 搜索，直到最后
        for (int i = startSlot; i < stopSlot; i++) {
            if (playMusic(entityMaid, availableInv, i)) {
                return;
            }
        }
        // 没有？那就从开头搜索到当前位置
        for (int i = 6; i <= startSlot; i++) {
            if (playMusic(entityMaid, availableInv, i)) {
                return;
            }
        }
        this.selectSlotId = 0;
        this.playTick = 0;
    }

    private boolean playMusic(EntityMaid entityMaid, CombinedInvWrapper availableInv, int slotId) {
        ItemStack stackInSlot = availableInv.getStackInSlot(slotId);
        if (stackInSlot.getItem() == InitItems.MUSIC_CD.get()) {
            ItemMusicCD.SongInfo info = ItemMusicCD.getSongInfo(stackInSlot);
            if (info == null) {
                return false;
            }
            this.selectSlotId = slotId - 6;
            this.playTick = info.songTime * 20 + 64;
            MaidMusicToClientMessage msg = new MaidMusicToClientMessage(entityMaid.getId(), info.songUrl, info.songTime, info.songName, info.songId);
            NetworkHandler.sendToNearby(entityMaid.level, entityMaid.blockPosition(), msg);
            return true;
        }
        return false;
    }
}
