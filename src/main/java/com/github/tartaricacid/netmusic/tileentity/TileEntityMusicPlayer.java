package com.github.tartaricacid.netmusic.tileentity;

import com.github.tartaricacid.netmusic.block.BlockMusicPlayer;
import com.github.tartaricacid.netmusic.init.InitBlocks;
import com.github.tartaricacid.netmusic.inventory.MusicPlayerInv;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.network.NetworkHandler;
import com.github.tartaricacid.netmusic.network.message.MusicToClientMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.github.tartaricacid.netmusic.block.BlockMusicPlayer.CYCLE_DISABLE;

public class TileEntityMusicPlayer extends BlockEntity {
    public static final BlockEntityType<TileEntityMusicPlayer> TYPE = BlockEntityType.Builder.of(TileEntityMusicPlayer::new, InitBlocks.MUSIC_PLAYER.get()).build(null);
    private static final String CD_ITEM_TAG = "ItemStackCD";
    private static final String IS_PLAY_TAG = "IsPlay";
    private static final String CURRENT_TIME_TAG = "CurrentTime";
    private static final String SIGNAL_TAG = "RedStoneSignal";
    private final ItemStackHandler playerInv = new MusicPlayerInv(this);
    private boolean isPlay = false;
    private int currentTime;
    private boolean hasSignal = false;

    public TileEntityMusicPlayer(BlockPos blockPos, BlockState blockState) {
        super(TYPE, blockPos, blockState);
    }

    @Override
    public void saveAdditional(CompoundTag compound, HolderLookup.Provider provider) {
        getPersistentData().put(CD_ITEM_TAG, playerInv.serializeNBT(provider));
        getPersistentData().putBoolean(IS_PLAY_TAG, isPlay);
        getPersistentData().putInt(CURRENT_TIME_TAG, currentTime);
        getPersistentData().putBoolean(SIGNAL_TAG, hasSignal);
        super.saveAdditional(compound, provider);
    }

    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        playerInv.deserializeNBT(provider, getPersistentData().getCompound(CD_ITEM_TAG));
        isPlay = getPersistentData().getBoolean(IS_PLAY_TAG);
        currentTime = getPersistentData().getInt(CURRENT_TIME_TAG);
        hasSignal = getPersistentData().getBoolean(SIGNAL_TAG);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return this.saveWithoutMetadata(provider);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public ItemStackHandler getPlayerInv() {
        return playerInv;
    }

    public IItemHandler createHandler() {
        BlockState state = this.getBlockState();
        if (state.getBlock() instanceof BlockMusicPlayer) {
            return this.playerInv;
        }
        return null;
    }

    public boolean isPlay() {
        return isPlay;
    }

    public void setPlay(boolean play) {
        isPlay = play;
    }

    public void setPlayToClient(ItemMusicCD.SongInfo info) {
        this.setCurrentTime(info.songTime * 20 + 64);
        this.isPlay = true;
        if (level != null && !level.isClientSide) {
            MusicToClientMessage msg = new MusicToClientMessage(worldPosition, info.songUrl, info.songTime, info.songName, info.songId, 0);
            NetworkHandler.sendToAllClients(msg);
        }
    }

    public void markDirty() {
        this.setChanged();
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    public void setCurrentTime(int time) {
        this.currentTime = time;
    }

    public int getCurrentTime() {
        return currentTime;
    }

    public boolean hasSignal() {
        return hasSignal;
    }

    public void setSignal(boolean signal) {
        this.hasSignal = signal;
    }

    public void tickTime() {
        if (currentTime > 0) {
            currentTime--;
        }
    }

    public static void tick(Level level, BlockPos blockPos, BlockState blockState, TileEntityMusicPlayer te) {
        te.tickTime();
        if (0 < te.getCurrentTime() && te.getCurrentTime() < 16 && te.getCurrentTime() % 5 == 0) {
            if (blockState.getValue(CYCLE_DISABLE)) {
                te.setPlay(false);
                te.markDirty();

                // push item to down block
                var capabilityDown = te.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, te.getBlockPos().below(), Direction.UP);
                if (capabilityDown != null){
                    ItemStack stack = te.getPlayerInv().extractItem(0, 1, false);
                    if (!stack.isEmpty()){
                        capabilityDown.insertItem(0, stack, false);
                    }
                }

                // pull item from top block
                var capabilityTop = te.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, te.getBlockPos().above(), Direction.DOWN);
                if (capabilityTop != null){
                    // find all slots with item
                    List<Integer> slots = new ArrayList<>();
                    int slotsCount = capabilityTop.getSlots();
                    for (int i = 0; i < slotsCount; i++) {
                        ItemStack stack = capabilityTop.getStackInSlot(i);
                        if (!stack.isEmpty() && stack.getItem() instanceof ItemMusicCD) {
                            slots.add(i);
                        }
                    }
                    // extract item from random slot
                    if (!slots.isEmpty()) {
                        // get random slot
                        int slot = slots.get(level.random.nextInt(slots.size()));
                        ItemStack stack = capabilityTop.extractItem(slot, 1, false);
                        if (!stack.isEmpty()) {
                            te.getPlayerInv().insertItem(0, stack, false);
                        }
                    }
                }
            }
            ItemStack stackInSlot = te.getPlayerInv().getStackInSlot(0);
            if (stackInSlot.isEmpty()) {
                return;
            }
            ItemMusicCD.SongInfo songInfo = ItemMusicCD.getSongInfo(stackInSlot);
            if (songInfo != null) {
                te.setPlayToClient(songInfo);
            }
        }
    }
}
