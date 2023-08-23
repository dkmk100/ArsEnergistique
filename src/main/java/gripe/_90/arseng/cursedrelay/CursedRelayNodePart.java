package gripe._90.arseng.cursedrelay;

import appeng.api.networking.IGridNodeListener;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.storage.IStorageMounts;
import appeng.api.storage.IStorageProvider;
import appeng.api.storage.MEStorage;
import appeng.api.util.AECableType;
import appeng.parts.AEBasePart;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

public class CursedRelayNodePart extends AEBasePart implements IStorageProvider {

    BlockPos relayPos = null;

    public CursedRelayNodePart(IPartItem<?> partItem) {
        super(partItem);
        getMainNode().setIdlePowerUsage(0);
        getMainNode().addService(IStorageProvider.class,this);
    }

    public void setRelayTile(CursedRelayTile tile){
        relayPos = tile.getBlockPos();
    }

    public void onConnectionFinalized(){
        IStorageProvider.requestUpdate(getMainNode());
    }

    private CursedRelayTile getRelayTile(){
        if(relayPos == null){
            return null;
        }
        else if(getLevel().getBlockEntity(relayPos) instanceof CursedRelayTile tile){
            return tile;
        }
        else{
            relayPos = null;
            return null;
        }
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
    }


    public MEStorage getConnectedGridStorage(){
        return this.getGridNode().getGrid().getStorageService().getInventory();
    }

    boolean wasOnline = false;

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        var currentOnline = getMainNode().isOnline();
        if (this.wasOnline != currentOnline) {
            this.wasOnline = currentOnline;
            IStorageProvider.requestUpdate(getMainNode());
        }
    }

    @Override
    public void mountInventories(IStorageMounts storageMounts) {
        Logger logger = LoggerContext.getContext().getLogger(getClass());
        CursedRelayTile tile = getRelayTile();
        if(tile == null){
            logger.warn("missing storage tile at: "+getBlockEntity().getBlockPos());
            return;
        }
        CursedRelayTile.RelayStorageWrapper otherWrapper = tile.getOtherStorage(this);
        if(otherWrapper != null) {
            logger.info("mounting wrapper for node: "+ otherWrapper.wrapper.pos);
            storageMounts.mount(otherWrapper);
        }
        else{
            logger.warn("missing storage wrapper at: "+getBlockEntity().getBlockPos());
        }
    }

    @Override
    public void readFromNBT(CompoundTag data) {
        if(data.contains("relay_x")) {
            int x = data.getInt("relay_x");
            int y = data.getInt("relay_y");
            int z = data.getInt("relay_z");
            relayPos = new BlockPos(x,y,z);
        }
        super.readFromNBT(data);
    }

    @Override
    public void writeToNBT(CompoundTag data) {
        super.writeToNBT(data);
        if(relayPos != null){
            data.putInt("relay_x",relayPos.getX());
            data.putInt("relay_y",relayPos.getY());
            data.putInt("relay_z",relayPos.getZ());
        }
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 2;
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(2, 2, 14, 14, 14, 16);
        bch.addBox(4, 4, 12, 12, 12, 14);
    }


}
