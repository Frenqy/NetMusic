package com.github.tartaricacid.netmusic.tileentity;

import com.github.tartaricacid.netmusic.block.BlockMusicPlayer;
import com.github.tartaricacid.netmusic.init.InitBlocks;
import com.github.tartaricacid.netmusic.inventory.MusicPlayerInv;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.network.NetworkHandler;
import com.github.tartaricacid.netmusic.network.message.MusicToClientMessage;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class TileEntityMusicPlayer extends TileEntity implements ITickableTileEntity {
    public static final TileEntityType<TileEntityMusicPlayer> TYPE = TileEntityType.Builder.of(TileEntityMusicPlayer::new, InitBlocks.MUSIC_PLAYER.get()).build(null);

    // 缓存当前正在播放的音乐播放器实例（全服只有一个）
    private static TileEntityMusicPlayer activeInstance = null;

    private static final String CD_ITEM_TAG = "ItemStackCD";
    private static final String IS_PLAY_TAG = "IsPlay";
    private static final String CURRENT_TIME_TAG = "CurrentTime";
    private static final String SIGNAL_TAG = "RedStoneSignal";
    private final ItemStackHandler playerInv = new MusicPlayerInv(this);
    private LazyOptional<IItemHandler> playerInvHandler;
    private boolean isPlay = false;
    private int currentTime;
    private boolean hasSignal = false;

    public TileEntityMusicPlayer() {
        super(TYPE);
    }

    /**
     * 获取当前活跃的音乐播放器实例
     * @return 正在播放的音乐播放器实例，如果没有则返回null
     */
    @Nullable
    public static TileEntityMusicPlayer getActiveInstance() {
        // 验证缓存的实例是否仍然有效
        if (activeInstance != null && (activeInstance.isRemoved() || !activeInstance.isPlay())) {
            activeInstance = null;
        }
        return activeInstance;
    }

    @Override
    public CompoundNBT save(CompoundNBT compound) {
        getTileData().put(CD_ITEM_TAG, playerInv.serializeNBT());
        getTileData().putBoolean(IS_PLAY_TAG, isPlay);
        getTileData().putInt(CURRENT_TIME_TAG, currentTime);
        getTileData().putBoolean(SIGNAL_TAG, hasSignal);
        return super.save(compound);
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        playerInv.deserializeNBT(getTileData().getCompound(CD_ITEM_TAG));
        isPlay = getTileData().getBoolean(IS_PLAY_TAG);
        currentTime = getTileData().getInt(CURRENT_TIME_TAG);
        hasSignal = getTileData().getBoolean(SIGNAL_TAG);
    }

    @Override
    public CompoundNBT getUpdateTag() {
        return this.save(new CompoundNBT());
    }

    @Nullable
    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(getBlockPos(), -1, this.save(new CompoundNBT()));
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        if (level != null) {
            this.load(level.getBlockState(pkt.getPos()), pkt.getTag());
        }
    }

    public ItemStackHandler getPlayerInv() {
        return playerInv;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (!this.remove && cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (this.playerInvHandler == null) {
                this.playerInvHandler = LazyOptional.of(this::createHandler);
            }
            return this.playerInvHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void clearCache() {
        super.clearCache();
        if (this.playerInvHandler != null) {
            LazyOptional<?> oldHandler = this.playerInvHandler;
            this.playerInvHandler = null;
            oldHandler.invalidate();
        }
    }

    private IItemHandler createHandler() {
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
        // 更新缓存
        activeInstance = this;
        if (level != null && !level.isClientSide) {
            MusicToClientMessage msg = new MusicToClientMessage(worldPosition, info.songUrl, info.songTime, info.songName, info.songId);
            // send to all players
            level.getServer().getPlayerList().getPlayers().forEach(player -> {
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
            });
        }
    }

    public void markDirty() {
        this.setChanged();
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, Constants.BlockFlags.DEFAULT);
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        if (playerInvHandler != null) {
            playerInvHandler.invalidate();
            playerInvHandler = null;
        }
        // 清除缓存
        if (activeInstance == this) {
            activeInstance = null;
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

    @Override
    public void tick() {
        this.tickTime();
        if (0 < this.getCurrentTime() && this.getCurrentTime() < 16 && this.getCurrentTime() % 5 == 0) {
            this.setPlay(false);
            this.markDirty();

            // push item to down block
            TileEntity blockEntityDown = this.getLevel().getBlockEntity(this.getBlockPos().below());
            if (blockEntityDown != null) {
                blockEntityDown.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(handler -> {
                    ItemStack stack = this.getPlayerInv().extractItem(0, 1, false);
                    if (!stack.isEmpty()) {
                        handler.insertItem(0, stack, false);
                    }
                });
            }

            // pull item from top block
            TileEntity blockEntityTop = this.getLevel().getBlockEntity(this.getBlockPos().above());
            if (blockEntityTop != null) {
                blockEntityTop.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(handler -> {
                    // find all slots with item
                    List<Integer> slots = new java.util.ArrayList<>();
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack stack = handler.getStackInSlot(i);
                        if (!stack.isEmpty() && stack.getItem() instanceof ItemMusicCD) {
                            slots.add(i);
                        }
                    }
                    // extract item from random slot
                    if (!slots.isEmpty()) {
                        // get random slot
                        int slot = slots.get(level.random.nextInt(slots.size()));
                        ItemStack stack = handler.extractItem(slot, 1, false);
                        if (!stack.isEmpty()) {
                            this.getPlayerInv().insertItem(0, stack, false);
                        }
                    }
                });
            }

            ItemStack stackInSlot = this.getPlayerInv().getStackInSlot(0);
            if (stackInSlot.isEmpty()) {
                return;
            }
            ItemMusicCD.SongInfo songInfo = ItemMusicCD.getSongInfo(stackInSlot);
            if (songInfo != null) {
                this.setPlayToClient(songInfo);
            }
        }
    }
}
