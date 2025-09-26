package com.github.tartaricacid.netmusic.tileentity;

import com.github.tartaricacid.netmusic.block.BlockMusicPlayer;
import com.github.tartaricacid.netmusic.inventory.MusicPlayerInv;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.network.MusicToClientMessage;
import com.github.tartaricacid.netmusic.proxy.CommonProxy;
import net.minecraft.block.BlockChest;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;

import javax.annotation.Nullable;

public class TileEntityMusicPlayer extends TileEntity implements ITickable {
    private static final String CD_ITEM_TAG = "ItemStackCD";
    private static final String IS_PLAY_TAG = "IsPlay";
    private static final String CURRENT_TIME_TAG = "CurrentTime";
    private static final String SIGNAL_TAG = "RedStoneSignal";
    private final ItemStackHandler playerInv = new MusicPlayerInv(this);
    private boolean isPlay = false;
    private int currentTime;
    private boolean hasSignal = false;

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        getTileData().setTag(CD_ITEM_TAG, playerInv.serializeNBT());
        getTileData().setBoolean(IS_PLAY_TAG, isPlay);
        getTileData().setInteger(CURRENT_TIME_TAG, currentTime);
        getTileData().setBoolean(SIGNAL_TAG, hasSignal);
        return super.writeToNBT(compound);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        playerInv.deserializeNBT(getTileData().getCompoundTag(CD_ITEM_TAG));
        isPlay = getTileData().getBoolean(IS_PLAY_TAG);
        currentTime = getTileData().getInteger(CURRENT_TIME_TAG);
        hasSignal = getTileData().getBoolean(SIGNAL_TAG);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> cap, @Nullable EnumFacing side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) this.playerInv;
        }
        return super.getCapability(cap, side);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    public void setPlayToClient(ItemMusicCD.SongInfo info) {
        this.setCurrentTime(info.songTime * 20 + 64);
        this.isPlay = true;
        if (world != null && !world.isRemote) {
            MusicToClientMessage msg = new MusicToClientMessage(pos, info.songUrl, info.songTime, info.songName);
            CommonProxy.INSTANCE.sendToAll(msg);
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
    public void update() {
        this.tickTime();
        if (0 < this.getCurrentTime() && this.getCurrentTime() < 16 && this.getCurrentTime() % 5 == 0) {
            this.setPlay(false);
            this.markDirty();

            // try push item to down block
            TileEntity blockEntityDown = this.getWorld().getTileEntity(this.getPos().down());
            if (blockEntityDown != null) {
                IItemHandler handler = blockEntityDown.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,EnumFacing.UP);
                if (handler != null){
                    ItemStack stack = this.getPlayerInv().extractItem(0, 1, false);
                    if (!stack.isEmpty()) {
                        handler.insertItem(0, stack, false);
                    }
                }
            }

            // check if internal inventory is empty
            if (this.getPlayerInv().getStackInSlot(0).isEmpty()) {
                // pull item from top block if internal inventory is empty
                TileEntity blockEntityTop = this.getWorld().getTileEntity(this.getPos().up());
                if (blockEntityTop != null) {
                    IItemHandler handler = blockEntityTop.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,EnumFacing.DOWN);
                    if (handler != null){
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
                            int slot = slots.get(world.rand.nextInt(slots.size()));
                            ItemStack stack = handler.extractItem(slot, 1, false);
                            if (!stack.isEmpty()) {
                                this.getPlayerInv().insertItem(0, stack, false);
                            }
                        }
                    }
                }
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

    @Override
    public void markDirty() {
        super.markDirty();
        if (world != null) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(getPos(), -1, writeToNBT(new NBTTagCompound()));
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    public ItemStackHandler getPlayerInv() {
        return playerInv;
    }

    public boolean isPlay() {
        return isPlay;
    }

    public void setPlay(boolean play) {
        isPlay = play;
    }
}
